/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.fabric.impl.client.indigo.renderer.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.MatrixUtil;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.util.TriState;
import net.fabricmc.fabric.impl.client.indigo.renderer.helper.ColorHelper;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.EncodingFormat;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;
import net.fabricmc.fabric.impl.renderer.VanillaModelEncoder;
import net.fabricmc.fabric.mixin.client.indigo.renderer.ItemRendererAccessor;

/**
 * The render context used for item rendering.
 */
public class ItemRenderContext extends AbstractRenderContext {
	/** Value vanilla uses for item rendering.  The only sensible choice, of course.  */
	private static final long ITEM_RANDOM_SEED = 42L;

	private final ItemColors colorMap;
	private final RandomSource random = RandomSource.create();
	private final Supplier<RandomSource> randomSupplier = () -> {
		random.setSeed(ITEM_RANDOM_SEED);
		return random;
	};

	private final MutableQuadViewImpl editorQuad = new MutableQuadViewImpl() {
		{
			data = new int[EncodingFormat.TOTAL_STRIDE];
			clear();
		}

		@Override
		public void emitDirectly() {
			renderQuad(this);
		}
	};

	private final BakedModelConsumerImpl vanillaModelConsumer = new BakedModelConsumerImpl();

	private ItemStack itemStack;
	private ModelTransformationMode transformMode;
	private PoseStack matrixStack;
	private MultiBufferSource vertexConsumerProvider;
	private int lightmap;

	private boolean isDefaultTranslucent;
	private boolean isDefaultGlint;
	private boolean isGlintDynamicDisplay;

	private PoseStack.Pose dynamicDisplayGlintEntry;
	private VertexConsumer translucentVertexConsumer;
	private VertexConsumer cutoutVertexConsumer;
	private VertexConsumer translucentGlintVertexConsumer;
	private VertexConsumer cutoutGlintVertexConsumer;

	public ItemRenderContext(ItemColors colorMap) {
		this.colorMap = colorMap;
	}

	@Override
	public QuadEmitter getEmitter() {
		editorQuad.clear();
		return editorQuad;
	}

	@Override
	public boolean isFaceCulled(@Nullable Direction face) {
		throw new IllegalStateException("isFaceCulled can only be called on a block render context.");
	}

	@Override
	public ModelTransformationMode itemTransformationMode() {
		return transformMode;
	}

	@Override
	public BakedModelConsumer bakedModelConsumer() {
		return vanillaModelConsumer;
	}

	public void renderModel(ItemStack itemStack, ModelTransformationMode transformMode, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, int lightmap, int overlay, BakedModel model) {
		this.itemStack = itemStack;
		this.transformMode = transformMode;
		this.matrixStack = matrixStack;
		this.vertexConsumerProvider = vertexConsumerProvider;
		this.lightmap = lightmap;
		this.overlay = overlay;
		computeOutputInfo();

		matrix = matrixStack.last().pose();
		normalMatrix = matrixStack.last().normal();

		model.emitItemQuads(itemStack, randomSupplier, this);

		this.itemStack = null;
		this.matrixStack = null;
		this.vertexConsumerProvider = null;

		dynamicDisplayGlintEntry = null;
		translucentVertexConsumer = null;
		cutoutVertexConsumer = null;
		translucentGlintVertexConsumer = null;
		cutoutGlintVertexConsumer = null;
	}

	private void computeOutputInfo() {
		isDefaultTranslucent = ItemBlockRenderTypes.getRenderType(itemStack) == Sheets.translucentItemSheet();
		isDefaultGlint = itemStack.hasFoil();
		isGlintDynamicDisplay = ItemRendererAccessor.fabric_callUsesDynamicDisplay(itemStack);
	}

