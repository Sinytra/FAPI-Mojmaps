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

package net.fabricmc.fabric.mixin.datagen;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import net.fabricmc.fabric.impl.datagen.FabricTagBuilder;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.TagBuilder;

@Mixin(TagsProvider.class)
public class TagProviderMixin {
	@ModifyArg(method = "lambda$run$5", at = @At(value = "INVOKE", target = "Lnet/minecraft/tags/TagFile;<init>(Ljava/util/List;Z)V"), index = 1)
	private boolean addReplaced(boolean replaced, @Local TagBuilder tagBuilder) {
		if (tagBuilder instanceof FabricTagBuilder fabricTagBuilder) {
			return fabricTagBuilder.fabric_isReplaced();
		}

		return replaced;
	}
}
