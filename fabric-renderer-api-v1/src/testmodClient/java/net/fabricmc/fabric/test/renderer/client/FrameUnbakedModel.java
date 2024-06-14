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

package net.fabricmc.fabric.test.renderer.client;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshBuilder;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public class FrameUnbakedModel implements UnbakedModel {
	private static final Material OBSIDIAN_SPRITE_ID = new Material(TextureAtlas.LOCATION_BLOCKS, ResourceLocation.withDefaultNamespace("block/obsidian"));

	@Override
	public Collection<ResourceLocation> getDependencies() {
		return Collections.emptySet();
	}

	@Override
	public void resolveParents(Function<ResourceLocation, UnbakedModel> modelLoader) {
	}

	/*
	 * Bake the model.
	 * In this case we can prebake the frame into a mesh, but will render the contained block when we draw the quads.
	 */
	@Nullable
	@Override
	public BakedModel bake(ModelBaker baker, Function<Material, TextureAtlasSprite> textureGetter, ModelState rotationContainer) {
		// The renderer API may not have an implementation, so we should check if it exists.
		if (!RendererAccess.INSTANCE.hasRenderer()) {
			// No renderer implementation is present.
			return null;
		}

		TextureAtlasSprite obsidianSprite = textureGetter.apply(OBSIDIAN_SPRITE_ID);

		Renderer renderer = RendererAccess.INSTANCE.getRenderer();
		MeshBuilder builder = renderer.meshBuilder();
		QuadEmitter emitter = builder.getEmitter();

		for (Direction direction : Direction.values()) {
			// Draw outer frame
			emitter.square(direction, 0.0F, 0.9F, 0.9F, 1.0F, 0.0F)
					.spriteBake(obsidianSprite, MutableQuadView.BAKE_LOCK_UV)
					.color(-1, -1, -1, -1)
					.emit();

			emitter.square(direction, 0.0F, 0.0F, 0.1F, 0.9F, 0.0F)
					.spriteBake(obsidianSprite, MutableQuadView.BAKE_LOCK_UV)
					.color(-1, -1, -1, -1)
					.emit();

			emitter.square(direction, 0.9F, 0.1F, 1.0F, 1.0F, 0.0F)
					.spriteBake(obsidianSprite, MutableQuadView.BAKE_LOCK_UV)
					.color(-1, -1, -1, -1)
					.emit();

			emitter.square(direction, 0.1F, 0.0F, 1.0F, 0.1F, 0.0F)
					.spriteBake(obsidianSprite, MutableQuadView.BAKE_LOCK_UV)
					.color(-1, -1, -1, -1)
					.emit();

			// Draw inner frame - inset by 0.9 so the frame looks like an actual mesh
			emitter.square(direction, 0.0F, 0.9F, 0.9F, 1.0F, 0.9F)
					.spriteBake(obsidianSprite, MutableQuadView.BAKE_LOCK_UV)
					.color(-1, -1, -1, -1)
					.emit();

			emitter.square(direction, 0.0F, 0.0F, 0.1F, 0.9F, 0.9F)
					.spriteBake(obsidianSprite, MutableQuadView.BAKE_LOCK_UV)
					.color(-1, -1, -1, -1)
					.emit();

			emitter.square(direction, 0.9F, 0.1F, 1.0F, 1.0F, 0.9F)
					.spriteBake(obsidianSprite, MutableQuadView.BAKE_LOCK_UV)
					.color(-1, -1, -1, -1)
					.emit();

			emitter.square(direction, 0.1F, 0.0F, 1.0F, 0.1F, 0.9F)
					.spriteBake(obsidianSprite, MutableQuadView.BAKE_LOCK_UV)
					.color(-1, -1, -1, -1)
					.emit();
		}

		return new FrameBakedModel(builder.build(), obsidianSprite);
	}
}
