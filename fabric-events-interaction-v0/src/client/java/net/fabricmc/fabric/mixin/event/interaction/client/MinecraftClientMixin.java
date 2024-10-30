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

package net.fabricmc.fabric.mixin.event.interaction.client;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin {
	private boolean fabric_attackCancelled;

	@Shadow
	private LocalPlayer player;

	@Shadow
	public abstract ClientPacketListener getConnection();

	@Shadow
	@Final
	public Options options;

	@Shadow
	@Nullable
	public MultiPlayerGameMode gameMode;

	@Shadow
	@Nullable
	public ClientLevel level;

	@Inject(
			at = @At(
					value = "INVOKE",
					target = "net/minecraft/client/network/ClientPlayerInteractionManager.interactEntityAtLocation(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/hit/EntityHitResult;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;"
			),
			method = "startUseItem",
			cancellable = true,
			locals = LocalCapture.CAPTURE_FAILHARD
	)
	private void injectUseEntityCallback(CallbackInfo ci, InteractionHand[] hands, int i1, int i2, InteractionHand hand, ItemStack stack, EntityHitResult hitResult, Entity entity) {
		InteractionResult result = UseEntityCallback.EVENT.invoker().interact(player, player.getCommandSenderWorld(), hand, entity, hitResult);

		if (result != InteractionResult.PASS) {
			if (result.consumesAction()) {
				Vec3 hitVec = hitResult.getLocation().subtract(entity.getX(), entity.getY(), entity.getZ());
				getConnection().send(ServerboundInteractPacket.createInteractionPacket(entity, player.isShiftKeyDown(), hand, hitVec));
			}

			if (result instanceof InteractionResult.Success success) {
				if (success.swingSource() == InteractionResult.SwingSource.CLIENT) {
					player.swing(hand);
				}
			}

			ci.cancel();
		}
	}

	@Inject(
			method = "handleKeybinds",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z",
					ordinal = 0
			)
	)
	private void injectHandleInputEventsForPreAttackCallback(CallbackInfo ci) {
		int attackKeyPressCount = ((KeyBindingAccessor) options.keyAttack).fabric_getTimesPressed();

		if (options.keyAttack.isDown() || attackKeyPressCount != 0) {
			fabric_attackCancelled = ClientPreAttackCallback.EVENT.invoker().onClientPlayerPreAttack(
					(Minecraft) (Object) this, player, attackKeyPressCount
			);
		} else {
			fabric_attackCancelled = false;
		}
	}

	@Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
	private void injectDoAttackForCancelling(CallbackInfoReturnable<Boolean> cir) {
		if (fabric_attackCancelled) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
	private void injectHandleBlockBreakingForCancelling(boolean breaking, CallbackInfo ci) {
		if (fabric_attackCancelled) {
			if (gameMode != null) {
				gameMode.stopDestroyBlock();
			}

			ci.cancel();
		}
	}
}
