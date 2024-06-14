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

package net.fabricmc.fabric.impl.client.particle;

import java.util.IdentityHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.particle.v1.FabricSpriteProvider;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.mixin.client.particle.ParticleManagerAccessor;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

public final class ParticleFactoryRegistryImpl implements ParticleFactoryRegistry {
	public static final ParticleFactoryRegistryImpl INSTANCE = new ParticleFactoryRegistryImpl();

	static class DeferredParticleFactoryRegistry implements ParticleFactoryRegistry {
		private final Map<ParticleType<?>, ParticleProvider<?>> factories = new IdentityHashMap<>();
		private final Map<ParticleType<?>, PendingParticleFactory<?>> constructors = new IdentityHashMap<>();

		@Override
		public <T extends ParticleOptions> void register(ParticleType<T> type, ParticleProvider<T> factory) {
			factories.put(type, factory);
		}

		@Override
		public <T extends ParticleOptions> void register(ParticleType<T> type, PendingParticleFactory<T> factory) {
			constructors.put(type, factory);
		}

		@SuppressWarnings("unchecked")
		void applyTo(ParticleFactoryRegistry registry) {
			for (Map.Entry<ParticleType<?>, ParticleProvider<?>> entry : factories.entrySet()) {
				ParticleType type = entry.getKey();
				ParticleProvider factory = entry.getValue();
				registry.register(type, factory);
			}

			for (Map.Entry<ParticleType<?>, PendingParticleFactory<?>> entry : constructors.entrySet()) {
				ParticleType type = entry.getKey();
				PendingParticleFactory constructor = entry.getValue();
				registry.register(type, constructor);
			}
		}
	}

	static class DirectParticleFactoryRegistry implements ParticleFactoryRegistry {
		private final ParticleEngine particleManager;

		DirectParticleFactoryRegistry(ParticleEngine particleManager) {
			this.particleManager = particleManager;
		}

		@Override
		public <T extends ParticleOptions> void register(ParticleType<T> type, ParticleProvider<T> factory) {
			((ParticleManagerAccessor) particleManager).getFactories().put(BuiltInRegistries.PARTICLE_TYPE.getId(type), factory);
		}

		@Override
		public <T extends ParticleOptions> void register(ParticleType<T> type, PendingParticleFactory<T> constructor) {
			SpriteSet delegate = new ParticleEngine.MutableSpriteSet();
			FabricSpriteProvider fabricSpriteProvider = new FabricSpriteProviderImpl(particleManager, delegate);
			((ParticleManagerAccessor) particleManager).getSpriteAwareFactories().put(BuiltInRegistries.PARTICLE_TYPE.getKey(type), delegate);
			register(type, constructor.create(fabricSpriteProvider));
		}
	}

	ParticleFactoryRegistry internalRegistry = new DeferredParticleFactoryRegistry();

	private ParticleFactoryRegistryImpl() { }

	@Override
	public <T extends ParticleOptions> void register(ParticleType<T> type, ParticleProvider<T> factory) {
		internalRegistry.register(type, factory);
	}

	@Override
	public <T extends ParticleOptions> void register(ParticleType<T> type, PendingParticleFactory<T> constructor) {
		internalRegistry.register(type, constructor);
	}

	public void initialize(ParticleEngine particleManager) {
		ParticleFactoryRegistry newRegistry = new DirectParticleFactoryRegistry(particleManager);
		DeferredParticleFactoryRegistry oldRegistry = (DeferredParticleFactoryRegistry) internalRegistry;
		oldRegistry.applyTo(newRegistry);
		internalRegistry = newRegistry;
	}
}
