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

package net.fabricmc.fabric.mixin.loot;

import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.fabricmc.fabric.impl.loot.LootUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.world.level.storage.loot.LootDataType;

@Mixin(SimpleJsonResourceReloadListener.class)
public class JsonDataLoaderMixin {
	@Inject(method = "scanDirectory", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/resources/FileToIdConverter;fileToId(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/resources/ResourceLocation;", shift = At.Shift.AFTER))
	private static void fillSourceMap(ResourceManager manager, String dataType, Gson gson, Map<ResourceLocation, JsonElement> results, CallbackInfo ci, @Local Map.Entry<ResourceLocation, Resource> entry, @Local(ordinal = 1) ResourceLocation id) {
		if (!LootDataType.TABLE.registryKey().location().getPath().equals(dataType)) return;

		LootUtil.SOURCES.get().put(id, LootUtil.determineSource(entry.getValue()));
	}
}
