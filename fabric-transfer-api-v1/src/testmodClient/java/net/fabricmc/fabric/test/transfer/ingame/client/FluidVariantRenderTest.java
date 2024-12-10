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

package net.fabricmc.fabric.test.transfer.ingame.client;

import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluids;

/**
 * Renders the water sprite in the top left of the screen, to make sure that it correctly depends on the position.
 */
public class FluidVariantRenderTest implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		FluidVariantAttributes.enableColoredVanillaFluidNames();

		HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
			Player player = Minecraft.getInstance().player;
			if (player == null) return;

			if (Minecraft.getInstance().gui.getDebugOverlay().showDebugScreen()) return;

			int renderY = 0;
			List<FluidVariant> variants = List.of(FluidVariant.of(Fluids.WATER), FluidVariant.of(Fluids.LAVA));

			for (FluidVariant variant : variants) {
				TextureAtlasSprite[] sprites = FluidVariantRendering.getSprites(variant);
				int color = FluidVariantRendering.getColor(variant, player.level(), player.blockPosition());

				if (sprites != null) {
					drawFluidInGui(drawContext, sprites[0], color, 0, renderY);
					renderY += 16;
					drawFluidInGui(drawContext, sprites[1], color, 0, renderY);
					renderY += 16;
				}

				List<Component> tooltip = FluidVariantRendering.getTooltip(variant);
				Font textRenderer = Minecraft.getInstance().font;

				renderY += 2;

				for (Component line : tooltip) {
					renderY += 10;
					drawContext.renderTooltip(textRenderer, line, -8, renderY);
				}
			}
		});
	}

	private static void drawFluidInGui(GuiGraphics drawContext, TextureAtlasSprite sprite, int color, int i, int j) {
		if (sprite == null) return;

		RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);

		float r = ((color >> 16) & 255) / 255f;
		float g = ((color >> 8) & 255) / 255f;
		float b = (color & 255) / 255f;
		RenderSystem.disableDepthTest();

		RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);
		BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		float x0 = (float) i;
		float y0 = (float) j;
		float x1 = x0 + 16;
		float y1 = y0 + 16;
		float z = 0.5f;
		float u0 = sprite.getU0();
		float v0 = sprite.getV0();
		float u1 = sprite.getU1();
		float v1 = sprite.getV1();
		Matrix4f model = drawContext.pose().last().pose();
		bufferBuilder.addVertex(model, x0, y1, z).setColor(r, g, b, 1).setUv(u0, v1);
		bufferBuilder.addVertex(model, x1, y1, z).setColor(r, g, b, 1).setUv(u1, v1);
		bufferBuilder.addVertex(model, x1, y0, z).setColor(r, g, b, 1).setUv(u1, v0);
		bufferBuilder.addVertex(model, x0, y0, z).setColor(r, g, b, 1).setUv(u0, v0);
		BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());

		RenderSystem.enableDepthTest();
	}
}
