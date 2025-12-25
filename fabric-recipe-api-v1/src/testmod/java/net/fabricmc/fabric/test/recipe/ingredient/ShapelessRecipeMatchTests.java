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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapelessRecipe;

public class ShapelessRecipeMatchTests {
	/**
	 * The recipe requires at least one undamaged pickaxe.
	 */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void testShapelessMatch(GameTestHelper context) {
		ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath("fabric-recipe-api-v1-testmod", "test_shapeless_match");
		ShapelessRecipe recipe = (ShapelessRecipe) context.getLevel().getRecipeManager().byKey(recipeId).get().value();

		ItemStack undamagedPickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
		ItemStack damagedPickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
		damagedPickaxe.setDamageValue(100);

		List<ItemStack> damagedPickaxes = Collections.nCopies(9, damagedPickaxe);

		if (recipe.matches(CraftingInput.of(3, 3, damagedPickaxes), context.getLevel())) {
			throw new GameTestAssertException("Recipe should not match with only damaged pickaxes");
		}

		List<ItemStack> oneUndamagedPickaxe = new LinkedList<>(damagedPickaxes);
		oneUndamagedPickaxe.set(0, undamagedPickaxe);

		if (!recipe.matches(CraftingInput.of(3, 3, oneUndamagedPickaxe), context.getLevel())) {
			throw new GameTestAssertException("Recipe should match with at least one undamaged pickaxe");
		}

		context.succeed();
	}
}
