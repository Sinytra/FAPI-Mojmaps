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

package net.fabricmc.fabric.test.tag.convention.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalEnchantmentTags;
import net.fabricmc.fabric.api.tag.convention.v2.TagUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;

public class TagUtilTest implements ModInitializer {
	private static final Logger LOGGER = LoggerFactory.getLogger(TagUtilTest.class);

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			if (!TagUtil.isIn(server.registryAccess(), ConventionalEnchantmentTags.INCREASE_BLOCK_DROPS, server.registryAccess().registryOrThrow(Registries.ENCHANTMENT).get(Enchantments.FORTUNE))) {
				throw new AssertionError("Failed to find fortune in c:increase_block_drops!");
			}

			if (TagUtil.isIn(ConventionalBiomeTags.IS_OVERWORLD, server.registryAccess().registryOrThrow(Registries.BIOME).get(Biomes.BADLANDS))) {
				throw new AssertionError("Found a dynamic entry in a static registry?!");
			}

			// If this fails, the tag is missing a biome or the util is broken
			if (!TagUtil.isIn(server.registryAccess(), ConventionalBiomeTags.IS_OVERWORLD, server.registryAccess().registryOrThrow(Registries.BIOME).get(Biomes.BADLANDS))) {
				throw new AssertionError("Failed to find an overworld biome (%s) in c:in_overworld!".formatted(Biomes.BADLANDS));
			}

			if (!TagUtil.isIn(server.registryAccess(), ConventionalBlockTags.ORES, Blocks.DIAMOND_ORE)) {
				throw new AssertionError("Failed to find diamond ore in c:ores!");
			}

			//Success!
			LOGGER.info("Completed TagUtil tests!");
		});
	}
}
