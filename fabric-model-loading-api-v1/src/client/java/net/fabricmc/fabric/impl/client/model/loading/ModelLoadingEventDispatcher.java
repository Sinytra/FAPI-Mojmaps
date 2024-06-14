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

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.fabric.api.client.model.loading.v1.BlockStateResolver;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelResolver;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public class ModelLoadingEventDispatcher {
	private static final Logger LOGGER = LoggerFactory.getLogger(ModelLoadingEventDispatcher.class);

	private final ModelBakery loader;
	private final ModelLoadingPluginContextImpl pluginContext;

	private final ObjectArrayList<ModelResolverContext> modelResolverContextStack = new ObjectArrayList<>();
	private final BlockStateResolverContext blockStateResolverContext = new BlockStateResolverContext();

	private final ObjectArrayList<OnLoadModifierContext> onLoadModifierContextStack = new ObjectArrayList<>();
	private final ObjectArrayList<BeforeBakeModifierContext> beforeBakeModifierContextStack = new ObjectArrayList<>();
	private final ObjectArrayList<AfterBakeModifierContext> afterBakeModifierContextStack = new ObjectArrayList<>();

	public ModelLoadingEventDispatcher(ModelBakery loader, List<ModelLoadingPlugin> plugins) {
		this.loader = loader;
		this.pluginContext = new ModelLoadingPluginContextImpl();

		for (ModelLoadingPlugin plugin : plugins) {
			try {
				plugin.onInitializeModelLoader(pluginContext);
			} catch (Exception exception) {
				LOGGER.error("Failed to initialize model loading plugin", exception);
			}
		}
	}

	public void addExtraModels(Consumer<ResourceLocation> extraModelConsumer) {
		for (ResourceLocation id : pluginContext.extraModels) {
			extraModelConsumer.accept(id);
		}
	}

	public boolean loadBlockStateModels(ResourceLocation id, StateDefinition<Block, BlockState> stateManager) {
		BlockStateResolver resolver = pluginContext.blockStateResolvers.get(id);

		if (resolver != null) {
			resolveBlockStates(resolver, stateManager.getOwner(), id);
			return true;
		} else {
			return false;
		}
	}

	private void resolveBlockStates(BlockStateResolver resolver, Block block, ResourceLocation blockId) {
		BlockStateResolverContext context = blockStateResolverContext;
		context.prepare(block);

		Reference2ReferenceMap<BlockState, UnbakedModel> resolvedModels = context.models;
		ImmutableList<BlockState> allStates = block.getStateDefinition().getPossibleStates();
		boolean thrown = false;

		// Call resolver
		try {
			resolver.resolveBlockStates(context);
		} catch (Exception e) {
			LOGGER.error("Failed to resolve block state models for block {}. Using missing model for all states.", block, e);
			thrown = true;
		}

		// Copy models over to the loader
		if (thrown) {
			UnbakedModel missingModel = ((ModelLoaderHooks) loader).fabric_getMissingModel();

			for (BlockState state : allStates) {
				ModelResourceLocation modelId = BlockModelShaper.stateToModelLocation(blockId, state);
				((ModelLoaderHooks) loader).fabric_add(modelId, missingModel);
			}
		} else if (resolvedModels.size() == allStates.size()) {
			// If there are as many resolved models as total states, all states have
			// been resolved and models do not need to be null-checked.
			resolvedModels.forEach((state, model) -> {
				ModelResourceLocation modelId = BlockModelShaper.stateToModelLocation(blockId, state);
				((ModelLoaderHooks) loader).fabric_add(modelId, model);
			});
		} else {
			UnbakedModel missingModel = ((ModelLoaderHooks) loader).fabric_getMissingModel();

			for (BlockState state : allStates) {
				ModelResourceLocation modelId = BlockModelShaper.stateToModelLocation(blockId, state);
				@Nullable
				UnbakedModel model = resolvedModels.get(state);

				if (model == null) {
					LOGGER.error("Block state resolver did not provide a model for state {} in block {}. Using missing model.", state, block);
					((ModelLoaderHooks) loader).fabric_add(modelId, missingModel);
				} else {
					((ModelLoaderHooks) loader).fabric_add(modelId, model);
				}
			}
		}

		resolvedModels.clear();
	}

	@Nullable
	public UnbakedModel resolveModel(ResourceLocation id) {
		if (modelResolverContextStack.isEmpty()) {
			modelResolverContextStack.add(new ModelResolverContext());
		}

		ModelResolverContext context = modelResolverContextStack.pop();
		context.prepare(id);

		UnbakedModel model = pluginContext.resolveModel().invoker().resolveModel(context);

		modelResolverContextStack.push(context);
		return model;
	}

	public UnbakedModel modifyModelOnLoad(UnbakedModel model, @UnknownNullability ResourceLocation resourceId, @UnknownNullability ModelResourceLocation topLevelId) {
		if (onLoadModifierContextStack.isEmpty()) {
			onLoadModifierContextStack.add(new OnLoadModifierContext());
		}

		OnLoadModifierContext context = onLoadModifierContextStack.pop();
		context.prepare(resourceId, topLevelId);

		model = pluginContext.modifyModelOnLoad().invoker().modifyModelOnLoad(model, context);

		onLoadModifierContextStack.push(context);
		return model;
	}

	public UnbakedModel modifyModelBeforeBake(UnbakedModel model, @UnknownNullability ResourceLocation resourceId, @UnknownNullability ModelResourceLocation topLevelId, Function<Material, TextureAtlasSprite> textureGetter, ModelState settings, ModelBaker baker) {
		if (beforeBakeModifierContextStack.isEmpty()) {
			beforeBakeModifierContextStack.add(new BeforeBakeModifierContext());
		}

		BeforeBakeModifierContext context = beforeBakeModifierContextStack.pop();
		context.prepare(resourceId, topLevelId, textureGetter, settings, baker);

		model = pluginContext.modifyModelBeforeBake().invoker().modifyModelBeforeBake(model, context);

		beforeBakeModifierContextStack.push(context);
		return model;
	}

	@Nullable
	public BakedModel modifyModelAfterBake(@Nullable BakedModel model, @UnknownNullability ResourceLocation resourceId, @UnknownNullability ModelResourceLocation topLevelId, UnbakedModel sourceModel, Function<Material, TextureAtlasSprite> textureGetter, ModelState settings, ModelBaker baker) {
		if (afterBakeModifierContextStack.isEmpty()) {
			afterBakeModifierContextStack.add(new AfterBakeModifierContext());
		}

		AfterBakeModifierContext context = afterBakeModifierContextStack.pop();
		context.prepare(resourceId, topLevelId, sourceModel, textureGetter, settings, baker);

		model = pluginContext.modifyModelAfterBake().invoker().modifyModelAfterBake(model, context);

		afterBakeModifierContextStack.push(context);
		return model;
	}

	private class ModelResolverContext implements ModelResolver.Context {
		private ResourceLocation id;

		private void prepare(ResourceLocation id) {
			this.id = id;
		}

		@Override
		public ResourceLocation id() {
			return id;
		}

		@Override
		public UnbakedModel getOrLoadModel(ResourceLocation id) {
			return ((ModelLoaderHooks) loader).fabric_getOrLoadModel(id);
		}

		@Override
		public ModelBakery loader() {
			return loader;
		}
	}

	private class BlockStateResolverContext implements BlockStateResolver.Context {
		private Block block;
		private final Reference2ReferenceMap<BlockState, UnbakedModel> models = new Reference2ReferenceOpenHashMap<>();

		private void prepare(Block block) {
			this.block = block;
			models.clear();
		}

		@Override
		public Block block() {
			return block;
		}

		@Override
		public void setModel(BlockState state, UnbakedModel model) {
			Objects.requireNonNull(model, "state cannot be null");
			Objects.requireNonNull(model, "model cannot be null");

			if (!state.is(block)) {
				throw new IllegalArgumentException("Attempted to set model for state " + state + " on block " + block);
			}

			if (models.putIfAbsent(state, model) != null) {
				throw new IllegalStateException("Duplicate model for state " + state + " on block " + block);
			}
		}

		@Override
		public UnbakedModel getOrLoadModel(ResourceLocation id) {
			return ((ModelLoaderHooks) loader).fabric_getOrLoadModel(id);
		}

		@Override
		public ModelBakery loader() {
			return loader;
		}
	}

	private class OnLoadModifierContext implements ModelModifier.OnLoad.Context {
		@UnknownNullability
		private ResourceLocation resourceId;
		@UnknownNullability
		private ModelResourceLocation topLevelId;

		private void prepare(@UnknownNullability ResourceLocation resourceId, @UnknownNullability ModelResourceLocation topLevelId) {
			this.resourceId = resourceId;
			this.topLevelId = topLevelId;
		}

		@Override
		@UnknownNullability("#topLevelId() != null")
		public ResourceLocation resourceId() {
			return resourceId;
		}

		@Override
		@UnknownNullability("#resourceId() != null")
		public ModelResourceLocation topLevelId() {
			return topLevelId;
		}

		@Override
		public UnbakedModel getOrLoadModel(ResourceLocation id) {
			return ((ModelLoaderHooks) loader).fabric_getOrLoadModel(id);
		}

		@Override
		public ModelBakery loader() {
			return loader;
		}
	}

	private class BeforeBakeModifierContext implements ModelModifier.BeforeBake.Context {
		@UnknownNullability
		private ResourceLocation resourceId;
		@UnknownNullability
		private ModelResourceLocation topLevelId;
		private Function<Material, TextureAtlasSprite> textureGetter;
		private ModelState settings;
		private ModelBaker baker;

		private void prepare(@UnknownNullability ResourceLocation resourceId, @UnknownNullability ModelResourceLocation topLevelId, Function<Material, TextureAtlasSprite> textureGetter, ModelState settings, ModelBaker baker) {
			this.resourceId = resourceId;
			this.topLevelId = topLevelId;
			this.textureGetter = textureGetter;
			this.settings = settings;
			this.baker = baker;
		}

		@Override
		@UnknownNullability("#topLevelId() != null")
		public ResourceLocation resourceId() {
			return resourceId;
		}

		@Override
		@UnknownNullability("#resourceId() != null")
		public ModelResourceLocation topLevelId() {
			return topLevelId;
		}

		@Override
		public Function<Material, TextureAtlasSprite> textureGetter() {
			return textureGetter;
		}

		@Override
		public ModelState settings() {
			return settings;
		}

		@Override
		public ModelBaker baker() {
			return baker;
		}

		@Override
		public ModelBakery loader() {
			return loader;
		}
	}

	private class AfterBakeModifierContext implements ModelModifier.AfterBake.Context {
		@UnknownNullability
		private ResourceLocation resourceId;
		@UnknownNullability
		private ModelResourceLocation topLevelId;
		private UnbakedModel sourceModel;
		private Function<Material, TextureAtlasSprite> textureGetter;
		private ModelState settings;
		private ModelBaker baker;

		private void prepare(@UnknownNullability ResourceLocation resourceId, @UnknownNullability ModelResourceLocation topLevelId, UnbakedModel sourceModel, Function<Material, TextureAtlasSprite> textureGetter, ModelState settings, ModelBaker baker) {
			this.resourceId = resourceId;
			this.topLevelId = topLevelId;
			this.sourceModel = sourceModel;
			this.textureGetter = textureGetter;
			this.settings = settings;
			this.baker = baker;
		}

		@Override
		@UnknownNullability("#topLevelId() != null")
		public ResourceLocation resourceId() {
			return resourceId;
		}

		@Override
		@UnknownNullability("#resourceId() != null")
		public ModelResourceLocation topLevelId() {
			return topLevelId;
		}

		@Override
		public UnbakedModel sourceModel() {
			return sourceModel;
		}

		@Override
		public Function<Material, TextureAtlasSprite> textureGetter() {
			return textureGetter;
		}

		@Override
		public ModelState settings() {
			return settings;
		}

		@Override
		public ModelBaker baker() {
			return baker;
		}

		@Override
		public ModelBakery loader() {
			return loader;
		}
	}
}
