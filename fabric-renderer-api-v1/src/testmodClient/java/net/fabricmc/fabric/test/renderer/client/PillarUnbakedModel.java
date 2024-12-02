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

import java.util.List;
import java.util.stream.Stream;
import net.fabricmc.fabric.test.renderer.RendererTest;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;

public class PillarUnbakedModel implements UnbakedModel {
	private static final List<Material> SPRITES = Stream.of("alone", "bottom", "middle", "top")
			.map(suffix -> new Material(TextureAtlas.LOCATION_BLOCKS, RendererTest.id("block/pillar_" + suffix)))
			.toList();

	@Override
	public void resolveDependencies(Resolver resolver) {
	}

	@Override
	public BakedModel bake(TextureSlots textures, ModelBaker baker, ModelState settings, boolean ambientOcclusion, boolean isSideLit, ItemTransforms transformation) {
		TextureAtlasSprite[] sprites = new TextureAtlasSprite[SPRITES.size()];

		for (int i = 0; i < sprites.length; ++i) {
			sprites[i] = baker.sprites().get(SPRITES.get(i));
		}

		return new PillarBakedModel(sprites);
	}
}
