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

import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.MultiPartBakedModel;
import net.minecraft.client.resources.model.WeightedBakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Tests that vanilla and Fabric API give the same random results.
 *
 * <p>Never do this in a real mod, this is purely for testing!
 */
public class RandomSupplierTest implements ClientModInitializer {
	private static long previousRandom = 0;
	private static boolean hasPreviousRandom = false;

	@Override
	public void onInitializeClient() {
		var checkingModel = new RandomCheckingBakedModel();
		var weighted = new WeightedBakedModel(List.of(
				WeightedEntry.wrap(checkingModel, 1),
				WeightedEntry.wrap(checkingModel, 2)));
		var multipart = new MultiPartBakedModel(List.of(
				Pair.of(state -> true, weighted),
				Pair.of(state -> true, weighted)));
		var weightedAgain = new WeightedBakedModel(List.of(
				WeightedEntry.wrap(multipart, 1),
				WeightedEntry.wrap(multipart, 2)));

		long startingSeed = 42;
		RandomSource random = RandomSource.create();

		random.setSeed(startingSeed);
		weightedAgain.getQuads(Blocks.STONE.defaultBlockState(), null, random);

		random.setSeed(startingSeed);
		weightedAgain.getQuads(Blocks.STONE.defaultBlockState(), null, random);

		Supplier<RandomSource> randomSupplier = () -> {
			random.setSeed(startingSeed);
			return random;
		};
		weightedAgain.emitBlockQuads(null, Blocks.STONE.defaultBlockState(), BlockPos.ZERO, randomSupplier, null);
	}

	private static class RandomCheckingBakedModel implements BakedModel {
		@Override
		public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, RandomSource random) {
			long value = random.nextLong();

			if (hasPreviousRandom) {
				if (value != previousRandom) {
					throw new AssertionError("Random value is not the same as the previous one!");
				}
			} else {
				hasPreviousRandom = true;
				previousRandom = value;
			}

			return List.of();
		}

		@Override
		public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context) {
			getQuads(state, null, randomSupplier.get());
		}

		@Override
		public boolean useAmbientOcclusion() {
			return false;
		}

		@Override
		public boolean isGui3d() {
			return false;
		}

		@Override
		public boolean usesBlockLight() {
			return false;
		}

		@Override
		public boolean isCustomRenderer() {
			return false;
		}

		@Override
		public TextureAtlasSprite getParticleIcon() {
			return null;
		}

		@Override
		public ItemTransforms getTransforms() {
			return null;
		}

		@Override
		public ItemOverrides getOverrides() {
			return null;
		}
	}
}
