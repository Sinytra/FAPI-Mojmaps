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

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.impl.attachment.AttachmentTargetImpl;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;

@Mixin(SerializableChunkData.class)
abstract class SerializedChunkMixin {
	// Adding a mutable record field like this is likely a bad idea, but I cannot see a better way.
	@Unique
	@Nullable
	private CompoundTag attachmentNbtData;

	@Inject(method = "parse", at = @At("RETURN"))
	private static void storeAttachmentNbtData(LevelHeightAccessor heightLimitView, RegistryAccess dynamicRegistryManager, CompoundTag nbt, CallbackInfoReturnable<SerializableChunkData> cir, @Share("attachmentDataNbt") LocalRef<CompoundTag> attachmentDataNbt) {
		final SerializableChunkData serializer = cir.getReturnValue();

		if (serializer == null) {
			return;
		}

		if (nbt.contains(AttachmentTarget.NBT_ATTACHMENT_KEY, Tag.TAG_COMPOUND)) {
			((SerializedChunkMixin) (Object) serializer).attachmentNbtData = nbt.getCompound(AttachmentTarget.NBT_ATTACHMENT_KEY);
		}
	}

	@Inject(method = "read", at = @At("RETURN"))
	private void setAttachmentDataInChunk(ServerLevel serverWorld, PoiManager pointOfInterestStorage, RegionStorageInfo storageKey, ChunkPos chunkPos, CallbackInfoReturnable<ProtoChunk> cir) {
		ProtoChunk chunk = cir.getReturnValue();

		if (chunk != null && attachmentNbtData != null) {
			var nbt = new CompoundTag();
			nbt.put(AttachmentTarget.NBT_ATTACHMENT_KEY, attachmentNbtData);
			((AttachmentTargetImpl) chunk).fabric_readAttachmentsFromNbt(nbt, serverWorld.registryAccess());
		}
	}

	@Inject(method = "copyOf", at = @At("RETURN"))
	private static void storeAttachmentNbtData(ServerLevel world, ChunkAccess chunk, CallbackInfoReturnable<SerializableChunkData> cir) {
		var nbt = new CompoundTag();
		((AttachmentTargetImpl) chunk).fabric_writeAttachmentsToNbt(nbt, world.registryAccess());
		((SerializedChunkMixin) (Object) cir.getReturnValue()).attachmentNbtData = nbt.getCompound(AttachmentTarget.NBT_ATTACHMENT_KEY);
	}

	@Inject(method = "write", at = @At("RETURN"))
	private void writeChunkAttachments(CallbackInfoReturnable<CompoundTag> cir) {
		if (attachmentNbtData != null) {
			cir.getReturnValue().put(AttachmentTarget.NBT_ATTACHMENT_KEY, attachmentNbtData);
		}
	}
}
