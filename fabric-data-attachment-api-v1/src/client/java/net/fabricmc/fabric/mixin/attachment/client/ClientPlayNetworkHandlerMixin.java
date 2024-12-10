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

package net.fabricmc.fabric.mixin.attachment.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import net.fabricmc.fabric.impl.attachment.AttachmentTargetImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;

@Mixin(ClientPacketListener.class)
abstract class ClientPlayNetworkHandlerMixin {
	@WrapOperation(
			method = "handleRespawn",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;resetPos()V")
	)
	private void copyAttachmentsOnClientRespawn(LocalPlayer newPlayer, Operation<Void> init, ClientboundRespawnPacket packet, @Local(ordinal = 0) LocalPlayer oldPlayer) {
		/*
		 * The KEEP_ATTRIBUTES flag is not set on a death respawn, and set in all other cases
		 */
		AttachmentTargetImpl.transfer(oldPlayer, newPlayer, !packet.shouldKeep(ClientboundRespawnPacket.KEEP_ATTRIBUTE_MODIFIERS));
		init.call(newPlayer);
	}
}
