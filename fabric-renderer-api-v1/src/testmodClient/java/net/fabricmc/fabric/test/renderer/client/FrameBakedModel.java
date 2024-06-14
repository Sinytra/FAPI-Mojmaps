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

package net.fabricmc.fabric.test.renderer.client;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;
import net.fabricmc.fabric.api.blockview.v2.FabricBlockView;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.MaterialFinder;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class FrameBakedModel implements BakedModel {
	private final Mesh frameMesh;
	private final TextureAtlasSprite frameSprite;
	private final RenderMaterial translucentMaterial;
	private final RenderMaterial translucentEmissiveMaterial;

	public FrameBakedModel(Mesh frameMesh, TextureAtlasSprite frameSprite) {
		this.frameMesh = frameMesh;
		this.frameSprite = frameSprite;

		MaterialFinder finder = RendererAccess.INSTANCE.getRenderer().materialFinder();
		this.translucentMaterial = finder.blendMode(BlendMode.TRANSLUCENT).find();
		finder.clear();
		this.translucentEmissiveMaterial = finder.blendMode(BlendMode.TRANSLUCENT).emissive(true).find();
	}

	@Override
	public boolean isVanillaAdapter() {
		return false;
	}

	@Override
	public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context) {
		// Emit our frame mesh
		this.frameMesh.outputTo(context.getEmitter());

		// We should not access the block entity from here. We should instead use the immutable render data provided by the block entity.
		if (!(((FabricBlockView) blockView).getBlockEntityRenderData(pos) instanceof Block mimickedBlock)) {
			return; // No inner block to render, or data of wrong type
		}

		BlockState innerState = mimickedBlock.defaultBlockState();

		// Now, we emit a transparent scaled-down version of the inner model
		// Try both emissive and non-emissive versions of the translucent material
		RenderMaterial material = pos.getX() % 2 == 0 ? translucentMaterial : translucentEmissiveMaterial;

		emitInnerQuads(context, material, () -> {
			// Use emitBlockQuads to allow for Renderer API features
			Minecraft.getInstance().getBlockRenderer().getBlockModel(innerState).emitBlockQuads(blockView, innerState, pos, randomSupplier, context);
		});
	}

	@Override
	public void emitItemQuads(ItemStack stack, Supplier<RandomSource> randomSupplier, RenderContext context) {
		// Emit our frame mesh
		this.frameMesh.outputTo(context.getEmitter());

		// Emit a scaled-down fence for testing, trying both materials again.
		RenderMaterial material = stack.has(DataComponents.CUSTOM_NAME) ? translucentEmissiveMaterial : translucentMaterial;

		ItemStack innerItem = Items.CRAFTING_TABLE.getDefaultInstance();
		BakedModel innerModel = Minecraft.getInstance().getItemRenderer().getModel(innerItem, null, null, 0);

		emitInnerQuads(context, material, () -> {
			innerModel.emitItemQuads(innerItem, randomSupplier, context);
		});
	}

	/**
	 * Emit a scaled-down version of the inner model.
	 */
	private void emitInnerQuads(RenderContext context, RenderMaterial material, Runnable innerModelEmitter) {
		// Let's push a transform to scale the model down and make it transparent
		context.pushTransform(quad -> {
			// Scale model down
			for (int vertex = 0; vertex < 4; ++vertex) {
				float x = quad.x(vertex) * 0.8f + 0.1f;
				float y = quad.y(vertex) * 0.8f + 0.1f;
				float z = quad.z(vertex) * 0.8f + 0.1f;
				quad.pos(vertex, x, y, z);
			}

			// Make the quad partially transparent
			// Change material to translucent
			quad.material(material);

			// Change vertex colors to be partially transparent
			for (int vertex = 0; vertex < 4; ++vertex) {
				int color = quad.color(vertex);
				int alpha = (color >> 24) & 0xFF;
				alpha = alpha * 3 / 4;
				color = (color & 0xFFFFFF) | (alpha << 24);
				quad.color(vertex, color);
			}

			// Return true because we want the quad to be rendered
			return true;
		});

		// Emit the inner block model
		innerModelEmitter.run();

		// Let's not forget to pop the transform!
		context.popTransform();
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, RandomSource random) {
		return Collections.emptyList(); // Renderer API makes this obsolete, so return no quads
	}

	@Override
	public boolean useAmbientOcclusion() {
		return true; // we want the block to have a shadow depending on the adjacent blocks
	}

	@Override
	public boolean isGui3d() {
		return false;
	}

	@Override
	public boolean usesBlockLight() {
		return true; // we want the block to be lit from the side when rendered as an item
	}

	@Override
	public boolean isCustomRenderer() {
		return false;
	}

	@Override
	public TextureAtlasSprite getParticleIcon() {
		return this.frameSprite;
	}

	@Override
	public ItemTransforms getTransforms() {
		return ModelHelper.MODEL_TRANSFORM_BLOCK;
	}

	@Override
	public ItemOverrides getOverrides() {
		return ItemOverrides.EMPTY;
	}
}
