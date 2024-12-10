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

import java.util.function.Function;
import com.mojang.blaze3d.vertex.BufferBuilder;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.fabricmc.fabric.impl.client.indigo.renderer.aocalc.AoCalculator;
import net.fabricmc.fabric.impl.client.indigo.renderer.aocalc.AoLuminanceFix;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Holds, manages and provides access to the chunk-related state
 * needed by fallback and mesh consumers during terrain rendering.
 *
 * <p>Exception: per-block position offsets are tracked here so they can
 * be applied together with chunk offsets.
 */
public class ChunkRenderInfo {
	/**
	 * Serves same function as brightness cache in Mojang's AO calculator,
	 * with some differences as follows...
	 *
	 * <ul><li>Mojang uses Object2Int.  This uses Long2Int for performance and to avoid
	 * creating new immutable BlockPos references.  But will break if someone
	 * wants to expand Y limit or world borders.  If we want to support that may
	 * need to switch or make configurable.
	 *
	 * <li>Mojang overrides the map methods to limit the cache to 50 values.
	 * However, a render chunk only has 18^3 blocks in it, and the cache is cleared every chunk.
	 * For performance and simplicity, we just let map grow to the size of the render chunk.
	 *
	 * <li>Mojang only uses the cache for Ao.  Here it is used for all brightness
	 * lookups, including flat lighting.
	 *
	 * <li>The Mojang cache is a separate threadlocal with a threadlocal boolean to
	 * enable disable. Cache clearing happens with the disable. There's no use case for
	 * us when the cache needs to be disabled (and no apparent case in Mojang's code either)
	 * so we simply clear the cache at the start of each new chunk. It is also
	 * not a threadlocal because it's held within a threadlocal BlockRenderer.</ul>
	 */
	private final Long2IntOpenHashMap brightnessCache;
	private final Long2FloatOpenHashMap aoLevelCache;

	private Function<RenderType, BufferBuilder> bufferFunc;
	BlockAndTintGetter blockView;

	ChunkRenderInfo() {
		brightnessCache = new Long2IntOpenHashMap();
		brightnessCache.defaultReturnValue(Integer.MAX_VALUE);
		aoLevelCache = new Long2FloatOpenHashMap();
		aoLevelCache.defaultReturnValue(Float.MAX_VALUE);
	}

	void prepare(RenderChunkRegion blockView, Function<RenderType, BufferBuilder> bufferFunc) {
		this.blockView = blockView;
		this.bufferFunc = bufferFunc;

		brightnessCache.clear();
		aoLevelCache.clear();
	}

	void release() {
		blockView = null;
		bufferFunc = null;
	}

	BufferBuilder getBuffer(RenderType layer) {
		return bufferFunc.apply(layer);
	}

	/**
	 * Cached values for {@link LevelRenderer#getLightColor(BlockAndTintGetter, BlockState, BlockPos)}.
	 * See also the comments for {@link #brightnessCache}.
	 */
	int cachedBrightness(BlockPos pos, BlockState state) {
		long key = pos.asLong();
		int result = brightnessCache.get(key);

		if (result == Integer.MAX_VALUE) {
			result = AoCalculator.getLightmapCoordinates(blockView, state, pos);
			brightnessCache.put(key, result);
		}

		return result;
	}

	float cachedAoLevel(BlockPos pos, BlockState state) {
		long key = pos.asLong();
		float result = aoLevelCache.get(key);

		if (result == Float.MAX_VALUE) {
			result = AoLuminanceFix.INSTANCE.apply(blockView, pos, state);
			aoLevelCache.put(key, result);
		}

		return result;
	}
}
