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

package net.fabricmc.fabric.mixin.resource.conditions;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

import com.google.gson.JsonElement;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.Decoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.fabricmc.fabric.impl.resource.conditions.ResourceConditionsImpl;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.Resource;

@Mixin(RegistryDataLoader.class)
public class RegistryLoaderMixin {
	@Unique
	private static final ThreadLocal<RegistryAccess> REGISTRIES = new ThreadLocal<>();

	/**
	 * Capture the current registries, so they can be passed to the resource conditions.
	 */
	@WrapOperation(method = "load(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/core/RegistryAccess;Ljava/util/List;)Lnet/minecraft/core/RegistryAccess$Frozen;", at = @At(value = "INVOKE", target = "Lnet/minecraft/resources/RegistryDataLoader;load(Lnet/minecraft/resources/RegistryDataLoader$LoadingFunction;Lnet/minecraft/core/RegistryAccess;Ljava/util/List;)Lnet/minecraft/core/RegistryAccess$Frozen;"))
	private static RegistryAccess.Frozen captureRegistries(@Coerce Object registryLoadable, RegistryAccess baseRegistryManager, List<RegistryDataLoader.RegistryData<?>> entries, Operation<RegistryAccess.Frozen> original) {
		try {
			REGISTRIES.set(baseRegistryManager);
			return original.call(registryLoadable, baseRegistryManager, entries);
		} finally {
			REGISTRIES.remove();
		}
	}

	@Inject(
			method = "loadElementFromResource(Lnet/minecraft/core/WritableRegistry;Lcom/mojang/serialization/Decoder;Lnet/minecraft/resources/RegistryOps;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/server/packs/resources/Resource;Lnet/minecraft/core/RegistrationInfo;)V",
			at = @At(value = "INVOKE_ASSIGN", target = "Lcom/google/gson/JsonParser;parseReader(Ljava/io/Reader;)Lcom/google/gson/JsonElement;", remap = false),
			cancellable = true
	)
	private static <E> void checkResourceCondition(
			WritableRegistry<E> registry, Decoder<E> decoder, RegistryOps<JsonElement> ops, ResourceKey<E> key, Resource resource, RegistrationInfo entryInfo,
			CallbackInfo ci, @Local Reader reader, @Local JsonElement json
	) throws IOException {
		// This method is called both on the server (when loading resources) and on the client (when syncing from the
		// server). We only want to apply resource conditions when loading via loadFromResource.
		RegistryAccess registries = REGISTRIES.get();
		if (registries == null) return;

		if (json.isJsonObject() && !ResourceConditionsImpl.applyResourceConditions(json.getAsJsonObject(), key.registry().toString(), key.location(), registries)) {
			reader.close();
			ci.cancel();
		}
	}
}
