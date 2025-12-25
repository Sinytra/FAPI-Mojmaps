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

package net.fabricmc.fabric.test.screenhandler;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fabricmc.fabric.test.screenhandler.block.BoxBlock;
import net.fabricmc.fabric.test.screenhandler.block.BoxBlockEntity;
import net.fabricmc.fabric.test.screenhandler.item.BagItem;
import net.fabricmc.fabric.test.screenhandler.item.PositionedBagItem;
import net.fabricmc.fabric.test.screenhandler.screen.BagScreenHandler;
import net.fabricmc.fabric.test.screenhandler.screen.BoxScreenHandler;
import net.fabricmc.fabric.test.screenhandler.screen.PositionedBagScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ScreenHandlerTest implements ModInitializer {
	public static final String ID = "fabric-screen-handler-api-v1-testmod";

	public static final Item BAG = new BagItem(new Item.Properties().stacksTo(1));
	public static final Item POSITIONED_BAG = new PositionedBagItem(new Item.Properties().stacksTo(1));
	public static final Block BOX = new BoxBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WOOD));
	public static final Item BOX_ITEM = new BlockItem(BOX, new Item.Properties());
	public static final BlockEntityType<BoxBlockEntity> BOX_ENTITY = FabricBlockEntityTypeBuilder.create(BoxBlockEntity::new, BOX).build();
	public static final MenuType<BagScreenHandler> BAG_SCREEN_HANDLER = new MenuType<>(BagScreenHandler::new, FeatureFlags.VANILLA_SET);
	public static final MenuType<PositionedBagScreenHandler> POSITIONED_BAG_SCREEN_HANDLER = new ExtendedScreenHandlerType<>(PositionedBagScreenHandler::new, PositionedBagScreenHandler.BagData.PACKET_CODEC);
	public static final MenuType<BoxScreenHandler> BOX_SCREEN_HANDLER = new ExtendedScreenHandlerType<>(BoxScreenHandler::new, BlockPos.STREAM_CODEC.cast());

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(ID, path);
	}

	@Override
	public void onInitialize() {
		Registry.register(BuiltInRegistries.ITEM, id("bag"), BAG);
		Registry.register(BuiltInRegistries.ITEM, id("positioned_bag"), POSITIONED_BAG);
		Registry.register(BuiltInRegistries.BLOCK, id("box"), BOX);
		Registry.register(BuiltInRegistries.ITEM, id("box"), BOX_ITEM);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("box"), BOX_ENTITY);
		Registry.register(BuiltInRegistries.MENU, id("bag"), BAG_SCREEN_HANDLER);
		Registry.register(BuiltInRegistries.MENU, id("positioned_bag"), POSITIONED_BAG_SCREEN_HANDLER);
		Registry.register(BuiltInRegistries.MENU, id("box"), BOX_SCREEN_HANDLER);
	}
}
