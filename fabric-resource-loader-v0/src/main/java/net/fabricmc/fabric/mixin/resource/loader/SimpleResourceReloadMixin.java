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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ProfiledReloadInstance;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleReloadInstance;
import net.minecraft.util.Unit;

import net.fabricmc.fabric.impl.resource.loader.FabricLifecycledResourceManager;
import net.fabricmc.fabric.impl.resource.loader.ResourceManagerHelperImpl;

@Mixin(SimpleReloadInstance.class)
public class SimpleResourceReloadMixin {
	@Unique
	private static final ThreadLocal<PackType> fabric_resourceType = new ThreadLocal<>();

	@Inject(method = "create", at = @At("HEAD"))
	private static void method_40087(ResourceManager resourceManager, List<PreparableReloadListener> list, Executor executor, Executor executor2, CompletableFuture<Unit> completableFuture, boolean bl, CallbackInfoReturnable<ReloadInstance> cir) {
		if (resourceManager instanceof FabricLifecycledResourceManager flrm) {
			fabric_resourceType.set(flrm.fabric_getResourceType());
		}
	}

	@ModifyArg(method = "create", index = 1, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/resources/SimpleReloadInstance;of(Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/util/List;Ljava/util/concurrent/Executor;Ljava/util/concurrent/Executor;Ljava/util/concurrent/CompletableFuture;)Lnet/minecraft/server/packs/resources/SimpleReloadInstance;"))
	private static List<PreparableReloadListener> sortSimple(List<PreparableReloadListener> reloaders) {
		List<PreparableReloadListener> sorted = ResourceManagerHelperImpl.sort(fabric_resourceType.get(), reloaders);
		fabric_resourceType.set(null);
		return sorted;
	}

	@Redirect(method = "create", at = @At(value = "NEW", target = "(Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/util/List;Ljava/util/concurrent/Executor;Ljava/util/concurrent/Executor;Ljava/util/concurrent/CompletableFuture;)Lnet/minecraft/server/packs/resources/ProfiledReloadInstance;"))
	private static ProfiledReloadInstance sortProfiled(ResourceManager manager, List<PreparableReloadListener> reloaders, Executor prepareExecutor, Executor applyExecutor, CompletableFuture<Unit> initialStage) {
		List<PreparableReloadListener> sorted = ResourceManagerHelperImpl.sort(fabric_resourceType.get(), reloaders);
		fabric_resourceType.set(null);
		return new ProfiledReloadInstance(manager, sorted, prepareExecutor, applyExecutor, initialStage);
	}
}
