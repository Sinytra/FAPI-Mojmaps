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

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;

public class CustomDataIngredient implements CustomIngredient {
	public static final CustomIngredientSerializer<CustomDataIngredient> SERIALIZER = new Serializer();
	private final Ingredient base;
	private final CompoundTag nbt;

	public CustomDataIngredient(Ingredient base, CompoundTag nbt) {
		if (nbt == null || nbt.isEmpty()) throw new IllegalArgumentException("NBT cannot be null; use components ingredient for strict matching");

		this.base = base;
		this.nbt = nbt;
	}

	@Override
	public boolean test(ItemStack stack) {
		if (!base.test(stack)) return false;

		CustomData nbt = stack.get(DataComponents.CUSTOM_DATA);

		return nbt != null && nbt.matchedBy(this.nbt);
	}

	@Override
	public List<ItemStack> getMatchingStacks() {
		List<ItemStack> stacks = new ArrayList<>(List.of(base.getItems()));
		stacks.replaceAll(stack -> {
			ItemStack copy = stack.copy();
			copy.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, existingNbt -> CustomData.of(existingNbt.copyTag().merge(this.nbt)));
			return copy;
		});
		stacks.removeIf(stack -> !base.test(stack));
		return stacks;
	}

	@Override
	public boolean requiresTesting() {
		return true;
	}

	@Override
	public CustomIngredientSerializer<?> getSerializer() {
		return SERIALIZER;
	}

	private Ingredient getBase() {
		return base;
	}

	private CompoundTag getNbt() {
		return nbt;
	}

	private static class Serializer implements CustomIngredientSerializer<CustomDataIngredient> {
		private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("fabric", "custom_data");

		private static final MapCodec<CustomDataIngredient> ALLOW_EMPTY_CODEC = createCodec(Ingredient.CODEC);
		private static final MapCodec<CustomDataIngredient> DISALLOW_EMPTY_CODEC = createCodec(Ingredient.CODEC_NONEMPTY);

		private static final StreamCodec<RegistryFriendlyByteBuf, CustomDataIngredient> PACKET_CODEC = StreamCodec.composite(
				Ingredient.CONTENTS_STREAM_CODEC, CustomDataIngredient::getBase,
				ByteBufCodecs.COMPOUND_TAG, CustomDataIngredient::getNbt,
				CustomDataIngredient::new
		);

		private static MapCodec<CustomDataIngredient> createCodec(Codec<Ingredient> ingredientCodec) {
			return RecordCodecBuilder.mapCodec(instance ->
					instance.group(
							ingredientCodec.fieldOf("base").forGetter(CustomDataIngredient::getBase),
							TagParser.LENIENT_CODEC.fieldOf("nbt").forGetter(CustomDataIngredient::getNbt)
					).apply(instance, CustomDataIngredient::new)
			);
		}

		@Override
		public ResourceLocation getIdentifier() {
			return ID;
		}

		@Override
		public MapCodec<CustomDataIngredient> getCodec(boolean allowEmpty) {
			return allowEmpty ? ALLOW_EMPTY_CODEC : DISALLOW_EMPTY_CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, CustomDataIngredient> getPacketCodec() {
			return PACKET_CODEC;
		}
	}
}
