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

package net.fabricmc.fabric.impl.resource.conditions;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;

public final class ResourceConditionsImpl implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("Fabric Resource Conditions");
	public static FeatureFlagSet currentFeatures = null;

	@Override
	public void onInitialize() {
		ResourceConditions.register(DefaultResourceConditionTypes.TRUE);
		ResourceConditions.register(DefaultResourceConditionTypes.NOT);
		ResourceConditions.register(DefaultResourceConditionTypes.AND);
		ResourceConditions.register(DefaultResourceConditionTypes.OR);
		ResourceConditions.register(DefaultResourceConditionTypes.ALL_MODS_LOADED);
		ResourceConditions.register(DefaultResourceConditionTypes.ANY_MODS_LOADED);
		ResourceConditions.register(DefaultResourceConditionTypes.TAGS_POPULATED);
		ResourceConditions.register(DefaultResourceConditionTypes.FEATURES_ENABLED);
		ResourceConditions.register(DefaultResourceConditionTypes.REGISTRY_CONTAINS);
	}

	public static boolean applyResourceConditions(JsonObject obj, String dataType, ResourceLocation key, @Nullable RegistryOps.RegistryInfoLookup registryInfo) {
		boolean debugLogEnabled = ResourceConditionsImpl.LOGGER.isDebugEnabled();

		if (obj.has(ResourceConditions.CONDITIONS_KEY)) {
			DataResult<ResourceCondition> conditions = ResourceCondition.CONDITION_CODEC.parse(JsonOps.INSTANCE, obj.get(ResourceConditions.CONDITIONS_KEY));

			if (conditions.isSuccess()) {
				boolean matched = conditions.getOrThrow().test(registryInfo);

				if (debugLogEnabled) {
					String verdict = matched ? "Allowed" : "Rejected";
					ResourceConditionsImpl.LOGGER.debug("{} resource of type {} with id {}", verdict, dataType, key);
				}

				return matched;
			} else {
				ResourceConditionsImpl.LOGGER.error("Failed to parse resource conditions for file of type {} with id {}, skipping: {}", dataType, key, conditions.error().get().message());
			}
		}

		return true;
	}

	// Condition implementations

	public static boolean conditionsMet(List<ResourceCondition> conditions, @Nullable RegistryOps.RegistryInfoLookup registryInfo, boolean and) {
		for (ResourceCondition condition : conditions) {
			if (condition.test(registryInfo) != and) {
				return !and;
			}
		}

		return and;
	}

	public static boolean modsLoaded(List<String> modIds, boolean and) {
		for (String modId : modIds) {
			if (FabricLoader.getInstance().isModLoaded(modId) != and) {
				return !and;
			}
		}

		return and;
	}

	/**
	 * Stores the tags deserialized before they are bound, to use them in the tags_populated conditions.
	 * If the resource reload fails, the thread local is not cleared and:
	 * - the map will remain in memory until the next reload;
	 * - any call to {@link #tagsPopulated} will check the tags from the failed reload instead of failing directly.
	 * This is probably acceptable.
	 */
	public static final AtomicReference<Map<ResourceKey<?>, Set<ResourceLocation>>> LOADED_TAGS = new AtomicReference<>();

	public static void setTags(List<Registry.PendingTags<?>> tags) {
		Map<ResourceKey<?>, Set<ResourceLocation>> tagMap = new IdentityHashMap<>();

		for (Registry.PendingTags<?> registryTags : tags) {
			tagMap.put(registryTags.key(), registryTags.lookup().listTagIds().map(TagKey::location).collect(Collectors.toSet()));
		}

		if (LOADED_TAGS.getAndSet(tagMap) != null) {
			throw new IllegalStateException("Tags already captured, this should not happen");
		}
	}

	// Cannot use registry because tags are not loaded to the registry at this stage yet.
	public static boolean tagsPopulated(ResourceLocation registryId, List<ResourceLocation> tags) {
		Map<ResourceKey<?>, Set<ResourceLocation>> tagMap = LOADED_TAGS.get();

		if (tagMap == null) {
			LOGGER.warn("Can't retrieve registry {}, failing tags_populated resource condition check", registryId);
			return false;
		}

		Set<ResourceLocation> tagSet = tagMap.get(ResourceKey.createRegistryKey(registryId));

		if (tagSet == null) {
			return tags.isEmpty();
		} else {
			return tagSet.containsAll(tags);
		}
	}

	public static boolean featuresEnabled(Collection<ResourceLocation> features) {
		MutableBoolean foundUnknown = new MutableBoolean();
		FeatureFlagSet set = FeatureFlags.REGISTRY.fromNames(features, (id) -> {
			LOGGER.info("Found unknown feature {}, treating it as failure", id);
			foundUnknown.setTrue();
		});

		if (foundUnknown.booleanValue()) {
			return false;
		}

		if (currentFeatures == null) {
			LOGGER.warn("Can't retrieve current features, failing features_enabled resource condition check.");
			return false;
		}

		return set.isSubsetOf(currentFeatures);
	}

	public static boolean registryContains(@Nullable RegistryOps.RegistryInfoLookup registryInfo, ResourceLocation registryId, List<ResourceLocation> entries) {
		ResourceKey<? extends Registry<Object>> registryKey = ResourceKey.createRegistryKey(registryId);

		if (registryInfo == null) {
			LOGGER.warn("Can't retrieve registry {}, failing registry_contains resource condition check", registryId);
			return false;
		}

		Optional<RegistryOps.RegistryInfo<Object>> wrapper = registryInfo.lookup(registryKey);

		if (wrapper.isPresent()) {
			for (ResourceLocation id : entries) {
				if (wrapper.get().getter().get(ResourceKey.create(registryKey, id)).isEmpty()) {
					return false;
				}
			}

			return true;
		} else {
			return entries.isEmpty();
		}
	}
}
