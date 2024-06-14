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

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public class DimensionalRenderingTest implements ClientModInitializer {
	private static final ResourceLocation END_SKY = ResourceLocation.withDefaultNamespace("textures/block/dirt.png");

	private static void render(WorldRenderContext context) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.depthMask(false);
		RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
		RenderSystem.setShaderTexture(0, END_SKY);
		Tesselator tessellator = Tesselator.getInstance();
		BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

		Matrix4f matrix4f = context.positionMatrix();
		bufferBuilder.addVertex(matrix4f, -100.0f, -100.0f, -100.0f).setUv(0.0F, 0.0F).setColor(255, 255, 255, 255);
		bufferBuilder.addVertex(matrix4f, -100.0f, -100.0f, 100.0f).setUv(0.0F, 1.0F).setColor(255, 255, 255, 255);
		bufferBuilder.addVertex(matrix4f, 100.0f, -100.0f, 100.0f).setUv(1.0F, 1.0F).setColor(255, 255, 255, 255);
		bufferBuilder.addVertex(matrix4f, 100.0f, -100.0f, -100.0f).setUv(1.0F, 0.0F).setColor(255, 255, 255, 255);

		bufferBuilder.addVertex(matrix4f, -100.0f, 100.0f, -100.0f).setUv(0.0F, 0.0F).setColor(255, 255, 255, 255);
		bufferBuilder.addVertex(matrix4f, -100.0f, -100.0f, -99.0f).setUv(0.0F, 1.0F).setColor(255, 255, 255, 255);
		bufferBuilder.addVertex(matrix4f, 100.0f, -100.0f, -99.0f).setUv(1.0F, 1.0F).setColor(255, 255, 255, 255);
		bufferBuilder.addVertex(matrix4f, 100.0f, 100.0f, -100.0f).setUv(1.0F, 0.0F).setColor(255, 255, 255, 255);

		bufferBuilder.addVertex(matrix4f, -100.0f, -100.0f, 100.0f).setUv(0.0F, 0.0F).setColor(255, 255, 255, 255);
		bufferBuilder.addVertex(matrix4f, -100.0f, 100.0f, 100.0f).setUv(0.0F, 1.0F).setColor(255, 255, 255, 255);
		bufferBuilder.addVertex(matrix4f, 100.0f, 100.0f, 100.0f).setUv(1.0F, 1.0F).setColor(255, 255, 255, 255);
		bufferBuilder.addVertex(matrix4f, 100.0f, -100.0f, 100.0f).setUv(1.0F, 0.0F).setColor(255, 255, 255, 255);

		bufferBuilder.addVertex(matrix4f, -100.0f, 100.0f, 101.0f).setUv(0.0F, 0.0F).setColor(255, 255, 255, 255);
		bufferBuilder.addVertex(matrix4f, -100.0f, 100.0f, -100.0f).setUv(0.0F, 1.0F).setColor(255, 255, 255, 255);
		bufferBuilder.addVertex(matrix4f, 100.0f, 100.0f, -100.0f).setUv(1.0F, 1.0F).setColor(255, 255, 255, 255);
		bufferBuilder.addVertex(matrix4f, 100.0f, 100.0f, 100.0f).setUv(1.0F, 0.0F).setColor(255, 255, 255, 255);

		bufferBuilder.addVertex(matrix4f, 100.0f, -100.0f, -100.0f).setUv(0.0F, 0.0F).setColor(255, 255, 255, 255);
		bufferBuilder.addVertex(matrix4f, 100.0f, -100.0f, 100.0f).setUv(0.0F, 1.0F).setColor(255, 255, 255, 255);
		bufferBuilder.addVertex(matrix4f, 100.0f, 100.0f, 100.0f).setUv(1.0F, 1.0F).setColor(255, 255, 255, 255);
		bufferBuilder.addVertex(matrix4f, 100.0f, 100.0f, -100.0f).setUv(1.0F, 0.0F).setColor(255, 255, 255, 255);

		bufferBuilder.addVertex(matrix4f, -100.0f, 100.0f, -100.0f).setUv(0.0F, 0.0F).setColor(255, 255, 255, 255);
		bufferBuilder.addVertex(matrix4f, -100.0f, 100.0f, 100.0f).setUv(0.0F, 1.0F).setColor(255, 255, 255, 255);
		bufferBuilder.addVertex(matrix4f, -100.0f, -100.0f, 100.0f).setUv(1.0F, 1.0F).setColor(255, 255, 255, 255);
		bufferBuilder.addVertex(matrix4f, -100.0f, -100.0f, -100.0f).setUv(1.0F, 0.0F).setColor(255, 255, 255, 255);
		BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());

		RenderSystem.depthMask(true);
		RenderSystem.disableBlend();
	}

	@Override
	public void onInitializeClient() {
		DimensionRenderingRegistry.registerSkyRenderer(ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("fabric_dimension", "void")), DimensionalRenderingTest::render);
		DimensionRenderingRegistry.registerDimensionEffects(ResourceLocation.fromNamespaceAndPath("fabric_dimension", "void"), new DimensionSpecialEffects.EndEffects());
	}
}
