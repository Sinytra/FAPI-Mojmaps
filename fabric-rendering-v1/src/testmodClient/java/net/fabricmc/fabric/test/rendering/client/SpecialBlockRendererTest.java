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
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.SpecialBlockRendererRegistry;
import net.minecraft.Util;
import net.minecraft.client.model.AllayModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * Tests {@link SpecialBlockRendererRegistry} by rendering an allay model above TNT blocks in a minecart.
 */
public class SpecialBlockRendererTest implements ClientModInitializer {
	private static final ResourceLocation ALLAY_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/allay/allay.png");

	@Override
	public void onInitializeClient() {
		SpecialBlockRendererRegistry.register(Blocks.TNT, new SpecialModelRenderer.Unbaked() {
			@Override
			public SpecialModelRenderer<?> bake(EntityModelSet entityModels) {
				AllayModel allayModel = new AllayModel(entityModels.bakeLayer(ModelLayers.ALLAY));

				return new SpecialModelRenderer<>() {
					@Override
					public void render(@Nullable Object data, ItemDisplayContext modelTransformationMode, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay, boolean glint) {
						matrices.pushPose();
						matrices.translate(0.5f, 0.0f, 0.5f);
						matrices.translate(0, 1.46875f, 0);
						matrices.scale(1, -1, 1);
						matrices.mulPose(Axis.YP.rotation((float) (Util.getMillis() * 0.001)));
						matrices.translate(0, -1.46875f, 0);
						VertexConsumer vertexConsumer = vertexConsumers.getBuffer(allayModel.renderType(ALLAY_TEXTURE));
						allayModel.renderToBuffer(matrices, vertexConsumer, light, overlay);
						matrices.popPose();
					}

					@Override
					@Nullable
					public Object extractArgument(ItemStack stack) {
						return null;
					}
				};
			}

			@Override
			public MapCodec<? extends SpecialModelRenderer.Unbaked> type() {
				return null;
			}
		});
	}
}
