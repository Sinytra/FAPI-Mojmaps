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

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;

/**
 * Holds, manages, and provides access to the block/world related state
 * needed to render quads.
 *
 * <p>Exception: per-block position offsets are tracked in {@link ChunkRenderInfo}
 * so they can be applied together with chunk offsets.
 */
public class BlockRenderInfo {
	private final BlockColors blockColorMap = Minecraft.getInstance().getBlockColors();
	private final BlockPos.MutableBlockPos searchPos = new BlockPos.MutableBlockPos();

	public BlockAndTintGetter blockView;
	public BlockPos blockPos;
	public BlockState blockState;

	boolean useAo;
	boolean defaultAo;
	RenderType defaultLayer;

	RandomSource random;
	long seed;
	boolean recomputeSeed;
	public final Supplier<RandomSource> randomSupplier = () -> {
		long seed = this.seed;

		if (recomputeSeed) {
			seed = blockState.getSeed(blockPos);
			this.seed = seed;
			recomputeSeed = false;
		}

		final RandomSource random = this.random;
		random.setSeed(seed);
		return random;
	};

	private boolean enableCulling;
	private int cullCompletionFlags;
	private int cullResultFlags;

	public void prepareForWorld(BlockAndTintGetter blockView, boolean enableCulling) {
		this.blockView = blockView;
		this.enableCulling = enableCulling;
	}

	public void prepareForBlock(BlockState blockState, BlockPos blockPos, boolean modelAo) {
		this.blockPos = blockPos;
		this.blockState = blockState;

		useAo = Minecraft.useAmbientOcclusion();
		defaultAo = useAo && modelAo && blockState.getLightEmission() == 0;

		defaultLayer = ItemBlockRenderTypes.getChunkRenderType(blockState);

		cullCompletionFlags = 0;
		cullResultFlags = 0;
	}

	public void release() {
		blockView = null;
		blockPos = null;
		blockState = null;
	}

	int blockColor(int tintIndex) {
		return 0xFF000000 | blockColorMap.getColor(blockState, blockView, blockPos, tintIndex);
	}

	boolean shouldDrawSide(@Nullable Direction side) {
		if (side == null || !enableCulling) {
			return true;
		}

		final int mask = 1 << side.get3DDataValue();

		if ((cullCompletionFlags & mask) == 0) {
			cullCompletionFlags |= mask;

			if (Block.shouldRenderFace(blockState, blockView.getBlockState(searchPos.setWithOffset(blockPos, side)), side)) {
				cullResultFlags |= mask;
				return true;
			} else {
				return false;
			}
		} else {
			return (cullResultFlags & mask) != 0;
		}
	}

	boolean shouldCullSide(@Nullable Direction side) {
		return !shouldDrawSide(side);
	}

	RenderType effectiveRenderLayer(BlendMode blendMode) {
		return blendMode == BlendMode.DEFAULT ? this.defaultLayer : blendMode.blockRenderLayer;
	}
}
