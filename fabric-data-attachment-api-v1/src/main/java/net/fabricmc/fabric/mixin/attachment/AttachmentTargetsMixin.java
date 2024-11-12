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

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.impl.attachment.AttachmentSerializingImpl;
import net.fabricmc.fabric.impl.attachment.AttachmentTargetImpl;
import net.fabricmc.fabric.impl.attachment.AttachmentTypeImpl;
import net.fabricmc.fabric.impl.attachment.sync.AttachmentChange;
import net.fabricmc.fabric.impl.attachment.sync.s2c.AttachmentSyncPayloadS2C;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;

@Mixin({BlockEntity.class, Entity.class, Level.class, ChunkAccess.class})
abstract class AttachmentTargetsMixin implements AttachmentTargetImpl {
	@Nullable
	private IdentityHashMap<AttachmentType<?>, Object> fabric_dataAttachments = null;
	@Nullable
	private IdentityHashMap<AttachmentType<?>, AttachmentChange> fabric_syncedAttachments = null;

	@SuppressWarnings("unchecked")
	@Override
	@Nullable
	public <T> T getAttached(AttachmentType<T> type) {
		return fabric_dataAttachments == null ? null : (T) fabric_dataAttachments.get(type);
	}

	@SuppressWarnings("unchecked")
	@Override
	@Nullable
	public <T> T setAttached(AttachmentType<T> type, @Nullable T value) {
		this.fabric_markChanged(type);

		if (this.fabric_shouldTryToSync() && type.isSynced()) {
			AttachmentChange change = AttachmentChange.create(fabric_getSyncTargetInfo(), type, value);
			acknowledgeSyncedEntry(type, change);
			this.fabric_syncChange(type, new AttachmentSyncPayloadS2C(List.of(change)));
		}

		if (value == null) {
			if (fabric_dataAttachments == null) {
				return null;
			}

			return (T) fabric_dataAttachments.remove(type);
		} else {
			if (fabric_dataAttachments == null) {
				fabric_dataAttachments = new IdentityHashMap<>();
			}

			return (T) fabric_dataAttachments.put(type, value);
		}
	}

	@Override
	public boolean hasAttached(AttachmentType<?> type) {
		return fabric_dataAttachments != null && fabric_dataAttachments.containsKey(type);
	}

	@Override
	public void fabric_writeAttachmentsToNbt(CompoundTag nbt, HolderLookup.Provider wrapperLookup) {
		AttachmentSerializingImpl.serializeAttachmentData(nbt, wrapperLookup, fabric_dataAttachments);
	}

	@Override
	public void fabric_readAttachmentsFromNbt(CompoundTag nbt, HolderLookup.Provider wrapperLookup) {
		// Note on player targets: no syncing can happen here as the networkHandler is still null
		// Instead it is done on player join (see AttachmentSync)
		this.fabric_dataAttachments = AttachmentSerializingImpl.deserializeAttachmentData(nbt, wrapperLookup);

		if (this.fabric_shouldTryToSync() && this.fabric_dataAttachments != null) {
			this.fabric_dataAttachments.forEach((type, value) -> {
				if (type.isSynced()) {
					acknowledgeSynced(type, value);
				}
			});
		}
	}

	@Override
	public boolean fabric_hasPersistentAttachments() {
		return AttachmentSerializingImpl.hasPersistentAttachments(fabric_dataAttachments);
	}

	@Override
	public Map<AttachmentType<?>, ?> fabric_getAttachments() {
		return fabric_dataAttachments;
	}

	@Unique
	private void acknowledgeSynced(AttachmentType<?> type, Object value) {
		acknowledgeSyncedEntry(type, AttachmentChange.create(fabric_getSyncTargetInfo(), type, value));
	}

	@Unique
	private void acknowledgeSyncedEntry(AttachmentType<?> type, @Nullable AttachmentChange change) {
		if (change == null) {
			if (fabric_syncedAttachments == null) {
				return;
			}

			fabric_syncedAttachments.remove(type);
		} else {
			if (fabric_syncedAttachments == null) {
				fabric_syncedAttachments = new IdentityHashMap<>();
			}

			fabric_syncedAttachments.put(type, change);
		}
	}

	@Override
	public void fabric_computeInitialSyncChanges(ServerPlayer player, Consumer<AttachmentChange> changeOutput) {
		if (fabric_syncedAttachments == null) {
			return;
		}

		for (Map.Entry<AttachmentType<?>, AttachmentChange> entry : fabric_syncedAttachments.entrySet()) {
			if (((AttachmentTypeImpl<?>) entry.getKey()).syncPredicate().test(this, player)) {
				changeOutput.accept(entry.getValue());
			}
		}
	}
}
