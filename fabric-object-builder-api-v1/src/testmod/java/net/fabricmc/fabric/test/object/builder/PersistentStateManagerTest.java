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

package net.fabricmc.fabric.test.object.builder;

import java.util.Objects;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class PersistentStateManagerTest implements ModInitializer {
	private boolean ranTests = false;

	@Override
	public void onInitialize() {
		ServerTickEvents.END_WORLD_TICK.register(world -> {
			if (ranTests) return;
			ranTests = true;

			TestState.getOrCreate(world).setValue("Hello!");
			assert Objects.equals(TestState.getOrCreate(world).getValue(), "Hello!");
		});
	}

	private static class TestState extends SavedData {
		/**
		 * We are testing that null can be passed as the dataFixType.
		 */
		private static final SavedData.Factory<TestState> TYPE = new Factory<>(TestState::new, TestState::fromTag, null);

		public static TestState getOrCreate(ServerLevel world) {
			return world.getDataStorage().computeIfAbsent(TestState.TYPE, ObjectBuilderTestConstants.id("test_state").toString().replace(":", "_"));
		}

		private String value = "";

		private TestState() {
		}

		private TestState(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
			setDirty();
		}

		@Override
		public CompoundTag save(CompoundTag nbt, HolderLookup.Provider wrapperLookup) {
			nbt.putString("value", value);
			return nbt;
		}

		private static TestState fromTag(CompoundTag tag, HolderLookup.Provider wrapperLookup) {
			return new TestState(tag.getString("value"));
		}
	}
}
