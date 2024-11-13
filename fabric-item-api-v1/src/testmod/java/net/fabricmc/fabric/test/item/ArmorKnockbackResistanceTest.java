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

import java.util.Map;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;

public class ArmorKnockbackResistanceTest implements ModInitializer {
	private static final ArmorMaterial WOOD_ARMOR = createTestArmorMaterial();

	@Override
	public void onInitialize() {
		ResourceKey<Item> registryKey = ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("fabric-item-api-v1-testmod", "wooden_boots"));
		Registry.register(BuiltInRegistries.ITEM, registryKey, new ArmorItem(WOOD_ARMOR, ArmorType.BOOTS, new Item.Properties().setId(registryKey)));
	}

	private static ArmorMaterial createTestArmorMaterial() {
		return new ArmorMaterial(
			0,
			Map.of(
				ArmorType.BOOTS, 1,
				ArmorType.LEGGINGS, 2,
				ArmorType.CHESTPLATE, 3,
				ArmorType.HELMET, 1,
				ArmorType.BODY, 3
			),
			1,
			SoundEvents.ARMOR_EQUIP_LEATHER,
			0,
			0.5F,
			ItemTags.REPAIRS_LEATHER_ARMOR,
			EquipmentAssets.IRON
		);
	}
}
