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

package net.fabricmc.fabric.impl.client.model.loading;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.fabric.api.client.model.loading.v1.BlockStateResolver;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.UnbakedBlockStateModel;
import net.minecraft.client.resources.model.BlockStateModelLoader;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ModelLoadingEventDispatcher {
	private static final Logger LOGGER = LoggerFactory.getLogger(ModelLoadingEventDispatcher.class);
	public static final ThreadLocal<ModelLoadingEventDispatcher> CURRENT = new ThreadLocal<>();

	private final ModelLoadingPluginContextImpl pluginContext;

	private final BlockStateResolverContext blockStateResolverContext = new BlockStateResolverContext();

	private final OnLoadModifierContext onLoadModifierContext = new OnLoadModifierContext();
	private final OnLoadBlockModifierContext onLoadBlockModifierContext = new OnLoadBlockModifierContext();

	public ModelLoadingEventDispatcher(List<ModelLoadingPlugin> plugins) {
		this.pluginContext = new ModelLoadingPluginContextImpl();

		for (ModelLoadingPlugin plugin : plugins) {
			try {
				plugin.initialize(pluginContext);
			} catch (Exception exception) {
				LOGGER.error("Failed to initialize model loading plugin", exception);
			}
		}
	}

	public void forEachExtraModel(Consumer<ResourceLocation> extraModelConsumer) {
		pluginContext.extraModels.forEach(extraModelConsumer);
	}

	@Nullable
	public UnbakedModel modifyModelOnLoad(@Nullable UnbakedModel model, ResourceLocation id) {
		onLoadModifierContext.prepare(id);
		return pluginContext.modifyModelOnLoad().invoker().modifyModelOnLoad(model, onLoadModifierContext);
	}

	public BlockStateModelLoader.LoadedModels modifyBlockModelsOnLoad(BlockStateModelLoader.LoadedModels models) {
		Map<ModelResourceLocation, BlockStateModelLoader.LoadedModel> map = models.models();

		if (!(map instanceof HashMap)) {
			map = new HashMap<>(map);
			models = new BlockStateModelLoader.LoadedModels(map);
		}

		putResolvedBlockStates(map);

		map.replaceAll((id, blockModel) -> {
			UnbakedBlockStateModel original = blockModel.model();
			UnbakedBlockStateModel modified = modifyBlockModelOnLoad(original, id, blockModel.state());

			if (original != modified) {
				return new BlockStateModelLoader.LoadedModel(blockModel.state(), modified);
			}

			return blockModel;
		});

		return models;
	}

	private void putResolvedBlockStates(Map<ModelResourceLocation, BlockStateModelLoader.LoadedModel> map) {
		pluginContext.blockStateResolvers.forEach((block, resolver) -> {
			Optional<ResourceKey<Block>> optionalKey = BuiltInRegistries.BLOCK.getResourceKey(block);

			if (optionalKey.isEmpty()) {
				return;
			}

			ResourceLocation blockId = optionalKey.get().location();

			resolveBlockStates(resolver, block, (state, model) -> {
				ModelResourceLocation modelId = BlockModelShaper.stateToModelLocation(blockId, state);
				map.put(modelId, new BlockStateModelLoader.LoadedModel(state, model));
			});
		});
	}

	private void resolveBlockStates(BlockStateResolver resolver, Block block, BiConsumer<BlockState, UnbakedBlockStateModel> output) {
		BlockStateResolverContext context = blockStateResolverContext;
		context.prepare(block);

		Reference2ReferenceMap<BlockState, UnbakedBlockStateModel> resolvedModels = context.models;
		ImmutableList<BlockState> allStates = block.getStateDefinition().getPossibleStates();
		boolean thrown = false;

		try {
			resolver.resolveBlockStates(context);
		} catch (Exception e) {
			LOGGER.error("Failed to resolve block state models for block {}. Using missing model for all states.", block, e);
			thrown = true;
		}

		if (!thrown) {
			if (resolvedModels.size() == allStates.size()) {
				// If there are as many resolved models as total states, all states have
				// been resolved and models do not need to be null-checked.
				resolvedModels.forEach(output);
			} else {
				for (BlockState state : allStates) {
					@Nullable
					UnbakedBlockStateModel model = resolvedModels.get(state);

					if (model == null) {
						LOGGER.error("Block state resolver did not provide a model for state {} in block {}. Using missing model.", state, block);
					} else {
						output.accept(state, model);
					}
				}
			}
		}

		resolvedModels.clear();
	}

	private UnbakedBlockStateModel modifyBlockModelOnLoad(UnbakedBlockStateModel model, ModelResourceLocation id, BlockState state) {
		onLoadBlockModifierContext.prepare(id, state);
		return pluginContext.modifyBlockModelOnLoad().invoker().modifyModelOnLoad(model, onLoadBlockModifierContext);
	}

	private static class BlockStateResolverContext implements BlockStateResolver.Context {
		private Block block;
		private final Reference2ReferenceMap<BlockState, UnbakedBlockStateModel> models = new Reference2ReferenceOpenHashMap<>();

		private void prepare(Block block) {
			this.block = block;
			models.clear();
		}

		@Override
		public Block block() {
			return block;
		}

		@Override
		public void setModel(BlockState state, UnbakedBlockStateModel model) {
			Objects.requireNonNull(model, "state cannot be null");
			Objects.requireNonNull(model, "model cannot be null");

			if (!state.is(block)) {
				throw new IllegalArgumentException("Attempted to set model for state " + state + " on block " + block);
			}

			if (models.putIfAbsent(state, model) != null) {
				throw new IllegalStateException("Duplicate model for state " + state + " on block " + block);
			}
		}
	}

	private static class OnLoadModifierContext implements ModelModifier.OnLoad.Context {
		private ResourceLocation id;

		private void prepare(ResourceLocation id) {
			this.id = id;
		}

		@Override
		public ResourceLocation id() {
			return id;
		}
	}

	private static class OnLoadBlockModifierContext implements ModelModifier.OnLoadBlock.Context {
		private ModelResourceLocation id;
		private BlockState state;

		private void prepare(ModelResourceLocation id, BlockState state) {
			this.id = id;
			this.state = state;
		}

		@Override
		public ModelResourceLocation id() {
			return id;
		}

		@Override
		public BlockState state() {
			return state;
		}
	}
}
