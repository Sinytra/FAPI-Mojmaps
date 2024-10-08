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

package net.fabricmc.fabric.mixin.client.indigo.renderer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.ItemRenderContext;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
	@Final
	@Shadow
	private ItemColors colors;

	@Unique
	private final ThreadLocal<ItemRenderContext> fabric_contexts = ThreadLocal.withInitial(() -> new ItemRenderContext(colors));

	@Inject(method = "renderItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;Z)V", at = @At(value = "HEAD"), cancellable = true)
	public void hook_renderItem(ItemStack stack, ItemDisplayContext transformMode, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, int light, int overlay, BakedModel model, boolean notInHand, CallbackInfo ci) {
		if (!model.isVanillaAdapter()) {
			fabric_contexts.get().renderModel(stack, transformMode, matrixStack, vertexConsumerProvider, light, overlay, model);
			ci.cancel();
		}
	}
}
