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

package net.fabricmc.fabric.mixin.recipe.ingredient;

import java.util.Set;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.fabricmc.fabric.impl.recipe.ingredient.CustomIngredientSync;
import net.fabricmc.fabric.impl.recipe.ingredient.SupportedIngredientsPacketEncoder;
import net.minecraft.network.PacketEncoder;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;

@Mixin(PacketEncoder.class)
public class EncoderHandlerMixin implements SupportedIngredientsPacketEncoder {
	@Unique
	private Set<ResourceLocation> fabric_supportedCustomIngredients = Set.of();

	@Override
	public void fabric_setSupportedCustomIngredients(Set<ResourceLocation> supportedCustomIngredients) {
		fabric_supportedCustomIngredients = supportedCustomIngredients;
	}

	@Inject(
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/network/codec/StreamCodec;encode(Ljava/lang/Object;Ljava/lang/Object;)V"
			),
			method = "encode(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;Lio/netty/buffer/ByteBuf;)V"
	)
	private void capturePacketEncoder(ChannelHandlerContext channelHandlerContext, Packet<?> packet, ByteBuf byteBuf, CallbackInfo ci) {
		CustomIngredientSync.CURRENT_SUPPORTED_INGREDIENTS.set(fabric_supportedCustomIngredients);
	}

	@Inject(
			at = {
					// Normal target after writing
					@At(
							value = "INVOKE",
							target = "Lnet/minecraft/network/codec/StreamCodec;encode(Ljava/lang/Object;Ljava/lang/Object;)V",
							shift = At.Shift.AFTER,
							by = 1
					),
					// In the catch handler in case some exception was thrown
					@At(
							value = "INVOKE",
							target = "Lnet/minecraft/network/protocol/Packet;isSkippable()Z"
					)
			},
			method = "encode(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;Lio/netty/buffer/ByteBuf;)V"
	)
	private void releasePacketEncoder(ChannelHandlerContext channelHandlerContext, Packet<?> packet, ByteBuf byteBuf, CallbackInfo ci) {
		CustomIngredientSync.CURRENT_SUPPORTED_INGREDIENTS.set(null);
	}
}
