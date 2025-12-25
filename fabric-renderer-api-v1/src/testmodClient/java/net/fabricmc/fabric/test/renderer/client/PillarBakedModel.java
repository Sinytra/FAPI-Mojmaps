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
import net.fabricmc.fabric.api.block.v1.FabricBlockState;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.test.renderer.Registration;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Very crude implementation of a pillar block model that connects with pillars above and below.
 */
public class PillarBakedModel implements BakedModel {
	private enum ConnectedTexture {
		ALONE, BOTTOM, MIDDLE, TOP
	}

	// alone, bottom, middle, top
	private final TextureAtlasSprite[] sprites;

	public PillarBakedModel(TextureAtlasSprite[] sprites) {
		this.sprites = sprites;
	}

	@Override
	public boolean isVanillaAdapter() {
		return false;
	}

	@Override
	public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context) {
		QuadEmitter emitter = context.getEmitter();
		// Do not use the passed state to ensure that this model connects
		// to and from blocks with a custom appearance correctly.
		BlockState worldState = blockView.getBlockState(pos);

		for (Direction side : Direction.values()) {
			ConnectedTexture texture = ConnectedTexture.ALONE;

			if (side.getAxis().isHorizontal()) {
				boolean connectAbove = canConnect(blockView, worldState, pos, pos.relative(Direction.UP), side);
				boolean connectBelow = canConnect(blockView, worldState, pos, pos.relative(Direction.DOWN), side);

				if (connectAbove && connectBelow) {
					texture = ConnectedTexture.MIDDLE;
				} else if (connectAbove) {
					texture = ConnectedTexture.BOTTOM;
				} else if (connectBelow) {
					texture = ConnectedTexture.TOP;
				}
			}

			emitter.square(side, 0, 0, 1, 1, 0);
			emitter.spriteBake(sprites[texture.ordinal()], MutableQuadView.BAKE_LOCK_UV);
			emitter.color(-1, -1, -1, -1);
			emitter.emit();
		}
	}

	@Override
	public void emitItemQuads(ItemStack stack, Supplier<RandomSource> randomSupplier, RenderContext context) {
		QuadEmitter emitter = context.getEmitter();

		for (Direction side : Direction.values()) {
			emitter.square(side, 0, 0, 1, 1, 0);
			emitter.spriteBake(sprites[ConnectedTexture.ALONE.ordinal()], MutableQuadView.BAKE_LOCK_UV);
			emitter.color(-1, -1, -1, -1);
			emitter.emit();
		}
	}

	private static boolean canConnect(BlockAndTintGetter blockView, BlockState originState, BlockPos originPos, BlockPos otherPos, Direction side) {
		BlockState otherState = blockView.getBlockState(otherPos);
		// In this testmod we can't rely on injected interfaces - in normal mods the (FabricBlockState) cast will be unnecessary
		BlockState originAppearance = ((FabricBlockState) originState).getAppearance(blockView, originPos, side, otherState, otherPos);

		if (!originAppearance.is(Registration.PILLAR_BLOCK)) {
			return false;
		}

		BlockState otherAppearance = ((FabricBlockState) otherState).getAppearance(blockView, otherPos, side, originState, originPos);

		if (!otherAppearance.is(Registration.PILLAR_BLOCK)) {
			return false;
		}

		return true;
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, RandomSource random) {
		return Collections.emptyList();
	}

	@Override
	public boolean useAmbientOcclusion() {
		return true;
	}

	@Override
	public boolean isGui3d() {
		return false;
	}

	@Override
	public boolean usesBlockLight() {
		return true;
	}

	@Override
	public boolean isCustomRenderer() {
		return false;
	}

	@Override
	public TextureAtlasSprite getParticleIcon() {
		return sprites[0];
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
