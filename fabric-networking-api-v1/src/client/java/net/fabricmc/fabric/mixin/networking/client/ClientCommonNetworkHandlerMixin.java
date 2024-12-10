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

package net.fabricmc.fabric.mixin.networking.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.fabricmc.fabric.impl.networking.NetworkHandlerExtensions;
import net.fabricmc.fabric.impl.networking.client.ClientConfigurationNetworkAddon;
import net.fabricmc.fabric.impl.networking.client.ClientPlayNetworkAddon;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ClientCommonNetworkHandlerMixin implements NetworkHandlerExtensions {
	@Inject(method = "handleCustomPayload(Lnet/minecraft/network/protocol/common/ClientboundCustomPayloadPacket;)V", at = @At("HEAD"), cancellable = true)
	public void onCustomPayload(ClientboundCustomPayloadPacket packet, CallbackInfo ci) {
		final CustomPacketPayload payload = packet.payload();
		boolean handled;

		if (this.getAddon() instanceof ClientPlayNetworkAddon addon) {
			handled = addon.handle(payload);
		} else if (this.getAddon() instanceof ClientConfigurationNetworkAddon addon) {
			handled = addon.handle(payload);
		} else {
			throw new IllegalStateException("Unknown network addon");
		}

		if (handled) {
			ci.cancel();
		}
	}
}
