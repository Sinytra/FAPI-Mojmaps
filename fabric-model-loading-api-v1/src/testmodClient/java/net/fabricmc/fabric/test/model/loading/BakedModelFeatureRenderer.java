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

package net.fabricmc.fabric.test.model.loading;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.Mth;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public class BakedModelFeatureRenderer<S extends LivingEntityRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {
	private final Supplier<BakedModel> modelSupplier;

	public BakedModelFeatureRenderer(RenderLayerParent<S, M> context, Supplier<BakedModel> modelSupplier) {
		super(context);
		this.modelSupplier = modelSupplier;
	}

	@Override
	public void render(PoseStack matrices, MultiBufferSource vertexConsumers, int light, S state, float limbAngle, float limbDistance) {
		BakedModel model = modelSupplier.get();
		VertexConsumer vertices = vertexConsumers.getBuffer(Sheets.cutoutBlockSheet());
		matrices.pushPose();
		matrices.mulPose(new Quaternionf(new AxisAngle4f(state.ageInTicks * 0.07F - state.bodyRot * Mth.DEG_TO_RAD, 0, 1, 0)));
		matrices.scale(-0.75F, -0.75F, 0.75F);
		float aboveHead = (float) (Math.sin(state.ageInTicks * 0.08F)) * 0.5F + 0.5F;
		matrices.translate(-0.5F, 0.75F + aboveHead, -0.5F);
		Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(matrices.last(), vertices, null, model, 1, 1, 1, light, OverlayTexture.NO_OVERLAY);
		matrices.popPose();
	}
}
