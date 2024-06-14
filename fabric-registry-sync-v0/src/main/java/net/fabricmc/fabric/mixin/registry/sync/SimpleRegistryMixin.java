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

package net.fabricmc.fabric.mixin.registry.sync;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.fabricmc.fabric.api.event.registry.RegistryAttributeHolder;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.fabricmc.fabric.api.event.registry.RegistryIdRemapCallback;
import net.fabricmc.fabric.impl.registry.sync.ListenableRegistry;
import net.fabricmc.fabric.impl.registry.sync.RegistrySyncManager;
import net.fabricmc.fabric.impl.registry.sync.RemapException;
import net.fabricmc.fabric.impl.registry.sync.RemapStateImpl;
import net.fabricmc.fabric.impl.registry.sync.RemappableRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

@Mixin(MappedRegistry.class)
public abstract class SimpleRegistryMixin<T> implements WritableRegistry<T>, RemappableRegistry, ListenableRegistry<T> {
	// Namespaces used by the vanilla game. "brigadier" is used by command argument type registry.
	// While Realms use "realms" namespace, it is irrelevant for Registry Sync.
	@Unique
	private static final Set<String> VANILLA_NAMESPACES = Set.of("minecraft", "brigadier");

	@Shadow
	@Final
	private ObjectList<Holder.Reference<T>> byId;
	@Shadow
	@Final
	private Reference2IntMap<T> toId;
	@Shadow
	@Final
	private Map<ResourceLocation, Holder.Reference<T>> byLocation;
	@Shadow
	@Final
	private Map<ResourceKey<T>, Holder.Reference<T>> byKey;

	@Shadow
	public abstract Optional<ResourceKey<T>> getResourceKey(T entry);

	@Shadow
	public abstract @Nullable T get(@Nullable ResourceLocation id);

	@Shadow
	public abstract ResourceKey<? extends Registry<T>> key();

	@Unique
	private static final Logger FABRIC_LOGGER = LoggerFactory.getLogger(SimpleRegistryMixin.class);

	@Unique
	private Event<RegistryEntryAddedCallback<T>> fabric_addObjectEvent;

	@Unique
	private Event<RegistryIdRemapCallback<T>> fabric_postRemapEvent;

	@Unique
	private Object2IntMap<ResourceLocation> fabric_prevIndexedEntries;
	@Unique
	private BiMap<ResourceLocation, Holder.Reference<T>> fabric_prevEntries;

	@Override
	public Event<RegistryEntryAddedCallback<T>> fabric_getAddObjectEvent() {
		return fabric_addObjectEvent;
	}

	@Override
	public Event<RegistryIdRemapCallback<T>> fabric_getRemapEvent() {
		return fabric_postRemapEvent;
	}

	@Inject(method = "<init>(Lnet/minecraft/resources/ResourceKey;Lcom/mojang/serialization/Lifecycle;Z)V", at = @At("RETURN"))
	private void init(ResourceKey key, Lifecycle lifecycle, boolean intrusive, CallbackInfo ci) {
		fabric_addObjectEvent = EventFactory.createArrayBacked(RegistryEntryAddedCallback.class,
			(callbacks) -> (rawId, id, object) -> {
				for (RegistryEntryAddedCallback<T> callback : callbacks) {
					callback.onEntryAdded(rawId, id, object);
				}
			}
		);
		fabric_postRemapEvent = EventFactory.createArrayBacked(RegistryIdRemapCallback.class,
			(callbacks) -> (a) -> {
				for (RegistryIdRemapCallback<T> callback : callbacks) {
					callback.onRemap(a);
				}
			}
		);
	}

	@Unique
	private void onChange(ResourceKey<T> registryKey) {
		if (RegistrySyncManager.postBootstrap || !VANILLA_NAMESPACES.contains(registryKey.location().getNamespace())) {
			RegistryAttributeHolder holder = RegistryAttributeHolder.get(key());

			if (!holder.hasAttribute(RegistryAttribute.MODDED)) {
				ResourceLocation id = key().location();
				FABRIC_LOGGER.debug("Registry {} has been marked as modded, registry entry {} was changed", id, registryKey.location());
				RegistryAttributeHolder.get(key()).addAttribute(RegistryAttribute.MODDED);
			}
		}
	}

