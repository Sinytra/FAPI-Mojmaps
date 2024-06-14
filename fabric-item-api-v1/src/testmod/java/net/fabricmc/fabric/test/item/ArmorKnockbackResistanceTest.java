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

package net.fabricmc.fabric.test.item;

import java.util.EnumMap;
import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

public class ArmorKnockbackResistanceTest implements ModInitializer {
	private static final Holder<ArmorMaterial> WOOD_ARMOR = Registry.registerForHolder(BuiltInRegistries.ARMOR_MATERIAL, ResourceLocation.fromNamespaceAndPath("fabric-item-api-v1-testmod", "wood"), createTestArmorMaterial());

	@Override
	public void onInitialize() {
		Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath("fabric-item-api-v1-testmod",
				"wooden_boots"), new ArmorItem(WOOD_ARMOR, ArmorItem.Type.BOOTS, new Item.Properties()));
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
			List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath("fabric-item-api-v1-testmod", "wood"))),
			0,
			0.5F
		);
	}
}
