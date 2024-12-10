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

package net.fabricmc.fabric.mixin.screenhandler;

import java.util.Objects;
import java.util.OptionalInt;

import com.mojang.authlib.GameProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fabricmc.fabric.impl.screenhandler.Networking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerEntityMixin extends Player {
	@Shadow
	private int containerCounter;

	private ServerPlayerEntityMixin(Level world, BlockPos pos, float yaw, GameProfile gameProfile) {
		super(world, pos, yaw, gameProfile);
	}

	@Shadow
	public abstract void closeContainer();

	@Redirect(method = "openMenu(Lnet/minecraft/world/MenuProvider;)Ljava/util/OptionalInt;", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;closeContainer()V"))
	private void fabric_closeHandledScreenIfAllowed(ServerPlayer player, MenuProvider factory) {
		if (factory.shouldCloseCurrentScreen()) {
			this.closeContainer();
		} else {
			// Called by closeHandledScreen in vanilla
			this.doCloseContainer();
		}
	}

	@Inject(method = "openMenu(Lnet/minecraft/world/MenuProvider;)Ljava/util/OptionalInt;", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;sendPacket(Lnet/minecraft/network/protocol/Packet;)V"), locals = LocalCapture.CAPTURE_FAILHARD)
	private void fabric_storeOpenedScreenHandler(MenuProvider factory, CallbackInfoReturnable<OptionalInt> info, AbstractContainerMenu handler) {
		if (factory instanceof ExtendedScreenHandlerFactory || (factory instanceof SimpleMenuProvider simpleFactory && simpleFactory.menuConstructor instanceof ExtendedScreenHandlerFactory)) {
			// Set the screen handler, so the factory method can access it through the player.
			containerMenu = handler;
		} else if (handler.getType() instanceof ExtendedScreenHandlerType<?, ?>) {
			ResourceLocation id = BuiltInRegistries.MENU.getKey(handler.getType());
			throw new IllegalArgumentException("[Fabric] Extended screen handler " + id + " must be opened with an ExtendedScreenHandlerFactory!");
		}
	}

	@Redirect(method = "openMenu(Lnet/minecraft/world/MenuProvider;)Ljava/util/OptionalInt;", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;sendPacket(Lnet/minecraft/network/protocol/Packet;)V"))
	private void fabric_replaceVanillaScreenPacket(ServerGamePacketListenerImpl networkHandler, Packet<?> packet, MenuProvider factory) {
		if (factory instanceof SimpleMenuProvider simpleFactory && simpleFactory.menuConstructor instanceof ExtendedScreenHandlerFactory<?> extendedFactory) {
			factory = extendedFactory;
		}

		if (factory instanceof ExtendedScreenHandlerFactory<?> extendedFactory) {
			AbstractContainerMenu handler = Objects.requireNonNull(containerMenu);

			if (handler.getType() instanceof ExtendedScreenHandlerType<?, ?>) {
				Networking.sendOpenPacket((ServerPlayer) (Object) this, extendedFactory, handler, containerCounter);
			} else {
				ResourceLocation id = BuiltInRegistries.MENU.getKey(handler.getType());
				throw new IllegalArgumentException("[Fabric] Non-extended screen handler " + id + " must not be opened with an ExtendedScreenHandlerFactory!");
			}
		} else {
			// Use vanilla logic for non-extended screen handlers
			networkHandler.send(packet);
		}
	}
}
