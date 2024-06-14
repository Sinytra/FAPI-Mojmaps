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

package net.fabricmc.fabric.test.renderer;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class Registration {
	public static final FrameBlock FRAME_BLOCK = register("frame", new FrameBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));
	public static final FrameBlock FRAME_MULTIPART_BLOCK = register("frame_multipart", new FrameBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));
	public static final FrameBlock FRAME_VARIANT_BLOCK = register("frame_variant", new FrameBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));
	public static final Block PILLAR_BLOCK = register("pillar", new Block(BlockBehaviour.Properties.of()));
	public static final Block OCTAGONAL_COLUMN_BLOCK = register("octagonal_column", new Block(BlockBehaviour.Properties.of().noOcclusion().strength(1.8F)));
	public static final Block RIVERSTONE_BLOCK = register("riverstone", new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)));

	public static final FrameBlock[] FRAME_BLOCKS = new FrameBlock[] {
			FRAME_BLOCK,
			FRAME_MULTIPART_BLOCK,
			FRAME_VARIANT_BLOCK,
	};

	public static final Item FRAME_ITEM = register("frame", new BlockItem(FRAME_BLOCK, new Item.Properties()));
	public static final Item FRAME_MULTIPART_ITEM = register("frame_multipart", new BlockItem(FRAME_MULTIPART_BLOCK, new Item.Properties()));
	public static final Item FRAME_VARIANT_ITEM = register("frame_variant", new BlockItem(FRAME_VARIANT_BLOCK, new Item.Properties()));
	public static final Item PILLAR_ITEM = register("pillar", new BlockItem(PILLAR_BLOCK, new Item.Properties()));
	public static final Item OCTAGONAL_COLUMN_ITEM = register("octagonal_column", new BlockItem(OCTAGONAL_COLUMN_BLOCK, new Item.Properties()));
	public static final Item RIVERSTONE_ITEM = register("riverstone", new BlockItem(RIVERSTONE_BLOCK, new Item.Properties()));

	public static final BlockEntityType<FrameBlockEntity> FRAME_BLOCK_ENTITY_TYPE = register("frame", FabricBlockEntityTypeBuilder.create(FrameBlockEntity::new, FRAME_BLOCKS).build(null));

	private static <T extends Block> T register(String path, T block) {
		return Registry.register(BuiltInRegistries.BLOCK, RendererTest.id(path), block);
	}

	private static <T extends Item> T register(String path, T item) {
		return Registry.register(BuiltInRegistries.ITEM, RendererTest.id(path), item);
	}

	private static <T extends BlockEntityType<?>> T register(String path, T blockEntityType) {
		return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, RendererTest.id(path), blockEntityType);
	}

	public static void init() {
	}
}
