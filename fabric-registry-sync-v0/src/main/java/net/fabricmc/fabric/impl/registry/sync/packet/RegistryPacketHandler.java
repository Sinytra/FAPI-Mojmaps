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

package net.fabricmc.fabric.impl.registry.sync.packet;

import java.util.Map;
import java.util.function.Consumer;
import java.util.zip.Deflater;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.jetbrains.annotations.Nullable;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.impl.registry.sync.RegistrySyncManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public abstract class RegistryPacketHandler<T extends RegistryPacketHandler.RegistrySyncPayload> {
	private int rawBufSize = 0;
	private int deflatedBufSize = 0;

	public abstract CustomPacketPayload.Type<T> getPacketId();

	public abstract void sendPacket(Consumer<T> sender, Map<ResourceLocation, Object2IntMap<ResourceLocation>> registryMap);

	public abstract void receivePayload(T payload);

	public abstract int getTotalPacketReceived();

	public abstract boolean isPacketFinished();

	@Nullable
	public abstract Map<ResourceLocation, Object2IntMap<ResourceLocation>> getSyncedRegistryMap();

	protected final void computeBufSize(FriendlyByteBuf buf) {
		if (!RegistrySyncManager.DEBUG) {
			return;
		}

		final byte[] deflateBuffer = new byte[8192];
		ByteBuf byteBuf = buf.copy();
		Deflater deflater = new Deflater();

		int i = byteBuf.readableBytes();
		FriendlyByteBuf deflatedBuf = PacketByteBufs.create();

		if (i < 256) {
			deflatedBuf.writeVarInt(0);
			deflatedBuf.writeBytes(byteBuf);
		} else {
			byte[] bs = new byte[i];
			byteBuf.readBytes(bs);
			deflatedBuf.writeVarInt(bs.length);
			deflater.setInput(bs, 0, i);
			deflater.finish();

			while (!deflater.finished()) {
				int j = deflater.deflate(deflateBuffer);
				deflatedBuf.writeBytes(deflateBuffer, 0, j);
			}

			deflater.reset();
		}

		rawBufSize = buf.readableBytes();
		deflatedBufSize = deflatedBuf.readableBytes();
	}

	public final int getRawBufSize() {
		return rawBufSize;
	}

	public final int getDeflatedBufSize() {
		return deflatedBufSize;
	}

	public interface RegistrySyncPayload extends CustomPacketPayload {
	}
}
