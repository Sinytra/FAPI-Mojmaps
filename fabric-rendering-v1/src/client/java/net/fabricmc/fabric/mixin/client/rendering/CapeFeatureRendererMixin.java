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

package net.fabricmc.fabric.mixin.client.rendering;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.item.equipment.EquipmentModel;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRenderEvents;

@Mixin(CapeLayer.class)
public class CapeFeatureRendererMixin {
	@WrapOperation(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/layers/CapeLayer;hasCustomModelForLayer(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/item/equipment/EquipmentModel$LayerType;)Z"), method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/PlayerRenderState;FF)V")
	public boolean injectCapeRenderCheck(CapeLayer instance, ItemStack itemStack, EquipmentModel.LayerType layerType, Operation<Boolean> original, @Local(argsOnly = true) PlayerRenderState state) {
		if (!LivingEntityFeatureRenderEvents.ALLOW_CAPE_RENDER.invoker().allowCapeRender(state)) {
			return false;
		}

		return original.call(instance, itemStack, layerType);
	}
}