	private void renderQuad(MutableQuadViewImpl quad) {
		if (!transform(quad)) {
			return;
		}

		final RenderMaterial mat = quad.material();
		final int colorIndex = mat.disableColorIndex() ? -1 : quad.colorIndex();
		final boolean emissive = mat.emissive();
		final VertexConsumer vertexConsumer = getVertexConsumer(mat.blendMode(), mat.glint());

		colorizeQuad(quad, colorIndex);
		shadeQuad(quad, emissive);
		bufferQuad(quad, vertexConsumer);
	}

	private void colorizeQuad(MutableQuadViewImpl quad, int colorIndex) {
		if (colorIndex != -1) {
			final int itemColor = colorMap.getColor(itemStack, colorIndex);

			for (int i = 0; i < 4; i++) {
				quad.color(i, ColorHelper.multiplyColor(itemColor, quad.color(i)));
			}
		}
	}

	private void shadeQuad(MutableQuadViewImpl quad, boolean emissive) {
		if (emissive) {
			for (int i = 0; i < 4; i++) {
				quad.lightmap(i, LightTexture.FULL_BRIGHT);
			}
		} else {
			final int lightmap = this.lightmap;

			for (int i = 0; i < 4; i++) {
				quad.lightmap(i, ColorHelper.maxBrightness(quad.lightmap(i), lightmap));
			}
		}
	}

	/**
	 * Caches custom blend mode / vertex consumers and mimics the logic
	 * in {@code RenderLayers.getItemLayer}. Layers other than
	 * translucent are mapped to cutout.
	 */
	private VertexConsumer getVertexConsumer(BlendMode blendMode, TriState glintMode) {
		boolean translucent;
		boolean glint;

		if (blendMode == BlendMode.DEFAULT) {
			translucent = isDefaultTranslucent;
		} else {
			translucent = blendMode == BlendMode.TRANSLUCENT;
		}

		if (glintMode == TriState.DEFAULT) {
			glint = isDefaultGlint;
		} else {
			glint = glintMode == TriState.TRUE;
		}

		if (translucent) {
			if (glint) {
				if (translucentGlintVertexConsumer == null) {
					translucentGlintVertexConsumer = createVertexConsumer(Sheets.translucentItemSheet(), true);
				}

				return translucentGlintVertexConsumer;
			} else {
				if (translucentVertexConsumer == null) {
					translucentVertexConsumer = createVertexConsumer(Sheets.translucentItemSheet(), false);
				}

				return translucentVertexConsumer;
			}
		} else {
			if (glint) {
				if (cutoutGlintVertexConsumer == null) {
					cutoutGlintVertexConsumer = createVertexConsumer(Sheets.cutoutBlockSheet(), true);
				}

				return cutoutGlintVertexConsumer;
			} else {
				if (cutoutVertexConsumer == null) {
					cutoutVertexConsumer = createVertexConsumer(Sheets.cutoutBlockSheet(), false);
				}

				return cutoutVertexConsumer;
			}
		}
	}

	private VertexConsumer createVertexConsumer(RenderType layer, boolean glint) {
		if (isGlintDynamicDisplay && glint) {
			if (dynamicDisplayGlintEntry == null) {
				dynamicDisplayGlintEntry = matrixStack.last().copy();

				if (transformMode == ModelTransformationMode.GUI) {
					MatrixUtil.mulComponentWise(dynamicDisplayGlintEntry.pose(), 0.5F);
				} else if (transformMode.isFirstPerson()) {
					MatrixUtil.mulComponentWise(dynamicDisplayGlintEntry.pose(), 0.75F);
				}
			}

			return ItemRenderer.getCompassFoilBuffer(vertexConsumerProvider, layer, dynamicDisplayGlintEntry);
		}

		return ItemRenderer.getFoilBuffer(vertexConsumerProvider, layer, true, glint);
	}

	private class BakedModelConsumerImpl implements BakedModelConsumer {
		@Override
		public void accept(BakedModel model) {
			accept(model, null);
		}

		@Override
		public void accept(BakedModel model, @Nullable BlockState state) {
			VanillaModelEncoder.emitItemQuads(model, state, randomSupplier, ItemRenderContext.this);
		}
	}
}
