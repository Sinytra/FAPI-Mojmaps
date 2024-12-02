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
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.WeightedBakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(WeightedBakedModel.class)
abstract class WeightedBakedModelMixin implements FabricBakedModel {
	@Shadow
	@Final
	private SimpleWeightedRandomList<BakedModel> list;

	@Unique
	private boolean isVanilla = true;

	@Inject(at = @At("RETURN"), method = "<init>")
	private void onInit(SimpleWeightedRandomList<BakedModel> dataPool, CallbackInfo ci) {
		for (WeightedEntry.Wrapper<BakedModel> model : list.unwrap()) {
			if (!model.data().isVanillaAdapter()) {
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
		BakedModel selected = this.list.getRandomValue(randomSupplier.get()).orElse(null);

		if (selected != null) {
			selected.emitBlockQuads(emitter, blockView, state, pos, () -> {
				RandomSource random = randomSupplier.get();
				random.nextInt(); // Imitate vanilla modifying the random before passing it to the submodel
				return random;
			}, cullTest);
		}
	}

	@Override
	public void emitItemQuads(QuadEmitter emitter, Supplier<RandomSource> randomSupplier) {
		BakedModel selected = this.list.getRandomValue(randomSupplier.get()).orElse(null);

		if (selected != null) {
			selected.emitItemQuads(emitter, () -> {
				RandomSource random = randomSupplier.get();
				random.nextInt(); // Imitate vanilla modifying the random before passing it to the submodel
				return random;
			});
		}
	}
}
