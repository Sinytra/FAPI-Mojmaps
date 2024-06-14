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

package net.fabricmc.fabric.test.resource.conditions;

import java.util.stream.Collectors;

import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.fabricmc.fabric.impl.resource.conditions.ResourceConditionsImpl;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlag;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class DefaultResourceConditionsTest {
	private static final String TESTMOD_ID = "fabric-resource-conditions-api-v1-testmod";
	private static final String API_MOD_ID = "fabric-resource-conditions-api-v1";
	private static final String UNKNOWN_MOD_ID = "fabric-tiny-potato-api-v1";
	private static final ResourceKey<? extends Registry<Object>> UNKNOWN_REGISTRY_KEY = ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(TESTMOD_ID, "unknown_registry"));
	private static final ResourceLocation UNKNOWN_ENTRY_ID = ResourceLocation.fromNamespaceAndPath(TESTMOD_ID, "tiny_potato");

	private void expectCondition(GameTestHelper context, String name, ResourceCondition condition, boolean expected) {
		HolderLookup.Provider registryLookup = context.getLevel().registryAccess();
		boolean actual = condition.test(registryLookup);

		if (actual != expected) {
			throw new AssertionError("Test \"%s\" for condition %s failed; expected %s, got %s".formatted(name, condition.getType().id(), expected, actual));
		}

		// Test serialization
		ResourceCondition.CODEC.encodeStart(JsonOps.INSTANCE, condition).getOrThrow(message -> new AssertionError("Could not serialize \"%s\": %s".formatted(name, message)));
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void logics(GameTestHelper context) {
		ResourceCondition alwaysTrue = ResourceConditions.alwaysTrue();
		ResourceCondition alwaysFalse = ResourceConditions.not(alwaysTrue);
		ResourceCondition trueAndTrue = ResourceConditions.and(alwaysTrue, alwaysTrue);
		ResourceCondition trueAndFalse = ResourceConditions.and(alwaysTrue, alwaysFalse);
		ResourceCondition emptyAnd = ResourceConditions.and();
		ResourceCondition trueOrFalse = ResourceConditions.or(alwaysTrue, alwaysFalse);
		ResourceCondition falseOrFalse = ResourceConditions.or(alwaysFalse, alwaysFalse);
		ResourceCondition emptyOr = ResourceConditions.or();

		expectCondition(context, "always true", alwaysTrue, true);
		expectCondition(context, "always false", alwaysFalse, false);
		expectCondition(context, "true and true", trueAndTrue, true);
		expectCondition(context, "true and false", trueAndFalse, false);
		expectCondition(context, "vacuous truth", emptyAnd, true);
		expectCondition(context, "true or false", trueOrFalse, true);
		expectCondition(context, "false or false", falseOrFalse, false);
		expectCondition(context, "empty OR is always false", emptyOr, false);

		context.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void allModsLoaded(GameTestHelper context) {
		ResourceCondition testmod = ResourceConditions.allModsLoaded(TESTMOD_ID);
		ResourceCondition testmodAndApi = ResourceConditions.allModsLoaded(TESTMOD_ID, API_MOD_ID);
		ResourceCondition unknownMod = ResourceConditions.allModsLoaded(UNKNOWN_MOD_ID);
		ResourceCondition unknownAndTestmod = ResourceConditions.allModsLoaded(UNKNOWN_MOD_ID, TESTMOD_ID);
		ResourceCondition noMod = ResourceConditions.allModsLoaded();

		expectCondition(context, "one loaded mod", testmod, true);
		expectCondition(context, "two loaded mods", testmodAndApi, true);
		expectCondition(context, "one unloaded mod", unknownMod, false);
		expectCondition(context, "both loaded and unloaded mods", unknownAndTestmod, false);
		expectCondition(context, "no mod", noMod, true);

		context.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void anyModsLoaded(GameTestHelper context) {
		ResourceCondition testmod = ResourceConditions.anyModsLoaded(TESTMOD_ID);
		ResourceCondition testmodAndApi = ResourceConditions.anyModsLoaded(TESTMOD_ID, API_MOD_ID);
		ResourceCondition unknownMod = ResourceConditions.anyModsLoaded(UNKNOWN_MOD_ID);
		ResourceCondition unknownAndTestmod = ResourceConditions.anyModsLoaded(UNKNOWN_MOD_ID, TESTMOD_ID);
		ResourceCondition noMod = ResourceConditions.anyModsLoaded();

		expectCondition(context, "one loaded mod", testmod, true);
		expectCondition(context, "two loaded mods", testmodAndApi, true);
		expectCondition(context, "one unloaded mod", unknownMod, false);
		expectCondition(context, "both loaded and unloaded mods", unknownAndTestmod, true);
		expectCondition(context, "no mod", noMod, false);

		context.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void featuresEnabled(GameTestHelper context) {
		ResourceCondition vanilla = ResourceConditions.featuresEnabled(FeatureFlags.VANILLA);
		// Reminder: GameTest enables all features by default
		ResourceCondition vanillaAndBundle = ResourceConditions.featuresEnabled(FeatureFlags.VANILLA, FeatureFlags.BUNDLE);
		ResourceLocation unknownId = ResourceLocation.fromNamespaceAndPath(TESTMOD_ID, "unknown_feature_to_test_condition");
		ResourceCondition unknown = ResourceConditions.featuresEnabled(unknownId);
		// Passing an array to avoid type ambiguity
		ResourceCondition empty = ResourceConditions.featuresEnabled(new FeatureFlag[]{});

		expectCondition(context, "vanilla only", vanilla, true);
		expectCondition(context, "vanilla and bundle", vanillaAndBundle, true);
		expectCondition(context, "unknown feature ID", unknown, false);
		expectCondition(context, "no feature", empty, true);

		context.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void registryContains(GameTestHelper context) {
		// Static registry
		ResourceKey<Block> dirtKey = BuiltInRegistries.BLOCK.getResourceKey(Blocks.DIRT).orElseThrow();
		ResourceCondition dirt = ResourceConditions.registryContains(dirtKey);
		ResourceCondition dirtAndUnknownBlock = ResourceConditions.registryContains(dirtKey, ResourceKey.create(Registries.BLOCK, UNKNOWN_ENTRY_ID));
		ResourceCondition emptyBlock = ResourceConditions.registryContains(Registries.BLOCK, new ResourceLocation[]{});
		ResourceCondition unknownRegistry = ResourceConditions.registryContains(UNKNOWN_REGISTRY_KEY, UNKNOWN_ENTRY_ID);
		ResourceCondition emptyUnknown = ResourceConditions.registryContains(UNKNOWN_REGISTRY_KEY, new ResourceLocation[]{});

		expectCondition(context, "dirt", dirt, true);
		expectCondition(context, "dirt and unknown block", dirtAndUnknownBlock, false);
		expectCondition(context, "block registry, empty check", emptyBlock, true);
		expectCondition(context, "unknown registry, non-empty", unknownRegistry, false);
		expectCondition(context, "unknown registry, empty", emptyUnknown, true);

		// Dynamic registry (in vitro; separate testmod needs to determine if this actually functions while loading)
		ResourceCondition plains = ResourceConditions.registryContains(Biomes.PLAINS);
		ResourceCondition unknownBiome = ResourceConditions.registryContains(ResourceKey.create(Registries.BIOME, UNKNOWN_ENTRY_ID));
		ResourceCondition emptyDynamic = ResourceConditions.registryContains(Registries.BIOME, new ResourceLocation[]{});

		expectCondition(context, "plains", plains, true);
		expectCondition(context, "unknown biome", unknownBiome, false);
		expectCondition(context, "biome registry, empty check", emptyDynamic, true);

		context.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void tagsPopulated(GameTestHelper context) {
		// We need to set the tags ourselves as it is cleared outside the resource loading context.
		ResourceConditionsImpl.LOADED_TAGS.set(
				context.getLevel().registryAccess().registries().collect(Collectors.toMap(
						RegistryAccess.RegistryEntry::key,
						e -> e.value().getTagNames().map(TagKey::location).collect(Collectors.toUnmodifiableSet())
				))
		);

		// Static registry
		ResourceCondition dirt = ResourceConditions.tagsPopulated(Registries.BLOCK, BlockTags.DIRT);
		ResourceCondition dirtAndUnknownBlock = ResourceConditions.tagsPopulated(Registries.BLOCK, BlockTags.DIRT, TagKey.create(Registries.BLOCK, UNKNOWN_ENTRY_ID));
		ResourceCondition emptyBlock = ResourceConditions.tagsPopulated(Registries.BLOCK);
		ResourceCondition unknownRegistry = ResourceConditions.tagsPopulated(UNKNOWN_REGISTRY_KEY, TagKey.create(UNKNOWN_REGISTRY_KEY, UNKNOWN_ENTRY_ID));
		ResourceCondition emptyUnknown = ResourceConditions.tagsPopulated(UNKNOWN_REGISTRY_KEY);

		expectCondition(context, "dirt tag", dirt, true);
		expectCondition(context, "dirt tag and unknown tag", dirtAndUnknownBlock, false);
		expectCondition(context, "block registry, empty tag checks", emptyBlock, true);
		expectCondition(context, "unknown registry, non-empty tag checks", unknownRegistry, false);
		expectCondition(context, "unknown registry, empty tag checks", emptyUnknown, true);

		// Dynamic registry (in vitro; separate testmod needs to determine if this actually functions while loading)
		ResourceCondition forest = ResourceConditions.tagsPopulated(Registries.BIOME, BiomeTags.IS_FOREST);
		ResourceCondition unknownBiome = ResourceConditions.tagsPopulated(Registries.BIOME, TagKey.create(Registries.BIOME, UNKNOWN_ENTRY_ID));
		ResourceCondition emptyDynamic = ResourceConditions.tagsPopulated(Registries.BIOME);

		expectCondition(context, "forest tag", forest, true);
		expectCondition(context, "unknown biome tag", unknownBiome, false);
		expectCondition(context, "biome registry, empty tag check", emptyDynamic, true);

		context.succeed();
	}
}
