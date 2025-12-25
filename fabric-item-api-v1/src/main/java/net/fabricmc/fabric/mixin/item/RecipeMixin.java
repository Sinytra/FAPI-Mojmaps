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

package net.fabricmc.fabric.mixin.item;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import net.fabricmc.fabric.impl.item.RecipeRemainderHandler;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;

@Mixin(Recipe.class)
public interface RecipeMixin<T extends RecipeInput> {
	@Inject(method = "getRemainingItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/crafting/RecipeInput;getItem(I)Lnet/minecraft/world/item/ItemStack;"), locals = LocalCapture.CAPTURE_FAILHARD)
	default void captureStack(T inventory, CallbackInfoReturnable<NonNullList<ItemStack>> cir, NonNullList<ItemStack> defaultedList, int i) {
		RecipeRemainderHandler.REMAINDER_STACK.set(inventory.getItem(i).getRecipeRemainder());
	}

	@Redirect(method = "getRemainingItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/Item;hasCraftingRemainingItem()Z"))
	private boolean hasStackRemainder(Item instance) {
		return !RecipeRemainderHandler.REMAINDER_STACK.get().isEmpty();
	}

	@Redirect(method = "getRemainingItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/Item;getCraftingRemainingItem()Lnet/minecraft/world/item/Item;"))
	private Item replaceGetRecipeRemainder(Item instance) {
		return Items.AIR;
	}

	@Redirect(method = "getRemainingItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/NonNullList;set(ILjava/lang/Object;)Ljava/lang/Object;"))
	private Object getStackRemainder(NonNullList<ItemStack> inventory, int index, Object element) {
		Object remainder = inventory.set(index, RecipeRemainderHandler.REMAINDER_STACK.get());
		RecipeRemainderHandler.REMAINDER_STACK.remove();
		return remainder;
	}
}
