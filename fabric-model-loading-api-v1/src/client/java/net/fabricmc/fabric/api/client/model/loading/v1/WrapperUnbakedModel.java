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

package net.fabricmc.fabric.api.client.model.loading.v1;

import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import org.jetbrains.annotations.Nullable;

/**
 * A simple implementation of {@link UnbakedModel} that delegates all method calls to the {@link #wrapped} field.
 * Implementations must set the {@link #wrapped} field somehow.
 */
public abstract class WrapperUnbakedModel implements UnbakedModel {
	protected UnbakedModel wrapped;

	protected WrapperUnbakedModel() {
	}

	protected WrapperUnbakedModel(UnbakedModel wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public void resolveDependencies(Resolver resolver) {
		wrapped.resolveDependencies(resolver);
	}

	@Override
	public BakedModel bake(TextureSlots textures, ModelBaker baker, ModelState settings, boolean ambientOcclusion, boolean isSideLit, ItemTransforms transformation) {
		return wrapped.bake(textures, baker, settings, ambientOcclusion, isSideLit, transformation);
	}

	@Override
	@Nullable
	public Boolean getAmbientOcclusion() {
		return wrapped.getAmbientOcclusion();
	}

	@Override
	@Nullable
	public GuiLight getGuiLight() {
		return wrapped.getGuiLight();
	}

	@Override
	@Nullable
	public ItemTransforms getTransforms() {
		return wrapped.getTransforms();
	}

	@Override
	public TextureSlots.Data getTextureSlots() {
		return wrapped.getTextureSlots();
	}

	@Override
	@Nullable
	public UnbakedModel getParent() {
		return wrapped.getParent();
	}
}
