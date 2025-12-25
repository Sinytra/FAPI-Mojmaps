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

package net.fabricmc.fabric.test.object.builder;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

public class BlockEntityTypeBuilderTest implements ModInitializer {
	private static final ResourceLocation INITIAL_BETRAYAL_BLOCK_ID = ObjectBuilderTestConstants.id("initial_betrayal_block");
	static final Block INITIAL_BETRAYAL_BLOCK = new BetrayalBlock(MapColor.COLOR_BLUE);

	private static final ResourceLocation ADDED_BETRAYAL_BLOCK_ID = ObjectBuilderTestConstants.id("added_betrayal_block");
	static final Block ADDED_BETRAYAL_BLOCK = new BetrayalBlock(MapColor.COLOR_GREEN);

	private static final ResourceLocation FIRST_MULTI_BETRAYAL_BLOCK_ID = ObjectBuilderTestConstants.id("first_multi_betrayal_block");
	static final Block FIRST_MULTI_BETRAYAL_BLOCK = new BetrayalBlock(MapColor.COLOR_RED);

	private static final ResourceLocation SECOND_MULTI_BETRAYAL_BLOCK_ID = ObjectBuilderTestConstants.id("second_multi_betrayal_block");
	static final Block SECOND_MULTI_BETRAYAL_BLOCK = new BetrayalBlock(MapColor.COLOR_YELLOW);

	private static final ResourceLocation BLOCK_ENTITY_TYPE_ID = ObjectBuilderTestConstants.id("betrayal_block");
	public static final BlockEntityType<?> BLOCK_ENTITY_TYPE = FabricBlockEntityTypeBuilder.create(BetrayalBlockEntity::new, INITIAL_BETRAYAL_BLOCK)
			.addBlock(ADDED_BETRAYAL_BLOCK)
			.addBlocks(FIRST_MULTI_BETRAYAL_BLOCK, SECOND_MULTI_BETRAYAL_BLOCK)
			.build();

	@Override
	public void onInitialize() {
		register(INITIAL_BETRAYAL_BLOCK_ID, INITIAL_BETRAYAL_BLOCK);
		register(ADDED_BETRAYAL_BLOCK_ID, ADDED_BETRAYAL_BLOCK);
		register(FIRST_MULTI_BETRAYAL_BLOCK_ID, FIRST_MULTI_BETRAYAL_BLOCK);
		register(SECOND_MULTI_BETRAYAL_BLOCK_ID, SECOND_MULTI_BETRAYAL_BLOCK);

		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, BLOCK_ENTITY_TYPE_ID, BLOCK_ENTITY_TYPE);
	}

	private static void register(ResourceLocation id, Block block) {
		Registry.register(BuiltInRegistries.BLOCK, id, block);

		Item item = new BlockItem(block, new Item.Properties());
		Registry.register(BuiltInRegistries.ITEM, id, item);
	}

	private static class BetrayalBlock extends Block implements EntityBlock {
		private BetrayalBlock(MapColor color) {
			super(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).mapColor(color));
		}

		@Override
		public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
			if (!world.isClientSide()) {
				BlockEntity blockEntity = world.getBlockEntity(pos);

				if (blockEntity == null) {
					throw new AssertionError("Missing block entity for betrayal block at " + pos);
				} else if (!BLOCK_ENTITY_TYPE.equals(blockEntity.getType())) {
					ResourceLocation id = BlockEntityType.getKey(blockEntity.getType());
					throw new AssertionError("Incorrect block entity for betrayal block at " + pos + ": " + id);
				}

				Component posText = Component.translatable("chat.coordinates", pos.getX(), pos.getY(), pos.getZ());
				Component message = Component.translatableEscape("text.fabric-object-builder-api-v1-testmod.block_entity_type_success", posText, BLOCK_ENTITY_TYPE_ID);

				player.displayClientMessage(message, false);
			}

			return InteractionResult.SUCCESS;
		}

		@Override
		public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
			return new BetrayalBlockEntity(pos, state);
		}
	}

	private static class BetrayalBlockEntity extends BlockEntity {
		private BetrayalBlockEntity(BlockPos pos, BlockState state) {
			super(BLOCK_ENTITY_TYPE, pos, state);
		}
	}
}
