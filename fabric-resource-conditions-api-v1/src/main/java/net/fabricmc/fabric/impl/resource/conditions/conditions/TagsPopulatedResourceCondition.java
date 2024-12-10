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

package net.fabricmc.fabric.impl.resource.conditions.conditions;

import java.util.Arrays;
import java.util.List;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.Nullable;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.fabricmc.fabric.impl.resource.conditions.DefaultResourceConditionTypes;
import net.fabricmc.fabric.impl.resource.conditions.ResourceConditionsImpl;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

public record TagsPopulatedResourceCondition(ResourceLocation registry, List<ResourceLocation> tags) implements ResourceCondition {
	// Cannot use registry-bound codec because they fail parsing if nonexistent,
	// and resource conditions themselves should not fail to parse on condition failure
	public static final MapCodec<TagsPopulatedResourceCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			ResourceLocation.CODEC.fieldOf("registry").orElse(Registries.ITEM.location()).forGetter(TagsPopulatedResourceCondition::registry),
			ResourceLocation.CODEC.listOf().fieldOf("values").forGetter(TagsPopulatedResourceCondition::tags)
	).apply(instance, TagsPopulatedResourceCondition::new));

	@SafeVarargs
	public <T> TagsPopulatedResourceCondition(ResourceLocation registry, TagKey<T>... tags) {
		this(registry, Arrays.stream(tags).map(TagKey::location).toList());
	}

	@SafeVarargs
	public <T> TagsPopulatedResourceCondition(TagKey<T>... tags) {
		this(tags[0].registry().getValue(), Arrays.stream(tags).map(TagKey::location).toList());
	}

	@Override
	public ResourceConditionType<?> getType() {
		return DefaultResourceConditionTypes.TAGS_POPULATED;
	}

	@Override
	public boolean test(@Nullable RegistryOps.RegistryInfoLookup registryInfo) {
		return ResourceConditionsImpl.tagsPopulated(this.registry(), this.tags());
	}
}
