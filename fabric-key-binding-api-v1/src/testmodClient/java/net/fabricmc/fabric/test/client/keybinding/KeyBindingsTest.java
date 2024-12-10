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

package net.fabricmc.fabric.test.client.keybinding;

import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.ToggleKeyMapping;
import net.minecraft.network.chat.Component;

public class KeyBindingsTest implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		KeyMapping binding1 = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.fabric-key-binding-api-v1-testmod.test_keybinding_1", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_P, "key.category.first.test"));
		KeyMapping binding2 = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.fabric-key-binding-api-v1-testmod.test_keybinding_2", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_U, "key.category.second.test"));
		KeyMapping stickyBinding = KeyBindingHelper.registerKeyBinding(new ToggleKeyMapping("key.fabric-key-binding-api-v1-testmod.test_keybinding_sticky", GLFW.GLFW_KEY_R, "key.category.first.test", () -> true));
		KeyMapping duplicateBinding = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.fabric-key-binding-api-v1-testmod.test_keybinding_duplicate", GLFW.GLFW_KEY_RIGHT_SHIFT, "key.category.first.test"));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (binding1.consumeClick()) {
				client.player.displayClientMessage(Component.literal("Key 1 was pressed!"), false);
			}

			while (binding2.consumeClick()) {
				client.player.displayClientMessage(Component.literal("Key 2 was pressed!"), false);
			}

			if (stickyBinding.isDown()) {
				client.player.displayClientMessage(Component.literal("Sticky Key was pressed!"), false);
			}

			while (duplicateBinding.consumeClick()) {
				client.player.displayClientMessage(Component.literal("Duplicate Key was pressed!"), false);
			}
		});
	}
}
