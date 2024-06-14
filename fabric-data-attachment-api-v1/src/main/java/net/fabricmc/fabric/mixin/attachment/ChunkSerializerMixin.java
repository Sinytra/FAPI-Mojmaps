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

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.fabricmc.fabric.impl.attachment.AttachmentTargetImpl;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;

@Mixin(ChunkSerializer.class)
abstract class ChunkSerializerMixin {
	@ModifyExpressionValue(
			at = @At(
					value = "NEW",
					target = "net/minecraft/world/level/chunk/LevelChunk"
			),
			method = "read"
	)
	private static LevelChunk readWorldChunkAttachments(LevelChunk chunk, ServerLevel world, PoiManager poiStorage, RegionStorageInfo storageKey, ChunkPos chunkPos, CompoundTag nbt) {
		((AttachmentTargetImpl) chunk).fabric_readAttachmentsFromNbt(nbt, world.registryAccess());
		return chunk;
	}

	@ModifyExpressionValue(
			at = @At(
					value = "NEW",
					target = "net/minecraft/world/level/chunk/ProtoChunk"
			),
			method = "read"
	)
	private static ProtoChunk readProtoChunkAttachments(ProtoChunk chunk, ServerLevel world, PoiManager poiStorage, RegionStorageInfo storageKey, ChunkPos chunkPos, CompoundTag nbt) {
		((AttachmentTargetImpl) chunk).fabric_readAttachmentsFromNbt(nbt, world.registryAccess());
		return chunk;
	}

	@Inject(
			at = @At("RETURN"),
			method = "write"
	)
	private static void writeChunkAttachments(ServerLevel world, ChunkAccess chunk, CallbackInfoReturnable<CompoundTag> cir) {
		((AttachmentTargetImpl) chunk).fabric_writeAttachmentsToNbt(cir.getReturnValue(), world.registryAccess());
	}
}
