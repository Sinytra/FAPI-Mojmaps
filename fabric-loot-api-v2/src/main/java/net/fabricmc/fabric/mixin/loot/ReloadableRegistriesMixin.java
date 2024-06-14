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

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.fabricmc.fabric.api.loot.v2.FabricLootTableBuilder;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.fabricmc.fabric.api.loot.v2.LootTableSource;
import net.fabricmc.fabric.impl.loot.LootUtil;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Implements the events from {@link LootTableEvents}.
 */
@Mixin(ReloadableServerRegistries.class)
abstract class ReloadableRegistriesMixin {
	@ModifyArg(method = "lambda$scheduleElementParse$2", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/WritableRegistry;register(Lnet/minecraft/resources/ResourceKey;Ljava/lang/Object;Lnet/minecraft/core/RegistrationInfo;)Lnet/minecraft/core/Holder$Reference;"), index = 1)
	private static Object modifyLootTable(Object value, @Local(argsOnly = true) ResourceLocation id) {
		if (!(value instanceof LootTable table)) return value;

		if (table == LootTable.EMPTY) {
			// This is a special table and cannot be modified.
			return value;
		}

		ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, id);
		// Populated inside JsonDataLoaderMixin
		LootTableSource source = LootUtil.SOURCES.get().getOrDefault(id, LootTableSource.DATA_PACK);
		// Invoke the REPLACE event for the current loot table.
		LootTable replacement = LootTableEvents.REPLACE.invoker().replaceLootTable(key, table, source);

		if (replacement != null) {
			// Set the loot table to MODIFY to be the replacement loot table.
			// The MODIFY event will also see it as a replaced loot table via the source.
			table = replacement;
			source = LootTableSource.REPLACED;
		}

		// Turn the current table into a modifiable builder and invoke the MODIFY event.
		LootTable.Builder builder = FabricLootTableBuilder.copyOf(table);
		LootTableEvents.MODIFY.invoker().modifyLootTable(key, builder, source);

		return builder.build();
	}

	@SuppressWarnings("unchecked")
	@Inject(method = "lambda$scheduleElementParse$4", at = @At("RETURN"))
	private static void onLootTablesLoaded(LootDataType lootDataType, ResourceManager resourceManager, RegistryOps registryOps, CallbackInfoReturnable<WritableRegistry> cir) {
		if (lootDataType != LootDataType.TABLE) return;

		LootTableEvents.ALL_LOADED.invoker().onLootTablesLoaded(resourceManager, (Registry<LootTable>) cir.getReturnValue());
		LootUtil.SOURCES.remove();
	}
}
