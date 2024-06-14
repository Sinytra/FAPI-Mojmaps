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

package net.fabricmc.fabric.test.recipe.ingredient;

import java.util.Objects;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.api.recipe.v1.ingredient.DefaultCustomIngredients;
import net.minecraft.Util;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;

public class IngredientMatchTests {
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void testAllIngredient(GameTestHelper context) {
		Ingredient allIngredient = DefaultCustomIngredients.all(Ingredient.of(Items.APPLE, Items.CARROT), Ingredient.of(Items.STICK, Items.CARROT));

		assertEquals(1, allIngredient.getItems().length);
		assertEquals(Items.CARROT, allIngredient.getItems()[0].getItem());
		assertEquals(false, allIngredient.isEmpty());

		assertEquals(false, allIngredient.test(new ItemStack(Items.APPLE)));
		assertEquals(true, allIngredient.test(new ItemStack(Items.CARROT)));
		assertEquals(false, allIngredient.test(new ItemStack(Items.STICK)));

		Ingredient emptyAllIngredient = DefaultCustomIngredients.all(Ingredient.of(Items.APPLE), Ingredient.of(Items.STICK));

		assertEquals(0, emptyAllIngredient.getItems().length);
		assertEquals(true, emptyAllIngredient.isEmpty());

		assertEquals(false, emptyAllIngredient.test(new ItemStack(Items.APPLE)));
		assertEquals(false, emptyAllIngredient.test(new ItemStack(Items.STICK)));

		context.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void testAnyIngredient(GameTestHelper context) {
		Ingredient anyIngredient = DefaultCustomIngredients.any(Ingredient.of(Items.APPLE, Items.CARROT), Ingredient.of(Items.STICK, Items.CARROT));

		assertEquals(4, anyIngredient.getItems().length);
		assertEquals(Items.APPLE, anyIngredient.getItems()[0].getItem());
		assertEquals(Items.CARROT, anyIngredient.getItems()[1].getItem());
		assertEquals(Items.STICK, anyIngredient.getItems()[2].getItem());;
		assertEquals(Items.CARROT, anyIngredient.getItems()[3].getItem());
		assertEquals(false, anyIngredient.isEmpty());

		assertEquals(true, anyIngredient.test(new ItemStack(Items.APPLE)));
		assertEquals(true, anyIngredient.test(new ItemStack(Items.CARROT)));
		assertEquals(true, anyIngredient.test(new ItemStack(Items.STICK)));

		context.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void testDifferenceIngredient(GameTestHelper context) {
		Ingredient differenceIngredient = DefaultCustomIngredients.difference(Ingredient.of(Items.APPLE, Items.CARROT), Ingredient.of(Items.STICK, Items.CARROT));

		assertEquals(1, differenceIngredient.getItems().length);
		assertEquals(Items.APPLE, differenceIngredient.getItems()[0].getItem());
		assertEquals(false, differenceIngredient.isEmpty());

		assertEquals(true, differenceIngredient.test(new ItemStack(Items.APPLE)));
		assertEquals(false, differenceIngredient.test(new ItemStack(Items.CARROT)));
		assertEquals(false, differenceIngredient.test(new ItemStack(Items.STICK)));

		context.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void testComponentIngredient(GameTestHelper context) {
		final Ingredient baseIngredient = Ingredient.of(Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE, Items.STICK);
		final Ingredient undamagedIngredient = DefaultCustomIngredients.components(
				baseIngredient,
				builder -> builder.set(DataComponents.DAMAGE, 0)
		);
		final Ingredient noNameUndamagedIngredient = DefaultCustomIngredients.components(
				baseIngredient,
				builder -> builder
						.set(DataComponents.DAMAGE, 0)
						.remove(DataComponents.CUSTOM_NAME)
		);

		ItemStack renamedUndamagedDiamondPickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
		renamedUndamagedDiamondPickaxe.set(DataComponents.CUSTOM_NAME, Component.literal("Renamed"));
		assertEquals(true, undamagedIngredient.test(renamedUndamagedDiamondPickaxe));
		assertEquals(false, noNameUndamagedIngredient.test(renamedUndamagedDiamondPickaxe));

		assertEquals(3, undamagedIngredient.getItems().length);
		ItemStack result0 = undamagedIngredient.getItems()[0];
		ItemStack result1 = undamagedIngredient.getItems()[1];

		assertEquals(Items.DIAMOND_PICKAXE, result0.getItem());
		assertEquals(Items.NETHERITE_PICKAXE, result1.getItem());
		assertEquals(DataComponentPatch.EMPTY, result0.getComponentsPatch());
		assertEquals(DataComponentPatch.EMPTY, result1.getComponentsPatch());
		assertEquals(false, undamagedIngredient.isEmpty());

		// Undamaged is fine
		assertEquals(true, undamagedIngredient.test(new ItemStack(Items.DIAMOND_PICKAXE)));
		assertEquals(true, undamagedIngredient.test(new ItemStack(Items.NETHERITE_PICKAXE)));

		// Damaged is not fine
		ItemStack damagedDiamondPickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
		damagedDiamondPickaxe.setDamageValue(10);
		assertEquals(false, undamagedIngredient.test(damagedDiamondPickaxe));

		// Checking for DAMAGE component requires the item is damageable in the first place
		assertEquals(false, undamagedIngredient.test(new ItemStack(Items.STICK)));

		// Custom data is strictly matched, like any other component with multiple fields
		final CompoundTag requiredData = new CompoundTag();
		requiredData.putInt("keyA", 1);
		final CompoundTag extraData = requiredData.copy();
		extraData.putInt("keyB", 2);

		final Ingredient customDataIngredient = DefaultCustomIngredients.components(
				baseIngredient,
				builder -> builder.set(DataComponents.CUSTOM_DATA, CustomData.of(requiredData))
		);
		ItemStack requiredDataStack = new ItemStack(Items.DIAMOND_PICKAXE);
		requiredDataStack.set(DataComponents.CUSTOM_DATA, CustomData.of(requiredData));
		ItemStack extraDataStack = new ItemStack(Items.DIAMOND_PICKAXE);
		extraDataStack.set(DataComponents.CUSTOM_DATA, CustomData.of(extraData));
		assertEquals(true, customDataIngredient.test(requiredDataStack));
		assertEquals(false, customDataIngredient.test(extraDataStack));

		// Default value is ignored in components(ItemStack)
		final Ingredient damagedPickaxeIngredient = DefaultCustomIngredients.components(renamedUndamagedDiamondPickaxe);
		ItemStack renamedDamagedDiamondPickaxe = renamedUndamagedDiamondPickaxe.copy();
		renamedDamagedDiamondPickaxe.setDamageValue(10);
		assertEquals(true, damagedPickaxeIngredient.test(renamedUndamagedDiamondPickaxe));
		assertEquals(true, damagedPickaxeIngredient.test(renamedDamagedDiamondPickaxe));

		context.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void testCustomDataIngredient(GameTestHelper context) {
		final CompoundTag requiredNbt = Util.make(new CompoundTag(), nbt -> {
			nbt.putInt("keyA", 1);
		});
		final CompoundTag acceptedNbt = Util.make(requiredNbt.copy(), nbt -> {
			nbt.putInt("keyB", 2);
		});
		final CompoundTag rejectedNbt1 = Util.make(new CompoundTag(), nbt -> {
			nbt.putInt("keyA", -1);
		});
		final CompoundTag rejectedNbt2 = Util.make(new CompoundTag(), nbt -> {
			nbt.putInt("keyB", 2);
		});

		final Ingredient baseIngredient = Ingredient.of(Items.STICK);
		final Ingredient customDataIngredient = DefaultCustomIngredients.customData(baseIngredient, requiredNbt);

		ItemStack stack = new ItemStack(Items.STICK);
		assertEquals(false, customDataIngredient.test(stack));
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(requiredNbt));
		assertEquals(true, customDataIngredient.test(stack));
		// This is a non-strict matching
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(acceptedNbt));
		assertEquals(true, customDataIngredient.test(stack));
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(rejectedNbt1));
		assertEquals(false, customDataIngredient.test(stack));
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(rejectedNbt2));
		assertEquals(false, customDataIngredient.test(stack));

		ItemStack[] matchingStacks = customDataIngredient.getItems();
		assertEquals(1, matchingStacks.length);
		assertEquals(Items.STICK, matchingStacks[0].getItem());
		assertEquals(CustomData.of(requiredNbt), matchingStacks[0].get(DataComponents.CUSTOM_DATA));

		context.succeed();
	}

	private static <T> void assertEquals(T expected, T actual) {
		if (!Objects.equals(expected, actual)) {
			throw new GameTestAssertException(String.format("assertEquals failed%nexpected: %s%n but was: %s", expected, actual));
		}
	}
}
