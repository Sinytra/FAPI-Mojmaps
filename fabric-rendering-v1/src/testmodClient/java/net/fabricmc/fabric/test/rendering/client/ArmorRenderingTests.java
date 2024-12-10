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

package net.fabricmc.fabric.test.rendering.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;

public class ArmorRenderingTests implements ClientModInitializer {
	private HumanoidModel<HumanoidRenderState> armorModel;
	private final ResourceLocation texture = ResourceLocation.withDefaultNamespace("textures/block/dirt.png");

	// Renders a biped model with dirt texture, replacing diamond helmet and diamond chest plate rendering
	@Override
	public void onInitializeClient() {
		ArmorRenderer.register((matrices, vertexConsumers, stack, renderState, slot, light, model) -> {
			if (armorModel == null) {
				armorModel = new HumanoidModel<>(Minecraft.getInstance().getEntityModelLoader().getModelPart(ModelLayers.PLAYER_OUTER_ARMOR));
			}

			armorModel.setupAnim(renderState);
			armorModel.setAllVisible(false);
			armorModel.body.visible = slot == EquipmentSlot.CHEST;
			armorModel.leftArm.visible = slot == EquipmentSlot.CHEST;
			armorModel.rightArm.visible = slot == EquipmentSlot.CHEST;
			armorModel.head.visible = slot == EquipmentSlot.HEAD;
			ArmorRenderer.renderPart(matrices, vertexConsumers, light, stack, armorModel, texture);
		}, Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE);
	}
}
