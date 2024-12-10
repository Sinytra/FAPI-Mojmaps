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

package net.fabricmc.fabric.impl.registry.sync;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public class SyncCompletePayload implements CustomPacketPayload {
	public static final SyncCompletePayload INSTANCE = new SyncCompletePayload();
	public static final CustomPacketPayload.Type<SyncCompletePayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("fabric", "registry/sync/complete"));
	public static final StreamCodec<FriendlyByteBuf, SyncCompletePayload> CODEC = StreamCodec.unit(INSTANCE);

	private SyncCompletePayload() { }

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
