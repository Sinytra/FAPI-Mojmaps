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

package net.fabricmc.fabric.test.rendering;

import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import net.fabricmc.api.ModInitializer;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

public class TooltipComponentTestInit implements ModInitializer {
	public static Item CUSTOM_TOOLTIP_ITEM = new CustomTooltipItem();
	public static Holder<ArmorMaterial> TEST_ARMOR_MATERIAL = Registry.registerForHolder(BuiltInRegistries.ARMOR_MATERIAL, ResourceLocation.fromNamespaceAndPath("fabric-rendering-v1-testmod", "test_material"), createTestArmorMaterial());
	public static Item CUSTOM_ARMOR_ITEM = new ArmorItem(TEST_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Properties());

	@Override
	public void onInitialize() {
		Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath("fabric-rendering-v1-testmod", "custom_tooltip"), CUSTOM_TOOLTIP_ITEM);
		Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath("fabric-rendering-v1-testmod", "test_chest"), CUSTOM_ARMOR_ITEM);
	}

	private static class CustomTooltipItem extends Item {
		CustomTooltipItem() {
			super(new Properties());
		}

		@Override
		public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
			return Optional.of(new Data(stack.getDescriptionId()));
		}
	}

	public record Data(String string) implements TooltipComponent {
	}

	private static ArmorMaterial createTestArmorMaterial() {
		return new ArmorMaterial(Util.make(new EnumMap<>(ArmorItem.Type.class), (map) -> {
			map.put(ArmorItem.Type.BOOTS, 1);
			map.put(ArmorItem.Type.LEGGINGS, 2);
			map.put(ArmorItem.Type.CHESTPLATE, 3);
			map.put(ArmorItem.Type.HELMET, 1);
			map.put(ArmorItem.Type.BODY, 3);
		}),
			0,
			SoundEvents.ITEM_ARMOR_EQUIP_LEATHER,
				() -> Ingredient.of(Items.LEATHER),
			List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath("fabric-rendering-v1-testmod", "test_material"))),
			0,
			0
		);
	}
}
