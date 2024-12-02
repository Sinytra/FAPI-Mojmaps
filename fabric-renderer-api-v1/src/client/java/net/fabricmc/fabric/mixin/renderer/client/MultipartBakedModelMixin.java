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

package net.fabricmc.fabric.mixin.renderer.client;

import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.MultiPartBakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(MultiPartBakedModel.class)
abstract class MultipartBakedModelMixin implements BakedModel {
	@Shadow
	@Final
	private List<MultiPartBakedModel.Selector> selectors;

	@Shadow
	@Final
	private Map<BlockState, BitSet> selectorCache;

	@Unique
	private boolean isVanilla = true;

	@Inject(at = @At("RETURN"), method = "<init>")
	private void onInit(List<MultiPartBakedModel.Selector> selectors, CallbackInfo ci) {
		for (MultiPartBakedModel.Selector selector : selectors) {
			if (!selector.model().isVanillaAdapter()) {
				isVanilla = false;
				break;
			}
		}
	}

	@Override
	public boolean isVanillaAdapter() {
		return isVanilla;
	}

	@Override
	public void emitBlockQuads(QuadEmitter emitter, BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, Predicate<@Nullable Direction> cullTest) {
		BitSet bitSet = this.selectorCache.get(state);

		if (bitSet == null) {
			bitSet = new BitSet();

			for (int i = 0; i < this.selectors.size(); i++) {
				MultiPartBakedModel.Selector selector = selectors.get(i);

				if (selector.condition().test(state)) {
					bitSet.set(i);
				}
			}

			selectorCache.put(state, bitSet);
		}

		RandomSource random = randomSupplier.get();
		// Imitate vanilla passing a new random to the submodels
		long randomSeed = random.nextLong();
		Supplier<RandomSource> subModelRandomSupplier = () -> {
			random.setSeed(randomSeed);
			return random;
		};

		for (int i = 0; i < this.selectors.size(); i++) {
			if (bitSet.get(i)) {
				selectors.get(i).model().emitBlockQuads(emitter, blockView, state, pos, subModelRandomSupplier, cullTest);
			}
		}
	}

	@Override
	public void emitItemQuads(QuadEmitter emitter, Supplier<RandomSource> randomSupplier) {
		// Vanilla doesn't use MultipartBakedModel for items.
	}
}
