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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.registry.CompostingChanceRegistry;
import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistryBuilder;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.registry.FlattenableBlockRegistry;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.fabricmc.fabric.api.registry.LandPathNodeTypesRegistry;
import net.fabricmc.fabric.api.registry.OxidizableBlocksRegistry;
import net.fabricmc.fabric.api.registry.SculkSensorFrequencyRegistry;
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry;
import net.fabricmc.fabric.api.registry.TillableBlockRegistry;
import net.fabricmc.fabric.api.registry.VillagerInteractionRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.BlockHitResult;

public final class ContentRegistryTest implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger(ContentRegistryTest.class);

	public static final ResourceLocation TEST_EVENT_ID = ResourceLocation.fromNamespaceAndPath("fabric-content-registries-v0-testmod", "test_event");
	public static final Holder.Reference<GameEvent> TEST_EVENT = Registry.registerForHolder(BuiltInRegistries.GAME_EVENT, TEST_EVENT_ID, new GameEvent(GameEvent.DEFAULT_NOTIFICATION_RADIUS));

	@Override
	public void onInitialize() {
		// Expected behavior:
		//  - obsidian is now compostable
		//  - diamond block is now flammable
		//  - sand is now flammable
		//  - red wool is flattenable to yellow wool
		//  - obsidian is now fuel
		//  - all items with the tag 'minecraft:dirt' are now fuel
		//  - dead bush is now considered as a dangerous block like sweet berry bushes (all entities except foxes should avoid it)
		//  - quartz pillars are strippable to hay blocks
		//  - green wool is tillable to lime wool
		//  - copper ore, iron ore, gold ore, and diamond ore can be waxed into their deepslate variants and scraped back again
		//  - aforementioned ores can be scraped from diamond -> gold -> iron -> copper
		//  - villagers can now collect, consume (at the same level of bread) and compost apples
		//  - villagers can now collect oak saplings
		//  - assign a loot table to the nitwit villager type
		//  - right-clicking a 'test_event' block will emit a 'test_event' game event, which will have a sculk sensor frequency of 2
		//  - instant health potions can be brewed from awkward potions with any item in the 'minecraft:small_flowers' tag
		//  - if Bundle experiment is enabled, luck potions can be brewed from awkward potions with a bundle
		//  - dirty potions can be brewed by adding any item in the 'minecraft:dirt' tag to any standard potion

		CompostingChanceRegistry.INSTANCE.add(Items.OBSIDIAN, 0.5F);
		FlammableBlockRegistry.getDefaultInstance().add(Blocks.DIAMOND_BLOCK, 4, 4);
		FlammableBlockRegistry.getDefaultInstance().add(BlockTags.SAND, 4, 4);
		FlattenableBlockRegistry.register(Blocks.RED_WOOL, Blocks.YELLOW_WOOL.defaultBlockState());
		FuelRegistry.INSTANCE.add(Items.OBSIDIAN, 50);
		FuelRegistry.INSTANCE.add(ItemTags.DIRT, 100);
		LandPathNodeTypesRegistry.register(Blocks.DEAD_BUSH, PathType.DAMAGE_OTHER, PathType.DANGER_OTHER);
		StrippableBlockRegistry.register(Blocks.QUARTZ_PILLAR, Blocks.HAY_BLOCK);

		// assert that StrippableBlockRegistry throws when the blocks don't have 'axis'
		try {
			StrippableBlockRegistry.register(Blocks.BLUE_WOOL, Blocks.OAK_LOG);
			StrippableBlockRegistry.register(Blocks.HAY_BLOCK, Blocks.BLUE_WOOL);
			throw new AssertionError("StrippableBlockRegistry didn't throw when blocks were missing the 'axis' property!");
		} catch (IllegalArgumentException e) {
			// expected behavior
			LOGGER.info("StrippableBlockRegistry test passed!");
		}

		TillableBlockRegistry.register(Blocks.GREEN_WOOL, context -> true, HoeItem.changeIntoState(Blocks.LIME_WOOL.defaultBlockState()));

		OxidizableBlocksRegistry.registerOxidizableBlockPair(Blocks.COPPER_ORE, Blocks.IRON_ORE);
		OxidizableBlocksRegistry.registerOxidizableBlockPair(Blocks.IRON_ORE, Blocks.GOLD_ORE);
		OxidizableBlocksRegistry.registerOxidizableBlockPair(Blocks.GOLD_ORE, Blocks.DIAMOND_ORE);

		OxidizableBlocksRegistry.registerWaxableBlockPair(Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE);
		OxidizableBlocksRegistry.registerWaxableBlockPair(Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE);
		OxidizableBlocksRegistry.registerWaxableBlockPair(Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE);
		OxidizableBlocksRegistry.registerWaxableBlockPair(Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE);

		// assert that OxidizableBlocksRegistry throws when registered blocks are null
		try {
			OxidizableBlocksRegistry.registerOxidizableBlockPair(Blocks.EMERALD_ORE, null);
			OxidizableBlocksRegistry.registerOxidizableBlockPair(null, Blocks.COAL_ORE);

			OxidizableBlocksRegistry.registerWaxableBlockPair(null, Blocks.DEAD_BRAIN_CORAL);
			OxidizableBlocksRegistry.registerWaxableBlockPair(Blocks.BRAIN_CORAL, null);

			throw new AssertionError("OxidizableBlocksRegistry didn't throw when blocks were null!");
		} catch (NullPointerException e) {
			// expected behavior
			LOGGER.info("OxidizableBlocksRegistry test passed!");
		}

		VillagerInteractionRegistries.registerCollectable(Items.APPLE);
		VillagerInteractionRegistries.registerFood(Items.APPLE, 4);
		VillagerInteractionRegistries.registerCompostable(Items.APPLE);

		VillagerInteractionRegistries.registerCollectable(Items.OAK_SAPLING);

		VillagerInteractionRegistries.registerGiftLootTable(VillagerProfession.NITWIT, ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.withDefaultNamespace("fake_loot_table")));

		Registry.register(BuiltInRegistries.BLOCK, TEST_EVENT_ID, new TestEventBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)));
		SculkSensorFrequencyRegistry.register(TEST_EVENT.key(), 2);

		// assert that SculkSensorFrequencyRegistry throws when registering a frequency outside the allowed range
		try {
			SculkSensorFrequencyRegistry.register(GameEvent.SHRIEK.key(), 18);

			throw new AssertionError("SculkSensorFrequencyRegistry didn't throw when frequency was outside allowed range!");
		} catch (IllegalArgumentException e) {
			// expected behavior
			LOGGER.info("SculkSensorFrequencyRegistry test passed!");
		}

		var dirtyPotion = new DirtyPotionItem(new Item.Properties().stacksTo(1));
		Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath("fabric-content-registries-v0-testmod", "dirty_potion"),
				dirtyPotion);
		/* Mods should use BrewingRecipeRegistry.registerPotionType(Item), which is access widened by fabric-transitive-access-wideners-v1
		 * This testmod uses an accessor due to Loom limitations that prevent TAWs from applying across Gradle subproject boundaries */
		FabricBrewingRecipeRegistryBuilder.BUILD.register(builder -> {
			builder.addContainer(dirtyPotion);
			builder.registerItemRecipe(Items.POTION, Ingredient.of(ItemTags.DIRT), dirtyPotion);
			builder.registerPotionRecipe(Potions.AWKWARD, Ingredient.of(ItemTags.SMALL_FLOWERS), Potions.HEALING);

			if (builder.getEnabledFeatures().contains(FeatureFlags.BUNDLE)) {
				builder.registerPotionRecipe(Potions.AWKWARD, Ingredient.of(Items.BUNDLE), Potions.LUCK);
			}
		});
	}

	public static class TestEventBlock extends Block {
		public TestEventBlock(Properties settings) {
			super(settings);
		}

		@Override
		public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
			// Emit the test event
			world.gameEvent(player, TEST_EVENT, pos);
			return InteractionResult.SUCCESS;
		}
	}

	public static class DirtyPotionItem extends PotionItem {
		public DirtyPotionItem(Properties settings) {
			super(settings);
		}

		@Override
		public Component getName(ItemStack stack) {
			return Component.literal("Dirty ").append(Items.POTION.getName(stack));
		}
	}
}
