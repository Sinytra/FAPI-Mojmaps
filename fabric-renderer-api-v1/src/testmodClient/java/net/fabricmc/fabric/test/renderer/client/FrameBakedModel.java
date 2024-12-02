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
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;
import net.fabricmc.fabric.api.blockview.v2.FabricBlockView;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.MaterialFinder;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadTransform;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.EmptyBlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class FrameBakedModel implements BakedModel {
	private final Mesh frameMesh;
	private final TextureAtlasSprite frameSprite;
	private final RenderMaterial translucentMaterial;
	private final RenderMaterial translucentEmissiveMaterial;

	public FrameBakedModel(Mesh frameMesh, TextureAtlasSprite frameSprite) {
		this.frameMesh = frameMesh;
		this.frameSprite = frameSprite;

		MaterialFinder finder = Renderer.get().materialFinder();
		this.translucentMaterial = finder.blendMode(BlendMode.TRANSLUCENT).find();
		finder.clear();
		this.translucentEmissiveMaterial = finder.blendMode(BlendMode.TRANSLUCENT).emissive(true).find();
	}

	@Override
	public boolean isVanillaAdapter() {
		return false;
	}

	@Override
	public void emitBlockQuads(QuadEmitter emitter, BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, Predicate<@Nullable Direction> cullTest) {
		// Emit our frame mesh
		frameMesh.outputTo(emitter);

		// We should not access the block entity from here. We should instead use the immutable render data provided by the block entity.
		if (!(((FabricBlockView) blockView).getBlockEntityRenderData(pos) instanceof Block mimickedBlock)) {
			return; // No inner block to render, or data of wrong type
		}

		BlockState innerState = mimickedBlock.defaultBlockState();
		BakedModel innerModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(innerState);

		// Now, we emit a transparent scaled-down version of the inner model
		// Try both emissive and non-emissive versions of the translucent material
		RenderMaterial material = pos.getX() % 2 == 0 ? translucentMaterial : translucentEmissiveMaterial;

		// Let's push a transform to scale the model down and make it transparent
		emitter.pushTransform(createInnerTransform(material));
		// Emit the inner block model
		innerModel.emitBlockQuads(emitter, blockView, innerState, pos, randomSupplier, cullTest);
		// Let's not forget to pop the transform!
		emitter.popTransform();
	}

	@Override
	public void emitItemQuads(QuadEmitter emitter, Supplier<RandomSource> randomSupplier) {
		// Emit our frame mesh
		frameMesh.outputTo(emitter);

		BlockState innerState = Blocks.OAK_FENCE.defaultBlockState();
		BakedModel innerModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(innerState);

		// Let's push a transform to scale the model down and make it transparent
		emitter.pushTransform(createInnerTransform(translucentMaterial));
		// Emit the inner block model
		innerModel.emitBlockQuads(emitter, EmptyBlockAndTintGetter.INSTANCE, innerState, BlockPos.ZERO, randomSupplier, face -> false);
		// Let's not forget to pop the transform!
		emitter.popTransform();
	}

	/**
	 * Create a transform to scale down the model, make it translucent, and assign the given material.
	 */
	private static QuadTransform createInnerTransform(RenderMaterial material) {
		return quad -> {
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
		};
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
	public TextureAtlasSprite getParticleIcon() {
		return this.frameSprite;
	}

	@Override
	public ItemTransforms getTransforms() {
		return ModelHelper.MODEL_TRANSFORM_BLOCK;
	}
}
