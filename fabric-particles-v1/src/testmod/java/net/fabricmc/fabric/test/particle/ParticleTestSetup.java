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

package net.fabricmc.fabric.test.particle;

import com.mojang.brigadier.Command;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ParticleTestSetup implements ModInitializer {
	// The dust particles of this block are always tinted (default).
	public static final Block ALWAYS_TINTED = new ParticleTintTestBlock(BlockBehaviour.Properties.of().instabreak(), 0xFF00FF);
	// The dust particles of this block are only tinted when the block is broken over water.
	public static final Block TINTED_OVER_WATER = new ParticleTintTestBlock(BlockBehaviour.Properties.of().instabreak(), 0xFFFF00);
	// The dust particles of this block are never tinted.
	public static final Block NEVER_TINTED = new ParticleTintTestBlock(BlockBehaviour.Properties.of().instabreak(), 0x00FFFF);

	@Override
	public void onInitialize() {
		registerBlock("always_tinted", ALWAYS_TINTED);
		registerBlock("tinted_over_water", TINTED_OVER_WATER);
		registerBlock("never_tinted", NEVER_TINTED);

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(Commands.literal("addparticletestblocks").executes(context -> {
				Inventory inventory = context.getSource().getPlayerOrException().getInventory();
				inventory.placeItemBackInInventory(new ItemStack(ALWAYS_TINTED));
				inventory.placeItemBackInInventory(new ItemStack(TINTED_OVER_WATER));
				inventory.placeItemBackInInventory(new ItemStack(NEVER_TINTED));
				return Command.SINGLE_SUCCESS;
			}));
		});
	}

	private static void registerBlock(String path, Block block) {
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath("fabric-particles-v1-testmod", path);
		Registry.register(BuiltInRegistries.BLOCK, id, block);
		Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(block, new Item.Properties()));
	}
}
