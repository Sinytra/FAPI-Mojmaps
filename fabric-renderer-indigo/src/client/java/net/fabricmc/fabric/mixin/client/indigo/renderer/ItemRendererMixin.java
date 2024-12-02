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

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.ItemRenderContext;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;

@Mixin(ItemRenderer.class)
abstract class ItemRendererMixin {
	@Unique
	private static final ThreadLocal<ItemRenderContext> CONTEXTS = ThreadLocal.withInitial(ItemRenderContext::new);

	@Inject(method = "renderItem(Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II[ILnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;)V", at = @At(value = "HEAD"), cancellable = true)
	private static void hookRenderItem(ItemDisplayContext transformationMode, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay, int[] tints, BakedModel model, RenderType layer, ItemStackRenderState.FoilType glint, CallbackInfo ci) {
		if (!model.isVanillaAdapter()) {
			CONTEXTS.get().render(transformationMode, matrices, vertexConsumers, light, overlay, tints, model, layer, glint);
			ci.cancel();
		}
	}
}
