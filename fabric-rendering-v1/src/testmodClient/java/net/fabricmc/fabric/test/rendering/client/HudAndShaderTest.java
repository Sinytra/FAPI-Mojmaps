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

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

/**
 * Tests {@link HudRenderCallback} and {@link CoreShaderRegistrationCallback} by drawing a green rectangle
 * in the lower-right corner of the screen.
 */
public class HudAndShaderTest implements ClientModInitializer {
	private static ShaderInstance testShader;

	@Override
	public void onInitializeClient() {
		CoreShaderRegistrationCallback.EVENT.register(context -> {
			// Register a custom shader taking POSITION vertices.
			ResourceLocation id = ResourceLocation.fromNamespaceAndPath("fabric-rendering-v1-testmod", "test");
			context.register(id, DefaultVertexFormat.POSITION, program -> testShader = program);
		});

		HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
			Minecraft client = Minecraft.getInstance();
			Window window = client.getWindow();
			int x = window.getGuiScaledWidth() - 15;
			int y = window.getGuiScaledHeight() - 15;
			RenderSystem.setShader(() -> testShader);
			RenderSystem.setShaderColor(0f, 1f, 0f, 1f);
			Matrix4f positionMatrix = drawContext.pose().last().pose();
			BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
			buffer.addVertex(positionMatrix, x, y, 50);
			buffer.addVertex(positionMatrix, x, y + 10, 50);
			buffer.addVertex(positionMatrix, x + 10, y + 10, 50);
			buffer.addVertex(positionMatrix, x + 10, y, 50);
			BufferUploader.drawWithShader(buffer.buildOrThrow());
			// Reset shader color
			RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
		});
	}
}
