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

package net.fabricmc.fabric.test.transfer.ingame;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class FluidChuteBlockEntity extends BlockEntity {
	final SingleFluidStorage storage = SingleFluidStorage.withFixedCapacity(FluidConstants.BUCKET * 4, this::setChanged);

	private int tickCounter = 0;

	public FluidChuteBlockEntity(BlockPos pos, BlockState state) {
		super(TransferTestInitializer.FLUID_CHUTE_TYPE, pos, state);
	}

	@SuppressWarnings("ConstantConditions")
	public void tick() {
		if (!level.isClientSide() && tickCounter++ % 20 == 0) {
			StorageUtil.move(
					FluidStorage.SIDED.find(level, worldPosition.relative(Direction.UP), Direction.DOWN),
					storage,
					fluid -> true,
					FluidConstants.BUCKET,
					null
			);
			StorageUtil.move(
					storage,
					FluidStorage.SIDED.find(level, worldPosition.relative(Direction.DOWN), Direction.UP),
					fluid -> true,
					FluidConstants.BUCKET,
					null
			);
		}
	}

	@Override
	protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider wrapperLookup) {
		super.saveAdditional(nbt, wrapperLookup);
		storage.writeNbt(nbt, wrapperLookup);
	}

	@Override
	public void loadAdditional(CompoundTag nbt, HolderLookup.Provider wrapperLookup) {
		super.loadAdditional(nbt, wrapperLookup);
		storage.readNbt(nbt, wrapperLookup);
	}
}
