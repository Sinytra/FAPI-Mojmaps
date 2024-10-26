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

package net.fabricmc.fabric.mixin.resource.loader;

import java.util.Objects;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.fabricmc.fabric.impl.resource.loader.FabricRecipeManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.crafting.RecipeManager;

@Mixin(RecipeManager.class)
public class ServerRecipeManagerMixin implements FabricRecipeManager {
	@Unique
	private HolderLookup.Provider registries;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void init(HolderLookup.Provider registries, CallbackInfo ci) {
		this.registries = registries;
	}

	@Override
	public HolderLookup.Provider fabric_getRegistries() {
		return Objects.requireNonNull(registries);
	}
}