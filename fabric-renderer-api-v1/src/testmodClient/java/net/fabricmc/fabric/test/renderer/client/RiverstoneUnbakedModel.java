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
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class RiverstoneUnbakedModel implements UnbakedModel {
	private static final ResourceLocation STONE_MODEL_ID = ResourceLocation.withDefaultNamespace("block/stone");
	private static final ResourceLocation GOLD_BLOCK_MODEL_ID = ResourceLocation.withDefaultNamespace("block/gold_block");

	@Override
	public Collection<ResourceLocation> getDependencies() {
		return Collections.emptySet();
	}

	@Override
	public void resolveParents(Function<ResourceLocation, UnbakedModel> modelLoader) {
		modelLoader.apply(STONE_MODEL_ID).resolveParents(modelLoader);
		modelLoader.apply(GOLD_BLOCK_MODEL_ID).resolveParents(modelLoader);
	}

	@Nullable
	@Override
	public BakedModel bake(ModelBaker baker, Function<Material, TextureAtlasSprite> textureGetter, ModelState rotationContainer) {
		BakedModel stoneModel = baker.bake(STONE_MODEL_ID, rotationContainer);
		BakedModel goldBlockModel = baker.bake(GOLD_BLOCK_MODEL_ID, rotationContainer);
		return new RiverstoneBakedModel(stoneModel, goldBlockModel);
	}
}
