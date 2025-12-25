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

package net.fabricmc.fabric.impl.recipe.ingredient.builtin;

import java.util.List;
import java.util.function.Function;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Base class for ALL and ANY ingredients.
 */
abstract class CombinedIngredient implements CustomIngredient {
	protected final List<Ingredient> ingredients;

	protected CombinedIngredient(List<Ingredient> ingredients) {
		if (ingredients.isEmpty()) {
			throw new IllegalArgumentException("ALL or ANY ingredient must have at least one sub-ingredient");
		}

		this.ingredients = ingredients;
	}

	@Override
	public boolean requiresTesting() {
		for (Ingredient ingredient : ingredients) {
			if (ingredient.requiresTesting()) {
				return true;
			}
		}

		return false;
	}

	List<Ingredient> getIngredients() {
		return ingredients;
	}

	static class Serializer<I extends CombinedIngredient> implements CustomIngredientSerializer<I> {
		private final ResourceLocation identifier;
		private final MapCodec<I> allowEmptyCodec;
		private final MapCodec<I> disallowEmptyCodec;
		private final StreamCodec<RegistryFriendlyByteBuf, I> packetCodec;

		Serializer(ResourceLocation identifier, Function<List<Ingredient>, I> factory, MapCodec<I> allowEmptyCodec, MapCodec<I> disallowEmptyCodec) {
			this.identifier = identifier;
			this.allowEmptyCodec = allowEmptyCodec;
			this.disallowEmptyCodec = disallowEmptyCodec;
			this.packetCodec = Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list())
					.map(factory, I::getIngredients);
		}

		@Override
		public ResourceLocation getIdentifier() {
			return identifier;
		}

		@Override
		public MapCodec<I> getCodec(boolean allowEmpty) {
			return allowEmpty ? allowEmptyCodec : disallowEmptyCodec;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, I> getPacketCodec() {
			return this.packetCodec;
		}
	}
}
