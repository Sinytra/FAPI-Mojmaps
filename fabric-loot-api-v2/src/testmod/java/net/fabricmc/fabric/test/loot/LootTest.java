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

package net.fabricmc.fabric.test.loot;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.fabricmc.fabric.api.loot.v2.LootTableSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetNameFunction;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;

public class LootTest implements ModInitializer {
	@Override
	public void onInitialize() {
		// Test loot table load event
		// The LootTable.Builder LootPool.Builder methods here should use
		// prebuilt entries and pools to test the injected methods.
		LootTableEvents.REPLACE.register((key, original, source) -> {
			if (Blocks.BLACK_WOOL.getLootTable() == key) {
				if (source != LootTableSource.VANILLA) {
					throw new AssertionError("black wool loot table should have LootTableSource.VANILLA, got " + source);
				}

				// Replace black wool drops with an iron ingot
				LootPool pool = LootPool.lootPool()
						.with(LootItem.lootTableItem(Items.IRON_INGOT).build())
						.build();

				return LootTable.lootTable().pool(pool).build();
			}

			return null;
		});

		// Test that the event is stopped when the loot table is replaced
		LootTableEvents.REPLACE.register((key, original, source) -> {
			if (Blocks.BLACK_WOOL.getLootTable() == key) {
				throw new AssertionError("Event should have been stopped from replaced loot table");
			}

			return null;
		});

		LootTableEvents.MODIFY.register((key, tableBuilder, source) -> {
			if (Blocks.BLACK_WOOL.getLootTable() == key && source != LootTableSource.REPLACED) {
				throw new AssertionError("black wool loot table should have LootTableSource.REPLACED, got " + source);
			}

			if (Blocks.WHITE_WOOL.getLootTable() == key) {
				if (source != LootTableSource.VANILLA) {
					throw new AssertionError("white wool loot table should have LootTableSource.VANILLA, got " + source);
				}

				// Add gold ingot with custom name to white wool drops
				LootPool pool = LootPool.lootPool()
						.with(LootItem.lootTableItem(Items.GOLD_INGOT).build())
						.conditionally(ExplosionCondition.survivesExplosion().build())
						.apply(SetNameFunction.setName(Component.literal("Gold from White Wool"), SetNameFunction.Target.CUSTOM_NAME).build())
						.build();

				tableBuilder.pool(pool);
			}

			// We modify red wool to drop diamonds in the test mod resources.
			if (Blocks.RED_WOOL.getLootTable() == key && source != LootTableSource.MOD) {
				throw new AssertionError("red wool loot table should have LootTableSource.MOD, got " + source);
			}

			// Modify yellow wool to drop *either* yellow wool or emeralds by adding
			// emeralds to the same loot pool.
			if (Blocks.YELLOW_WOOL.getLootTable() == key) {
				tableBuilder.modifyPools(poolBuilder -> poolBuilder.add(LootItem.lootTableItem(Items.EMERALD)));
			}
		});

		LootTableEvents.ALL_LOADED.register((resourceManager, lootRegistry) -> {
			LootTable blackWoolTable = lootRegistry.get(Blocks.BLACK_WOOL.getLootTable());

			if (blackWoolTable == LootTable.EMPTY) {
				throw new AssertionError("black wool loot table should not be empty");
			}
		});
	}
}
