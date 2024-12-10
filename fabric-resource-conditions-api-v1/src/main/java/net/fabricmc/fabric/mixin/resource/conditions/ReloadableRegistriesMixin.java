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

import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.server.packs.resources.ResourceManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Should apply before Loot API.
@Mixin(value = ReloadableServerRegistries.class, priority = 900)
public class ReloadableRegistriesMixin {
	// The cross-thread nature of the stuff makes this necessary. It is technically possible to query the wrapper from
	// the ops, but it requires more mixins.
	// Key refers to value, but value does not refer to key, so WeakHashMap is fine.
	@Unique
	private static final WeakHashMap<RegistryOps<?>, HolderLookup.Provider> REGISTRY_LOOKUPS = new WeakHashMap<>();

	@WrapOperation(method = "reload", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/HolderLookup$Provider;create(Ljava/util/stream/Stream;)Lnet/minecraft/core/HolderLookup$Provider;"))
	private static HolderLookup.Provider storeWrapperLookup(Stream<HolderLookup.RegistryLookup<?>> wrappers, Operation<HolderLookup.Provider> original, @Share("wrapper") LocalRef<HolderLookup.Provider> share) {
		HolderLookup.Provider lookup = original.call(wrappers);
		share.set(lookup);
		return lookup;
	}

	@Inject(method = "reload", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/core/HolderLookup$Provider;createSerializationContext(Lcom/mojang/serialization/DynamicOps;)Lnet/minecraft/resources/RegistryOps;", shift = At.Shift.AFTER))
	private static void storeWrapperLookup(LayeredRegistryAccess<RegistryLayer> dynamicRegistries, List<Registry.PendingTags<?>> pendingTagLoads, ResourceManager resourceManager, Executor prepareExecutor, CallbackInfoReturnable<CompletableFuture<ReloadableServerRegistries.LoadResult>> cir, @Local RegistryOps ops, @Share("wrapper") LocalRef<HolderLookup.Provider> share) {
		REGISTRY_LOOKUPS.put(ops, share.get());
	}
}
