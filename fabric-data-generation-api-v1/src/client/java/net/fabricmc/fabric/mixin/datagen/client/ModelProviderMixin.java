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

package net.fabricmc.fabric.mixin.datagen.client;

import java.util.concurrent.CompletableFuture;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.impl.datagen.client.FabricModelProviderDefinitions;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;

@Mixin(ModelProvider.class)
public class ModelProviderMixin {
	@Unique
	private FabricDataOutput fabricDataOutput;

	@Inject(method = "<init>", at = @At("RETURN"))
	public void init(PackOutput output, CallbackInfo ci) {
		if (output instanceof FabricDataOutput fabricDataOutput) {
			this.fabricDataOutput = fabricDataOutput;
		}
	}

	@Redirect(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/data/models/BlockModelGenerators;run()V"))
	private void registerBlockStateModels(BlockModelGenerators instance) {
		if (((Object) this) instanceof FabricModelProvider fabricModelProvider) {
			fabricModelProvider.generateBlockStateModels(instance);
		} else {
			// Fallback to the vanilla registration when not a fabric provider
			instance.run();
		}
	}

	@Redirect(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/data/models/ItemModelGenerators;run()V"))
	private void registerItemModels(ItemModelGenerators instance) {
		if (((Object) this) instanceof FabricModelProvider fabricModelProvider) {
			fabricModelProvider.generateItemModels(instance);
		} else {
			// Fallback to the vanilla registration when not a fabric provider
			instance.run();
		}
	}

	@Inject(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/data/models/BlockModelGenerators;run()V"))
	private void setFabricDataOutput(CachedOutput writer, CallbackInfoReturnable<CompletableFuture<?>> cir,
							@Local ModelProvider.BlockStateGeneratorCollector blockStateSuppliers,
							@Local ModelProvider.ItemInfoCollector itemAssets) {
		((FabricModelProviderDefinitions) blockStateSuppliers).setFabricDataOutput(fabricDataOutput);
		((FabricModelProviderDefinitions) itemAssets).setFabricDataOutput(fabricDataOutput);
	}
}
