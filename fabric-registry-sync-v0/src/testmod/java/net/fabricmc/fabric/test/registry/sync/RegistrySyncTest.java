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

package net.fabricmc.fabric.test.registry.sync;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.registry.DynamicRegistrySetupCallback;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.fabricmc.fabric.api.event.registry.RegistryAttributeHolder;
import net.fabricmc.fabric.impl.registry.sync.RegistrySyncManager;
import net.fabricmc.fabric.impl.registry.sync.RemapException;
import net.minecraft.commands.Commands;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class RegistrySyncTest implements ModInitializer {
	private static final Logger LOGGER = LogUtils.getLogger();

	/**
	 * These are system property's as it allows for easier testing with different run configurations.
	 */
	public static final boolean REGISTER_BLOCKS = Boolean.parseBoolean(System.getProperty("fabric.registry.sync.test.register.blocks", "true"));
	public static final boolean REGISTER_ITEMS = Boolean.parseBoolean(System.getProperty("fabric.registry.sync.test.register.items", "true"));

	// Store a list of Registries used with PacketCodecs.registry, and then check that they are marked as synced when the server starts.
	// We check them later as they may be used before the registry attributes are assigned.
	private static boolean hasCheckedEarlyRegistries = false;
	private static final List<ResourceKey<? extends Registry<?>>> sycnedRegistriesToCheck = new ArrayList<>();

	@Override
	public void onInitialize() {
		if (REGISTER_BLOCKS) {
			// For checking raw id bulk in direct registry packet, make registry_sync namespace have two bulks.
			registerBlocks("registry_sync", 5, 0);
			registerBlocks("registry_sync2", 50, 0);
			registerBlocks("registry_sync", 2, 5);

			Validate.isTrue(RegistryAttributeHolder.get(BuiltInRegistries.BLOCK).hasAttribute(RegistryAttribute.MODDED), "Modded block was registered but registry not marked as modded");

			if (REGISTER_ITEMS) {
				Validate.isTrue(RegistryAttributeHolder.get(BuiltInRegistries.ITEM).hasAttribute(RegistryAttribute.MODDED), "Modded item was registered but registry not marked as modded");
			}
		}

		ResourceKey<Registry<String>> fabricRegistryKey = ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath("registry_sync", "fabric_registry"));
		MappedRegistry<String> fabricRegistry = FabricRegistryBuilder.createSimple(fabricRegistryKey)
				.attribute(RegistryAttribute.SYNCED)
				.buildAndRegister();

		Registry.register(fabricRegistry, ResourceLocation.fromNamespaceAndPath("registry_sync", "test"), "test");

		Validate.isTrue(BuiltInRegistries.REGISTRY.keySet().contains(ResourceLocation.fromNamespaceAndPath("registry_sync", "fabric_registry")));

		Validate.isTrue(RegistryAttributeHolder.get(fabricRegistry).hasAttribute(RegistryAttribute.MODDED));
		Validate.isTrue(RegistryAttributeHolder.get(fabricRegistry).hasAttribute(RegistryAttribute.SYNCED));

		final AtomicBoolean setupCalled = new AtomicBoolean(false);

		DynamicRegistrySetupCallback.EVENT.register(registryManager -> {
			setupCalled.set(true);
			registryManager.registerEntryAdded(Registries.BIOME, (rawId, id, object) -> {
				LOGGER.info("Biome added: {}", id);
			});
		});

		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			hasCheckedEarlyRegistries = true;
			sycnedRegistriesToCheck.forEach(RegistrySyncTest::checkSyncedRegistry);

			if (!setupCalled.get()) {
				throw new IllegalStateException("DRM setup was not called before startup!");
			}
		});

		// Vanilla status effects don't have an entry for the int id 0, test we can handle this.
		RegistryAttributeHolder.get(BuiltInRegistries.MOB_EFFECT).addAttribute(RegistryAttribute.MODDED);

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(Commands.literal("remote_remap_error_test").executes(context -> {
					Map<ResourceLocation, Object2IntMap<ResourceLocation>> registryData = Map.of(
							Registries.BLOCK.location(), createFakeRegistryEntries(),
							Registries.ITEM.location(), createFakeRegistryEntries()
					);

					try {
						RegistrySyncManager.checkRemoteRemap(registryData);
					} catch (RemapException e) {
						final ServerPlayer player = context.getSource().getPlayer();

						if (player != null) {
							player.connection.disconnect(Objects.requireNonNull(e.getText()));
						}

						return 1;
					}

					throw new IllegalStateException();
				})));
	}

	public static void checkSyncedRegistry(ResourceKey<? extends Registry<?>> registry) {
		if (!BuiltInRegistries.REGISTRY.containsKey(registry.location())) {
			// Skip dynamic registries, as there are always synced.
			return;
		}

		if (!hasCheckedEarlyRegistries) {
			sycnedRegistriesToCheck.add(registry);
			return;
		}

		if (!RegistryAttributeHolder.get(registry).hasAttribute(RegistryAttribute.SYNCED)) {
			throw new IllegalStateException("Registry " + registry.location() + " is not marked as SYNCED!");
		}
	}

	private static void registerBlocks(String namespace, int amount, int startingId) {
		for (int i = 0; i < amount; i++) {
			Block block = new Block(BlockBehaviour.Properties.of());
			Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(namespace, "block_" + (i + startingId)), block);

			if (REGISTER_ITEMS) {
				BlockItem blockItem = new BlockItem(block, new Item.Properties());
				Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(namespace, "block_" + (i + startingId)), blockItem);
			}
		}
	}

	private static Object2IntMap<ResourceLocation> createFakeRegistryEntries() {
		Object2IntMap<ResourceLocation> map = new Object2IntOpenHashMap<>();

		for (int i = 0; i < 12; i++) {
			map.put(ResourceLocation.fromNamespaceAndPath("mod_" + i, "entry"), 0);
		}

		return map;
	}
}
