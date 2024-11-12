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

package net.fabricmc.fabric.mixin.attachment;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.impl.attachment.AttachmentTargetImpl;
import net.fabricmc.fabric.impl.attachment.AttachmentTypeImpl;
import net.fabricmc.fabric.impl.attachment.sync.AttachmentSync;
import net.fabricmc.fabric.impl.attachment.sync.AttachmentTargetInfo;
import net.fabricmc.fabric.impl.attachment.sync.s2c.AttachmentSyncPayloadS2C;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

@Mixin(BlockEntity.class)
abstract class BlockEntityMixin implements AttachmentTargetImpl {
	@Shadow
	@Final
	protected BlockPos worldPosition;
	@Shadow
	@Nullable
	protected Level level;

	@Shadow
	public abstract void setChanged();

	@Shadow
	public abstract boolean hasLevel();

	@Inject(
			method = "loadWithComponents",
			at = @At("RETURN")
	)
	private void readBlockEntityAttachments(CompoundTag nbt, HolderLookup.Provider registryLookup, CallbackInfo ci) {
		this.fabric_readAttachmentsFromNbt(nbt, registryLookup);
	}

	@Inject(
			method = "saveWithoutMetadata",
			at = @At("RETURN")
	)
	private void writeBlockEntityAttachments(HolderLookup.Provider wrapperLookup, CallbackInfoReturnable<CompoundTag> cir) {
		this.fabric_writeAttachmentsToNbt(cir.getReturnValue(), wrapperLookup);
	}

	@Override
	public void fabric_markChanged(AttachmentType<?> type) {
		this.setChanged();
	}

	@Override
	public AttachmentTargetInfo<?> fabric_getSyncTargetInfo() {
		return new AttachmentTargetInfo.BlockEntityTarget(this.worldPosition);
	}

	@Override
	public void fabric_syncChange(AttachmentType<?> type, AttachmentSyncPayloadS2C payload) {
		PlayerLookup.tracking((BlockEntity) (Object) this)
				.forEach(player -> {
					if (((AttachmentTypeImpl<?>) type).syncPredicate().test(this, player)) {
						AttachmentSync.trySync(payload, player);
					}
				});
	}

	@Override
	public boolean fabric_shouldTryToSync() {
		// Persistent attachments are read at a time with no world
		return !this.hasLevel() || !this.level.isClientSide();
	}
}
