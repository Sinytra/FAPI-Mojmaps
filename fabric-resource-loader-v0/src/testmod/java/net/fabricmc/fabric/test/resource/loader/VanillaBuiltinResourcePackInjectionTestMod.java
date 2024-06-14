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

package net.fabricmc.fabric.test.resource.loader;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class VanillaBuiltinResourcePackInjectionTestMod implements ModInitializer {
	public static final String MODID = "fabric-resource-loader-v0-testmod";

	public static final Block TEST_BLOCK = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE));

	@Override
	public void onInitialize() {
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MODID, "testblock");

		Registry.register(BuiltInRegistries.BLOCK, id, TEST_BLOCK);
		Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(TEST_BLOCK, new Item.Properties()));
	}
}