	@Inject(method = "register(Lnet/minecraft/resources/ResourceKey;Ljava/lang/Object;Lnet/minecraft/core/RegistrationInfo;)Lnet/minecraft/core/Holder$Reference;", at = @At("RETURN"))
	private void set(ResourceKey<T> key, T entry, RegistrationInfo arg, CallbackInfoReturnable<Holder.Reference<T>> info) {
		// We need to restore the 1.19 behavior of binding the value to references immediately.
		// Unfrozen registries cannot be interacted with otherwise, because the references would throw when
		// trying to access their values.
		info.getReturnValue().bindValue(entry);

		fabric_addObjectEvent.invoker().onEntryAdded(toId.getInt(entry), key.location(), entry);
		onChange(key);
	}

	@Override
	public void remap(String name, Object2IntMap<ResourceLocation> remoteIndexedEntries, RemapMode mode) throws RemapException {
		// Throw on invalid conditions.
		switch (mode) {
		case AUTHORITATIVE:
			break;
		case REMOTE: {
			List<String> strings = null;

			for (ResourceLocation remoteId : remoteIndexedEntries.keySet()) {
				if (!byLocation.containsKey(remoteId)) {
					if (strings == null) {
						strings = new ArrayList<>();
					}

					strings.add(" - " + remoteId);
				}
			}

			if (strings != null) {
				StringBuilder builder = new StringBuilder("Received ID map for " + name + " contains IDs unknown to the receiver!");

				for (String s : strings) {
					builder.append('\n').append(s);
				}

				throw new RemapException(builder.toString());
			}

			break;
		}
		case EXACT: {
			if (!byLocation.keySet().equals(remoteIndexedEntries.keySet())) {
				List<String> strings = new ArrayList<>();

				for (ResourceLocation remoteId : remoteIndexedEntries.keySet()) {
					if (!byLocation.containsKey(remoteId)) {
						strings.add(" - " + remoteId + " (missing on local)");
					}
				}

				for (ResourceLocation localId : keySet()) {
					if (!remoteIndexedEntries.containsKey(localId)) {
						strings.add(" - " + localId + " (missing on remote)");
					}
				}

				StringBuilder builder = new StringBuilder("Local and remote ID sets for " + name + " do not match!");

				for (String s : strings) {
					builder.append('\n').append(s);
				}

				throw new RemapException(builder.toString());
			}

			break;
		}
		}

		// Make a copy of the previous maps.
		// For now, only one is necessary - on an integrated server scenario,
		// AUTHORITATIVE == CLIENT, which is fine.
		// The reason we preserve the first one is because it contains the
		// vanilla order of IDs before mods, which is crucial for vanilla server
		// compatibility.
		if (fabric_prevIndexedEntries == null) {
			fabric_prevIndexedEntries = new Object2IntOpenHashMap<>();
			fabric_prevEntries = HashBiMap.create(byLocation);

			for (T o : this) {
				fabric_prevIndexedEntries.put(getKey(o), getId(o));
			}
		}

		Int2ObjectMap<ResourceLocation> oldIdMap = new Int2ObjectOpenHashMap<>();

		for (T o : this) {
			oldIdMap.put(getId(o), getKey(o));
		}

		// If we're AUTHORITATIVE, we append entries which only exist on the
		// local side to the new entry list. For REMOTE, we instead drop them.
		switch (mode) {
		case AUTHORITATIVE: {
			int maxValue = 0;

			Object2IntMap<ResourceLocation> oldRemoteIndexedEntries = remoteIndexedEntries;
			remoteIndexedEntries = new Object2IntOpenHashMap<>();

			for (ResourceLocation id : oldRemoteIndexedEntries.keySet()) {
				int v = oldRemoteIndexedEntries.getInt(id);
				remoteIndexedEntries.put(id, v);
				if (v > maxValue) maxValue = v;
			}

			for (ResourceLocation id : keySet()) {
				if (!remoteIndexedEntries.containsKey(id)) {
					FABRIC_LOGGER.warn("Adding " + id + " to saved/remote registry.");
					remoteIndexedEntries.put(id, ++maxValue);
				}
			}

			break;
		}
		case REMOTE: {
			int maxId = -1;

			for (ResourceLocation id : keySet()) {
				if (!remoteIndexedEntries.containsKey(id)) {
					if (maxId < 0) {
						for (int value : remoteIndexedEntries.values()) {
							if (value > maxId) {
								maxId = value;
							}
						}
					}

					if (maxId < 0) {
						throw new RemapException("Failed to assign new id to client only registry entry");
					}

					maxId++;

					FABRIC_LOGGER.debug("An ID for {} was not sent by the server, assuming client only registry entry and assigning a new id ({}) in {}", id.toString(), maxId, key().location().toString());
					remoteIndexedEntries.put(id, maxId);
				}
			}

			break;
		}
		}

		Int2IntMap idMap = new Int2IntOpenHashMap();

		for (int i = 0; i < byId.size(); i++) {
			Holder.Reference<T> reference = byId.get(i);

			// Unused id, can happen if there are holes in the registry.
			if (reference == null) {
				throw new RemapException("Unused id " + i + " in registry " + key().location());
			}

			ResourceLocation id = reference.key().location();

			// see above note
			if (remoteIndexedEntries.containsKey(id)) {
				idMap.put(i, remoteIndexedEntries.getInt(id));
			}
		}

		// entries was handled above, if it was necessary.
		byId.clear();
		toId.clear();

		List<ResourceLocation> orderedRemoteEntries = new ArrayList<>(remoteIndexedEntries.keySet());
		orderedRemoteEntries.sort(Comparator.comparingInt(remoteIndexedEntries::getInt));

		for (ResourceLocation identifier : orderedRemoteEntries) {
			int id = remoteIndexedEntries.getInt(identifier);
			Holder.Reference<T> object = byLocation.get(identifier);

			// Warn if an object is missing from the local registry.
			// This should only happen in AUTHORITATIVE mode, and as such we
			// throw an exception otherwise.
			if (object == null) {
				if (mode != RemapMode.AUTHORITATIVE) {
					throw new RemapException(identifier + " missing from registry, but requested!");
				} else {
					FABRIC_LOGGER.warn(identifier + " missing from registry, but requested!");
				}

				continue;
			}

			// Add the new object
			byId.size(Math.max(this.byId.size(), id + 1));
			assert byId.get(id) == null;
			byId.set(id, object);
			toId.put(object.value(), id);
		}

		fabric_getRemapEvent().invoker().onRemap(new RemapStateImpl<>(this, oldIdMap, idMap));
	}

	@Override
	public void unmap(String name) throws RemapException {
		if (fabric_prevIndexedEntries != null) {
			List<ResourceLocation> addedIds = new ArrayList<>();

			// Emit AddObject events for previously culled objects.
			for (ResourceLocation id : fabric_prevEntries.keySet()) {
				if (!byLocation.containsKey(id)) {
					assert fabric_prevIndexedEntries.containsKey(id);
					addedIds.add(id);
				}
			}

			byLocation.clear();
			byKey.clear();

			byLocation.putAll(fabric_prevEntries);

			for (Map.Entry<ResourceLocation, Holder.Reference<T>> entry : fabric_prevEntries.entrySet()) {
				ResourceKey<T> entryKey = ResourceKey.create(key(), entry.getKey());
				byKey.put(entryKey, entry.getValue());
			}

			remap(name, fabric_prevIndexedEntries, RemapMode.AUTHORITATIVE);

			for (ResourceLocation id : addedIds) {
				fabric_getAddObjectEvent().invoker().onEntryAdded(toId.getInt(byLocation.get(id)), id, get(id));
			}

			fabric_prevIndexedEntries = null;
			fabric_prevEntries = null;
		}
	}
}
