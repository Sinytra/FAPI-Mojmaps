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

package net.fabricmc.fabric.test.content.registry;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ContentRegistryGameTest {
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void testCompostingChanceRegistry(GameTestHelper context) {
		BlockPos pos = new BlockPos(0, 1, 0);
		context.setBlock(pos, Blocks.COMPOSTER);
		ItemStack obsidian = new ItemStack(Items.OBSIDIAN, 64);
		Player player = context.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, obsidian);
		// If on level 0, composting always increases composter level
		context.useBlock(pos, player);
		context.assertBlockProperty(pos, ComposterBlock.LEVEL, 1);
		context.assertValueEqual(obsidian.getCount(), 63, "obsidian stack count");
		context.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void testFlattenableBlockRegistry(GameTestHelper context) {
		BlockPos pos = new BlockPos(0, 1, 0);
		context.setBlock(pos, Blocks.RED_WOOL);
		ItemStack shovel = new ItemStack(Items.NETHERITE_SHOVEL);
		Player player = context.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, shovel);
		context.useBlock(pos, player);
		context.assertBlockPresent(Blocks.YELLOW_WOOL, pos);
		context.assertValueEqual(shovel.getDamageValue(), 1, "shovel damage");
		context.succeed();
	}

	private void smelt(GameTestHelper context, ItemStack fuelStack, BiConsumer<AbstractFurnaceBlockEntity, HopperBlockEntity> callback) {
		// Create a furnace to simulate smelting in
		// A blast furnace will smelt twice as fast, so it is used here
		var furnacePos = new BlockPos(0, 1, 0);
		BlockState furnaceState = Blocks.BLAST_FURNACE.defaultBlockState();

		context.setBlock(furnacePos, furnaceState);

		if (!(context.getBlockEntity(furnacePos) instanceof AbstractFurnaceBlockEntity furnace)) {
			throw new AssertionError("Furnace was not placed");
		}

		// Create a hopper that attempts to insert fuel into the furnace
		BlockPos hopperPos = furnacePos.east();
		BlockState hopperState = Blocks.HOPPER.defaultBlockState()
				.setValue(HopperBlock.FACING, context.getTestRotation().rotate(Direction.WEST));

		context.setBlock(hopperPos, hopperState);

		if (!(context.getBlockEntity(hopperPos) instanceof HopperBlockEntity hopper)) {
			throw new AssertionError("Hopper was not placed");
		}

		// Insert the fuel into the hopper, which transfers it into the furnace
		hopper.setItem(0, fuelStack.copy());

		// Insert the item that should be smelted into the furnace
		// Smelting a single item takes 200 fuel time
		furnace.setItem(0, new ItemStack(Items.RAW_IRON, 1));

		context.runAfterDelay(105, () -> callback.accept(furnace, hopper));
	}

	private void smeltCompleted(GameTestHelper context, ItemStack fuelStack) {
		smelt(context, fuelStack, (furnace, hopper) -> {
			context.assertTrue(hopper.isEmpty(), "fuel hopper should have been emptied");

			context.assertTrue(furnace.getItem(0).isEmpty(), "furnace input slot should have been emptied");
			context.assertTrue(furnace.getItem(0).isEmpty(), "furnace fuel slot should have been emptied");
			context.assertTrue(ItemStack.matches(furnace.getItem(2), new ItemStack(Items.IRON_INGOT, 1)), "one iron ingot should have been smelted and placed into the furnace output slot");

			context.succeed();
		});
	}

	private void smeltFailed(GameTestHelper context, ItemStack fuelStack) {
		smelt(context, fuelStack, (furnace, hopper) -> {
			context.assertTrue(ItemStack.matches(hopper.getItem(0), fuelStack), "fuel hopper should not have been emptied");

			context.assertTrue(ItemStack.matches(furnace.getItem(0), new ItemStack(Items.RAW_IRON, 1)), "furnace input slot should not have been emptied");
			context.assertTrue(furnace.getItem(1).isEmpty(), "furnace fuel slot should not have been filled");
			context.assertTrue(furnace.getItem(2).isEmpty(), "furnace output slot should not have been filled");

			context.succeed();
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 110)
	public void testSmeltingFuelIncludedByItem(GameTestHelper context) {
		// Item with 50 fuel time x4 = 200 fuel time
		smeltCompleted(context, new ItemStack(ContentRegistryTest.SMELTING_FUEL_INCLUDED_BY_ITEM, 4));
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 110)
	public void testSmeltingFuelIncludedByTag(GameTestHelper context) {
		// Item in tag with 100 fuel time x2 = 200 fuel time
		smeltCompleted(context, new ItemStack(ContentRegistryTest.SMELTING_FUEL_INCLUDED_BY_TAG, 2));
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 110)
	public void testSmeltingFuelExcludedByTag(GameTestHelper context) {
		// Item is in both the smelting fuels tag and the excluded smithing fuels tag
		smeltFailed(context, new ItemStack(ContentRegistryTest.SMELTING_FUEL_EXCLUDED_BY_TAG));
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 110)
	public void testSmeltingFuelExcludedByVanillaTag(GameTestHelper context) {
		// Item is in both the smelting fuel tag and vanilla's excluded non-flammable wood tag
		smeltFailed(context, new ItemStack(ContentRegistryTest.SMELTING_FUEL_EXCLUDED_BY_VANILLA_TAG));
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void testStrippableBlockRegistry(GameTestHelper context) {
		BlockPos pos = new BlockPos(0, 1, 0);
		context.setBlock(pos, Blocks.QUARTZ_PILLAR);
		ItemStack axe = new ItemStack(Items.NETHERITE_AXE);
		Player player = context.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, axe);
		context.useBlock(pos, player);
		context.assertBlockPresent(Blocks.HAY_BLOCK, pos);
		context.assertValueEqual(axe.getDamageValue(), 1, "axe damage");
		context.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void testTillableBlockRegistry(GameTestHelper context) {
		BlockPos pos = new BlockPos(0, 1, 0);
		context.setBlock(pos, Blocks.GREEN_WOOL);
		ItemStack hoe = new ItemStack(Items.NETHERITE_HOE);
		Player player = context.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, hoe);
		context.useBlock(pos, player);
		context.assertBlockPresent(Blocks.LIME_WOOL, pos);
		context.assertValueEqual(hoe.getDamageValue(), 1, "hoe damage");
		context.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void testOxidizableBlocksRegistry(GameTestHelper context) {
		// Test de-oxidation. (the registry does not make the blocks oxidize.)
		Player player = context.makeMockPlayer(GameType.SURVIVAL);
		BlockPos pos = new BlockPos(0, 1, 0);
		context.setBlock(pos, Blocks.DIAMOND_ORE);
		ItemStack axe = new ItemStack(Items.NETHERITE_AXE);
		player.setItemInHand(InteractionHand.MAIN_HAND, axe);
		context.useBlock(pos, player);
		context.assertBlockPresent(Blocks.GOLD_ORE, pos);
		context.assertValueEqual(axe.getDamageValue(), 1, "axe damage");
		context.useBlock(pos, player);
		context.assertBlockPresent(Blocks.IRON_ORE, pos);
		context.useBlock(pos, player);
		context.assertBlockPresent(Blocks.COPPER_ORE, pos);
		context.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void testWaxableBlocksRegistry(GameTestHelper context) {
		Player player = context.makeMockPlayer(GameType.SURVIVAL);
		BlockPos pos = new BlockPos(0, 1, 0);
		context.setBlock(pos, Blocks.DIAMOND_ORE);
		ItemStack honeycomb = new ItemStack(Items.HONEYCOMB, 64);
		player.setItemInHand(InteractionHand.MAIN_HAND, honeycomb);
		context.useBlock(pos, player);
		context.assertBlockPresent(Blocks.DEEPSLATE_DIAMOND_ORE, pos);
		context.assertValueEqual(honeycomb.getCount(), 63, "honeycomb count");
		ItemStack axe = new ItemStack(Items.NETHERITE_AXE);
		player.setItemInHand(InteractionHand.MAIN_HAND, axe);
		context.useBlock(pos, player);
		context.assertBlockPresent(Blocks.DIAMOND_ORE, pos);
		context.assertValueEqual(axe.getDamageValue(), 1, "axe damage");
		context.succeed();
	}

	private void brew(GameTestHelper context, ItemStack input, ItemStack bottle, Consumer<BrewingStandBlockEntity> callback) {
		BlockPos pos = new BlockPos(0, 1, 0);
		context.setBlock(pos, Blocks.BREWING_STAND);

		if (!(context.getBlockEntity(pos) instanceof BrewingStandBlockEntity brewingStand)) {
			throw new AssertionError("Brewing stand was not placed");
		}

		brewingStand.setItem(0, bottle);
		brewingStand.setItem(3, input);
		brewingStand.setItem(4, new ItemStack(Items.BLAZE_POWDER, 64));
		context.runAfterDelay(401, () -> callback.accept(brewingStand));
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 410)
	public void testBrewingFlower(GameTestHelper context) {
		brew(context, new ItemStack(Items.DANDELION), PotionContents.createItemStack(Items.POTION, Potions.AWKWARD), brewingStand -> {
			ItemStack bottle = brewingStand.getItem(0);
			PotionContents potion = bottle.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
			context.assertValueEqual(potion.potion().orElseThrow(), Potions.HEALING, "brewed potion");
			context.succeed();
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 410)
	public void testBrewingDirt(GameTestHelper context) {
		brew(context, new ItemStack(Items.DIRT), PotionContents.createItemStack(Items.POTION, Potions.AWKWARD), brewingStand -> {
			ItemStack bottle = brewingStand.getItem(0);
			context.assertTrue(bottle.getItem() instanceof ContentRegistryTest.DirtyPotionItem, "potion became dirty");
			context.succeed();
		});
	}
}
