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

package net.fabricmc.fabric.test.base.client;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.lang3.function.FailableConsumer;
import org.apache.commons.lang3.function.FailableFunction;
import org.apache.commons.lang3.mutable.MutableObject;

import net.minecraft.SharedConstants;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.test.base.client.mixin.CyclingButtonWidgetAccessor;
import net.fabricmc.fabric.test.base.client.mixin.ScreenAccessor;
import net.fabricmc.fabric.test.base.client.mixin.TitleScreenAccessor;
import net.fabricmc.loader.api.FabricLoader;

public final class FabricClientTestHelper {
	public static void waitForLoadingComplete() {
		// client is not ticking and can't accept tasks, waitFor doesn't work so we'll do this until then
		while (!ThreadingImpl.clientCanAcceptTasks) {
			runTick();

			try {
				//noinspection BusyWait
				Thread.sleep(50);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		waitFor("Loading to complete", client -> client.getOverlay() == null, 5 * SharedConstants.TICKS_PER_MINUTE);
	}

	public static void waitForScreen(Class<? extends Screen> screenClass) {
		waitFor("Screen %s".formatted(screenClass.getName()), client -> client.screen != null && client.screen.getClass() == screenClass);
	}

	public static void openGameMenu() {
		setScreen((client) -> new PauseScreen(true));
		waitForScreen(PauseScreen.class);
	}

	public static void openInventory() {
		setScreen((client) -> new InventoryScreen(Objects.requireNonNull(client.player)));

		boolean creative = computeOnClient(client -> Objects.requireNonNull(client.player).isCreative());
		waitForScreen(creative ? CreativeModeInventoryScreen.class : InventoryScreen.class);
	}

	public static void closeScreen() {
		setScreen((client) -> null);
	}

	private static void setScreen(Function<Minecraft, Screen> screenSupplier) {
		runOnClient(client -> client.setScreen(screenSupplier.apply(client)));
	}

	public static void takeScreenshot(String name) {
		takeScreenshot(name, 1);
	}

	public static void takeScreenshot(String name, int delayTicks) {
		// Allow time for any screens to open
		runTicks(delayTicks);

		runOnClient(client -> {
			Screenshot.grab(FabricLoader.getInstance().getGameDir().toFile(), name + ".png", client.getMainRenderTarget(), (message) -> {
			});
		});
	}

	public static void clickScreenButton(String translationKey) {
		final String buttonText = Component.translatable(translationKey).getString();

		waitFor("Click button" + buttonText, client -> {
			final Screen screen = client.screen;

			if (screen == null) {
				return false;
			}

			final ScreenAccessor screenAccessor = (ScreenAccessor) screen;

			for (Renderable drawable : screenAccessor.getRenderables()) {
				if (drawable instanceof AbstractButton pressableWidget && pressMatchingButton(pressableWidget, buttonText)) {
					return true;
				}

				if (drawable instanceof LayoutElement widget) {
					widget.visitWidgets(clickableWidget -> pressMatchingButton(clickableWidget, buttonText));
				}
			}

			// Was unable to find the button to press
			return false;
		});
	}

	private static boolean pressMatchingButton(AbstractWidget widget, String text) {
		if (widget instanceof Button buttonWidget) {
			if (text.equals(buttonWidget.getMessage().getString())) {
				buttonWidget.onPress();
				return true;
			}
		}

		if (widget instanceof CycleButton<?> buttonWidget) {
			CyclingButtonWidgetAccessor accessor = (CyclingButtonWidgetAccessor) buttonWidget;

			if (text.equals(accessor.getName().getString())) {
				buttonWidget.onPress();
				return true;
			}
		}

		return false;
	}

	public static void waitForWorldTicks(long ticks) {
		// Wait for the world to be loaded and get the start ticks
		waitFor("World load", client -> client.level != null && !(client.screen instanceof LevelLoadingScreen), 30 * SharedConstants.TICKS_PER_MINUTE);
		final long startTicks = computeOnClient(client -> client.level.getGameTime());
		waitFor("World load", client -> Objects.requireNonNull(client.level).getGameTime() > startTicks + ticks, 10 * SharedConstants.TICKS_PER_MINUTE);
	}

	public static void enableDebugHud() {
		runOnClient(client -> client.gui.getDebugOverlay().toggleOverlay());
	}

	public static void setPerspective(CameraType perspective) {
		runOnClient(client -> client.options.setCameraType(perspective));
	}

	public static void connectToServer(TestDedicatedServer server) {
		runOnClient(client -> {
			final var serverInfo = new ServerData("localhost", server.getConnectionAddress(), ServerData.Type.OTHER);
			ConnectScreen.startConnecting(client.screen, client, ServerAddress.parseString(server.getConnectionAddress()), serverInfo, false, null);
		});
	}

	public static void waitForTitleScreenFade() {
		waitFor("Title screen fade", client -> {
			if (!(client.screen instanceof TitleScreen titleScreen)) {
				return false;
			}

			return !((TitleScreenAccessor) titleScreen).getFading();
		});
	}

	public static void waitForServerStop() {
		waitFor("Server stop", client -> !ThreadingImpl.isServerRunning, SharedConstants.TICKS_PER_MINUTE);
	}

	private static void waitFor(String what, Predicate<Minecraft> predicate) {
		waitFor(what, predicate, 10 * SharedConstants.TICKS_PER_SECOND);
	}

	private static void waitFor(String what, Predicate<Minecraft> predicate, int timeoutTicks) {
		int tickCount;

		for (tickCount = 0; tickCount < timeoutTicks && !computeOnClient(predicate::test); tickCount++) {
			runTick();
		}

		if (tickCount == timeoutTicks && !computeOnClient(predicate::test)) {
			throw new RuntimeException("Timed out waiting for " + what);
		}
	}

	public static void runTicks(int ticks) {
		for (int i = 0; i < ticks; i++) {
			runTick();
		}
	}

	public static void runTick() {
		ThreadingImpl.runTick();
	}

	public static <E extends Throwable> void runOnClient(FailableConsumer<Minecraft, E> action) throws E {
		ThreadingImpl.runOnClient(() -> action.accept(Minecraft.getInstance()));
	}

	public static <T, E extends Throwable> T computeOnClient(FailableFunction<Minecraft, T, E> action) throws E {
		MutableObject<T> result = new MutableObject<>();
		runOnClient(client -> result.setValue(action.apply(client)));
		return result.getValue();
	}
}
