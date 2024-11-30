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

import net.minecraft.client.renderer.block.model.UnbakedBlockStateModel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A simple implementation of {@link UnbakedBlockStateModel} that delegates all method calls to the {@link #wrapped} field.
 * Implementations must set the {@link #wrapped} field somehow.
 */
public abstract class WrapperGroupableModel implements UnbakedBlockStateModel {
	protected UnbakedBlockStateModel wrapped;

	protected WrapperGroupableModel() {
	}

	protected WrapperGroupableModel(UnbakedBlockStateModel wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public void resolveDependencies(Resolver resolver) {
		wrapped.resolveDependencies(resolver);
	}

	@Override
	public BakedModel bake(ModelBaker baker) {
		return wrapped.bake(baker);
	}

	@Override
	public Object visualEqualityGroup(BlockState state) {
		return wrapped.visualEqualityGroup(state);
	}
}
