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

import org.jetbrains.annotations.Nullable;
import net.fabricmc.fabric.api.blockview.v2.RenderDataBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class FrameBlockEntity extends BlockEntity implements RenderDataBlockEntity {
	@Nullable
	private Block block = null;

	public FrameBlockEntity(BlockPos blockPos, BlockState blockState) {
		super(Registration.FRAME_BLOCK_ENTITY_TYPE, blockPos, blockState);
	}

	@Override
	public void loadAdditional(CompoundTag tag, HolderLookup.Provider wrapperLookup) {
		super.loadAdditional(tag, wrapperLookup);

		if (tag.contains("block", Tag.TAG_STRING)) {
			this.block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(tag.getString("block")));
		} else {
			this.block = null;
		}

		if (this.getLevel() != null && this.getLevel().isClientSide()) {
			// This call forces a chunk remesh.
			level.sendBlockUpdated(worldPosition, null, null, 0);
		}
	}

	@Override
	public void saveAdditional(CompoundTag tag, HolderLookup.Provider wrapperLookup) {
		super.saveAdditional(tag, wrapperLookup);

		if (this.block != null) {
			tag.putString("block", BuiltInRegistries.BLOCK.getKey(this.block).toString());
		} else {
			// Always need something in the tag, otherwise S2C syncing will never apply the packet.
			tag.putInt("block", -1);
		}
	}

	@Override
	public void setChanged() {
		super.setChanged();

		if (this.hasLevel() && !this.getLevel().isClientSide()) {
			((ServerLevel) level).getChunkSource().blockChanged(getBlockPos());
		}
	}

	@Nullable
	public Block getBlock() {
		return this.block;
	}

	public void setBlock(@Nullable Block block) {
		this.block = block;
		this.setChanged();
	}

	@Nullable
	@Override
	public Block getRenderData() {
		return this.block;
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider wrapperLookup) {
		return this.saveCustomOnly(wrapperLookup);
	}
}
