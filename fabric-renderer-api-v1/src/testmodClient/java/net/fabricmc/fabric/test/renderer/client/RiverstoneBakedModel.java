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

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;
import net.fabricmc.fabric.api.blockview.v2.FabricBlockView;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

public class RiverstoneBakedModel implements BakedModel {
	private final BakedModel regularModel;
	private final BakedModel riverModel;

	public RiverstoneBakedModel(BakedModel regularModel, BakedModel riverModel) {
		this.regularModel = regularModel;
		this.riverModel = riverModel;
	}

	@Override
	public boolean isVanillaAdapter() {
		return false;
	}

	@Override
	public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context) {
		if (((FabricBlockView) blockView).hasBiomes() && ((FabricBlockView) blockView).getBiomeFabric(pos).is(BiomeTags.IS_RIVER)) {
			riverModel.emitBlockQuads(blockView, state, pos, randomSupplier, context);
		} else {
			regularModel.emitBlockQuads(blockView, state, pos, randomSupplier, context);
		}
	}

	@Override
	public void emitItemQuads(ItemStack stack, Supplier<RandomSource> randomSupplier, RenderContext context) {
		regularModel.emitItemQuads(stack, randomSupplier, context);
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, RandomSource random) {
		return Collections.emptyList();
	}

	@Override
	public boolean useAmbientOcclusion() {
		return true;
	}

	@Override
	public boolean isGui3d() {
		return false;
	}

	@Override
	public boolean usesBlockLight() {
		return true;
	}

	@Override
	public boolean isCustomRenderer() {
		return false;
	}

	@Override
	public TextureAtlasSprite getParticleIcon() {
		return regularModel.getParticleIcon();
	}

	@Override
	public ItemTransforms getTransforms() {
		return ModelHelper.MODEL_TRANSFORM_BLOCK;
	}

	@Override
	public ItemOverrides getOverrides() {
		return ItemOverrides.EMPTY;
	}
}
