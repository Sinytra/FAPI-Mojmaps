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

import java.util.function.Function;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class Registration {
	public static final FrameBlock FRAME_BLOCK = register("frame", FrameBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
	public static final FrameBlock FRAME_MULTIPART_BLOCK = register("frame_multipart", FrameBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
	public static final FrameBlock FRAME_VARIANT_BLOCK = register("frame_variant", FrameBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
	public static final Block PILLAR_BLOCK = register("pillar", Block::new, BlockBehaviour.Properties.of());
	public static final Block OCTAGONAL_COLUMN_BLOCK = register("octagonal_column", OctagonalColumnBlock::new, BlockBehaviour.Properties.of().noOcclusion().strength(1.8F));
	public static final Block RIVERSTONE_BLOCK = register("riverstone", Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.STONE));

	public static final FrameBlock[] FRAME_BLOCKS = new FrameBlock[] {
			FRAME_BLOCK,
			FRAME_MULTIPART_BLOCK,
			FRAME_VARIANT_BLOCK,
	};

	public static final Item FRAME_ITEM = registerItem("frame", (settings) -> new BlockItem(FRAME_BLOCK, settings));
	public static final Item FRAME_MULTIPART_ITEM = registerItem("frame_multipart", (settings) -> new BlockItem(FRAME_MULTIPART_BLOCK, settings));
	public static final Item FRAME_VARIANT_ITEM = registerItem("frame_variant", (settings) -> new BlockItem(FRAME_VARIANT_BLOCK, settings));
	public static final Item PILLAR_ITEM = registerItem("pillar", (settings) -> new BlockItem(PILLAR_BLOCK, settings));
	public static final Item OCTAGONAL_COLUMN_ITEM = registerItem("octagonal_column", (settings) -> new BlockItem(OCTAGONAL_COLUMN_BLOCK, settings));
	public static final Item RIVERSTONE_ITEM = registerItem("riverstone", (settings) -> new BlockItem(RIVERSTONE_BLOCK, settings));

	public static final BlockEntityType<FrameBlockEntity> FRAME_BLOCK_ENTITY_TYPE = register("frame", FabricBlockEntityTypeBuilder.create(FrameBlockEntity::new, FRAME_BLOCKS).build());

	// see also Blocks#register, which is functionally the same
	private static <T extends Block> T register(String path, Function<BlockBehaviour.Properties, T> constructor, BlockBehaviour.Properties settings) {
		ResourceLocation id = RendererTest.id(path);
		return Registry.register(BuiltInRegistries.BLOCK, id, constructor.apply(settings.setId(ResourceKey.create(Registries.BLOCK, id))));
	}

	private static <T extends Item> T registerItem(String path, Function<Item.Properties, T> itemFunction) {
		ResourceKey<Item> registryKey = ResourceKey.create(Registries.ITEM, RendererTest.id(path));
		return Registry.register(BuiltInRegistries.ITEM, registryKey, itemFunction.apply(new Item.Properties().setId(registryKey)));
	}

	private static <T extends BlockEntityType<?>> T register(String path, T blockEntityType) {
		return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, RendererTest.id(path), blockEntityType);
	}

	public static void init() {
	}
}
