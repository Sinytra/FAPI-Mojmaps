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

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.model.ArmorStandArmorModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;

/**
 * This test exists solely for testing generics.
 * As such it is not in the mod json
 */
public class FeatureRendererGenericTests implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// These aren't tests in the normal sense. These exist to test that generics are sane.
		LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
			if (entityRenderer instanceof PlayerRenderer) {
				registrationHelper.register(new TestPlayerFeature((PlayerRenderer) entityRenderer));

				// This is T extends AbstractClientPlayerEntity
				registrationHelper.register(new GenericTestPlayerFeature<>((PlayerRenderer) entityRenderer));
			}

			if (entityRenderer instanceof ArmorStandRenderer) {
				registrationHelper.register(new TestArmorStandFeature((ArmorStandRenderer) entityRenderer));
			}

			// Obviously not recommended, just used for testing generics
			registrationHelper.register(new ElytraLayer<>(entityRenderer, context.getModelSet()));

			if (entityRenderer instanceof HumanoidMobRenderer) {
				// It works, method ref is encouraged
				registrationHelper.register(new ItemInHandLayer<>((HumanoidMobRenderer<?, ?>) entityRenderer, context.getItemInHandRenderer()));
			}
		});

		LivingEntityFeatureRendererRegistrationCallback.EVENT.register(this::registerFeatures);
	}

	private void registerFeatures(EntityType<? extends LivingEntity> entityType, LivingEntityRenderer<?, ?> entityRenderer, LivingEntityFeatureRendererRegistrationCallback.RegistrationHelper registrationHelper, EntityRendererProvider.Context context) {
		if (entityRenderer instanceof PlayerRenderer) {
			registrationHelper.register(new TestPlayerFeature((PlayerRenderer) entityRenderer));

			// This is T extends AbstractClientPlayerEntity
			registrationHelper.register(new GenericTestPlayerFeature<>((PlayerRenderer) entityRenderer));
		}

		if (entityRenderer instanceof ArmorStandRenderer) {
			registrationHelper.register(new TestArmorStandFeature((ArmorStandRenderer) entityRenderer));
		}

		// Obviously not recommended, just used for testing generics.
		registrationHelper.register(new ElytraLayer<>(entityRenderer, context.getModelSet()));

		if (entityRenderer instanceof HumanoidMobRenderer) {
			// It works, method ref is encouraged
			registrationHelper.register(new ItemInHandLayer<>((HumanoidMobRenderer<?, ?>) entityRenderer, context.getItemInHandRenderer()));
		}
	}

	static class TestPlayerFeature extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
		TestPlayerFeature(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> context) {
			super(context);
		}

		@Override
		public void render(PoseStack matrices, MultiBufferSource vertexConsumers, int light, AbstractClientPlayer entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
		}
	}

	static class GenericTestPlayerFeature<T extends AbstractClientPlayer, M extends PlayerModel<T>> extends RenderLayer<T, M> {
		GenericTestPlayerFeature(RenderLayerParent<T, M> context) {
			super(context);
		}

		@Override
		public void render(PoseStack matrices, MultiBufferSource vertexConsumers, int light, T entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
		}
	}

	static class TestArmorStandFeature extends RenderLayer<ArmorStand, ArmorStandArmorModel> {
		TestArmorStandFeature(RenderLayerParent<ArmorStand, ArmorStandArmorModel> context) {
			super(context);
		}

		@Override
		public void render(PoseStack matrices, MultiBufferSource vertexConsumers, int light, ArmorStand entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
		}
	}
}
