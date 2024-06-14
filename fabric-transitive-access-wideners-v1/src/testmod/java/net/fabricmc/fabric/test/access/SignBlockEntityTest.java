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

package net.fabricmc.fabric.test.access;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;

public final class SignBlockEntityTest implements ModInitializer {
	public static final String MOD_ID = "fabric-transitive-access-wideners-v1-testmod";
	public static final StandingSignBlock TEST_SIGN = new StandingSignBlock(WoodType.OAK, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SIGN)) {
		@Override
		public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
			return new TestSign(pos, state);
		}
	};
	public static final WallSignBlock TEST_WALL_SIGN = new WallSignBlock(WoodType.OAK, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SIGN)) {
		@Override
		public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
			return new TestSign(pos, state);
		}
	};
	public static final SignItem TEST_SIGN_ITEM = new SignItem(new Item.Properties(), TEST_SIGN, TEST_WALL_SIGN);
	public static final BlockEntityType<TestSign> TEST_SIGN_BLOCK_ENTITY = FabricBlockEntityTypeBuilder.create(TestSign::new, TEST_SIGN, TEST_WALL_SIGN).build();

	@Override
	public void onInitialize() {
		Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(MOD_ID, "test_sign"), TEST_SIGN);
		Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(MOD_ID, "test_wall_sign"), TEST_WALL_SIGN);
		Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MOD_ID, "test_sign"), TEST_SIGN_ITEM);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(MOD_ID, "test_sign"), TEST_SIGN_BLOCK_ENTITY);
	}

	public static class TestSign extends SignBlockEntity {
		public TestSign(BlockPos pos, BlockState state) {
			super(pos, state);
		}

		@Override
		public BlockEntityType<?> getType() {
			return TEST_SIGN_BLOCK_ENTITY;
		}
	}
}
