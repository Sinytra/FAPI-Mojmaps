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

package net.fabricmc.fabric.impl.client.itemgroup;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.fabricmc.fabric.impl.itemgroup.FabricItemGroupImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

public class FabricCreativeGuiComponents {
	private static final ResourceLocation BUTTON_TEX = ResourceLocation.fromNamespaceAndPath("fabric", "textures/gui/creative_buttons.png");
	private static final double TABS_PER_PAGE = FabricItemGroupImpl.TABS_PER_PAGE;
	public static final Set<CreativeModeTab> COMMON_GROUPS = Set.of(CreativeModeTabs.SEARCH, CreativeModeTabs.INVENTORY, CreativeModeTabs.HOTBAR).stream()
			.map(BuiltInRegistries.CREATIVE_MODE_TAB::getOrThrow)
			.collect(Collectors.toSet());

	public static int getPageCount() {
		return (int) Math.ceil((CreativeModeTabs.tabs().size() - COMMON_GROUPS.size()) / TABS_PER_PAGE);
	}

	public static class ItemGroupButtonWidget extends Button {
		final CreativeModeInventoryScreen screen;
		final Type type;

		public ItemGroupButtonWidget(int x, int y, Type type, CreativeModeInventoryScreen screen) {
			super(x, y, 11, 12, type.text, (bw) -> type.clickConsumer.accept(screen), Button.DEFAULT_NARRATION);
			this.type = type;
			this.screen = screen;
		}

		@Override
		protected void renderWidget(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
			this.active = type.isEnabled.test(screen);
			this.visible = screen.hasAdditionalPages();

			if (!this.visible) {
				return;
			}

			int u = active && this.isHovered() ? 22 : 0;
			int v = active ? 0 : 12;
			drawContext.blit(BUTTON_TEX, this.getX(), this.getY(), u + (type == Type.NEXT ? 11 : 0), v, 11, 12);

			if (this.isHovered()) {
				drawContext.renderTooltip(Minecraft.getInstance().font, Component.translatable("fabric.gui.creativeTabPage", screen.getCurrentPage() + 1, getPageCount()), mouseX, mouseY);
			}
		}
	}

	public enum Type {
		NEXT(Component.literal(">"), CreativeModeInventoryScreen::switchToNextPage, screen -> screen.getCurrentPage() + 1 < screen.getPageCount()),
		PREVIOUS(Component.literal("<"), CreativeModeInventoryScreen::switchToPreviousPage, screen -> screen.getCurrentPage() != 0);

		final Component text;
		final Consumer<CreativeModeInventoryScreen> clickConsumer;
		final Predicate<CreativeModeInventoryScreen> isEnabled;

		Type(Component text, Consumer<CreativeModeInventoryScreen> clickConsumer, Predicate<CreativeModeInventoryScreen> isEnabled) {
			this.text = text;
			this.clickConsumer = clickConsumer;
			this.isEnabled = isEnabled;
		}
	}
}
