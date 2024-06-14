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

import java.util.Set;

import org.jetbrains.annotations.Nullable;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelResolver;
import net.fabricmc.fabric.test.renderer.RendererTest;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;

public class ModelResolverImpl implements ModelResolver {
	private static final Set<ResourceLocation> FRAME_MODEL_LOCATIONS = Set.of(
			RendererTest.id("block/frame"),
			RendererTest.id("item/frame"),
			RendererTest.id("item/frame_multipart"),
			RendererTest.id("item/frame_variant")
	);

	private static final Set<ResourceLocation> PILLAR_MODEL_LOCATIONS = Set.of(
			RendererTest.id("block/pillar"),
			RendererTest.id("item/pillar")
	);

	private static final Set<ResourceLocation> OCTAGONAL_COLUMN_MODEL_LOCATIONS = Set.of(
			RendererTest.id("block/octagonal_column"),
			RendererTest.id("item/octagonal_column")
	);

	private static final Set<ResourceLocation> RIVERSTONE_MODEL_LOCATIONS = Set.of(
			RendererTest.id("block/riverstone"),
			RendererTest.id("item/riverstone")
	);

	@Override
	@Nullable
	public UnbakedModel resolveModel(Context context) {
		ResourceLocation id = context.id();

		if (FRAME_MODEL_LOCATIONS.contains(id)) {
			return new FrameUnbakedModel();
		}

		if (PILLAR_MODEL_LOCATIONS.contains(id)) {
			return new PillarUnbakedModel();
		}

		if (OCTAGONAL_COLUMN_MODEL_LOCATIONS.contains(id)) {
			return new OctagonalColumnUnbakedModel();
		}

		if (RIVERSTONE_MODEL_LOCATIONS.contains(id)) {
			return new RiverstoneUnbakedModel();
		}

		return null;
	}
}
