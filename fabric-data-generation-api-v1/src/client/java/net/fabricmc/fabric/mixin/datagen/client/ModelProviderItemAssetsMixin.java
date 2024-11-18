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

package net.fabricmc.fabric.mixin.datagen.client;

import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.impl.datagen.client.FabricModelProviderDefinitions;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

@Mixin(ModelProvider.ItemInfoCollector.class)
public class ModelProviderItemAssetsMixin implements FabricModelProviderDefinitions {
	@Shadow
	@Final
	private Map<Item, ClientItem> ITEM_ASSETS;
	@Unique
	private FabricDataOutput fabricDataOutput;

	@Override
	public void setFabricDataOutput(FabricDataOutput fabricDataOutput) {
		this.fabricDataOutput = fabricDataOutput;
	}

	@WrapOperation(method = "lambda$finalizeAndValidate$0", at = @At(value = "INVOKE", target = "Ljava/util/Map;containsKey(Ljava/lang/Object;)Z", ordinal = 1, remap = false))
	private boolean filterItemsForProcessingMod(Map<Item, ClientItem> map, Object o, Operation<Boolean> original) {
		BlockItem blockItem = (BlockItem) o;

		if (fabricDataOutput != null) {
			// Only generate the item model if the block state json was registered
			if (ITEM_ASSETS.containsKey(blockItem)) {
				return true;
			}

			if (!BuiltInRegistries.ITEM.getKey(blockItem).getNamespace().equals(fabricDataOutput.getModId())) {
				// Skip over items that are not from the mod we are processing.
				return true;
			}
		}

		return original.call(map, blockItem);
	}

	@Redirect(method = "finalizeAndValidate", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream;", ordinal = 0, remap = false))
	private Stream<Holder.Reference<Item>> filterItemsForProcessingMod(Stream<Holder.Reference<Item>> instance, Predicate<Holder.Reference<Item>> predicate) {
		return instance.filter((item) -> {
			if (fabricDataOutput != null) {
				if (!fabricDataOutput.isStrictValidationEnabled()) {
					return false;
				}

				if (!item.key().location().getNamespace().equals(fabricDataOutput.getModId())) {
					// Skip over items that are not from the mod we are processing.
					return false;
				}
			}

			return predicate.test(item);
		});
	}
}
