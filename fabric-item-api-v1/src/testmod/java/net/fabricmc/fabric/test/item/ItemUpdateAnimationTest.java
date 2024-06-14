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

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

public class ItemUpdateAnimationTest implements ModInitializer {
	public static final DataComponentType<Integer> TICKS = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, ResourceLocation.fromNamespaceAndPath("fabric-item-api-v1-testmod", "ticks"),
																			DataComponentType.<Integer>builder().persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());

	@Override
	public void onInitialize() {
		Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath("fabric-item-api-v1-testmod", "updating_allowed"), new UpdatingItem(true));
		Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath("fabric-item-api-v1-testmod", "updating_disallowed"), new UpdatingItem(false));
	}
}
