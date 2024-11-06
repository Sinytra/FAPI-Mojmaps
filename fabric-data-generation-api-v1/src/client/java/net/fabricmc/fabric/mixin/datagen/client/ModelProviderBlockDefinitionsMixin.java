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

import java.util.function.Predicate;
import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.core.Holder;
import net.minecraft.data.client.ModelProvider;
import net.minecraft.world.level.block.Block;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.impl.datagen.client.FabricModelProviderDefinitions;

@Mixin(ModelProvider.class_10406.class)
public class ModelProviderBlockDefinitionsMixin implements FabricModelProviderDefinitions {
	@Unique
	private FabricDataOutput fabricDataOutput;

	@Override
	public void setFabricDataOutput(FabricDataOutput fabricDataOutput) {
		this.fabricDataOutput = fabricDataOutput;
	}

	// Target the first .filter() call, to filter out blocks that are not from the mod we are processing.
	@Redirect(method = "method_65462", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream;", ordinal = 0, remap = false))
	private Stream<Holder.Reference<Block>> filterBlocksForProcessingMod(Stream<Holder.Reference<Block>> instance, Predicate<Holder.Reference<Block>> predicate) {
		return instance.filter((block) -> {
			if (fabricDataOutput != null) {
				if (!fabricDataOutput.isStrictValidationEnabled()) {
					return false;
				}

				if (!block.key().location().getNamespace().equals(fabricDataOutput.getModId())) {
					// Skip over blocks that are not from the mod we are processing.
					return false;
				}
			}

			return predicate.test(block);
		});
	}
}
