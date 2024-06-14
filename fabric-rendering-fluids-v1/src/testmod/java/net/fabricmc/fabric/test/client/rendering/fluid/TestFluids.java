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

package net.fabricmc.fabric.test.client.rendering.fluid;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class TestFluids {
	public static final NoOverlayFluid NO_OVERLAY = Registry.register(BuiltInRegistries.FLUID, "fabric-rendering-fluids-v1-testmod:no_overlay", new NoOverlayFluid.Still());
	public static final NoOverlayFluid NO_OVERLAY_FLOWING = Registry.register(BuiltInRegistries.FLUID, "fabric-rendering-fluids-v1-testmod:no_overlay_flowing", new NoOverlayFluid.Flowing());

	public static final LiquidBlock NO_OVERLAY_BLOCK = Registry.register(BuiltInRegistries.BLOCK, "fabric-rendering-fluids-v1-testmod:no_overlay", new LiquidBlock(NO_OVERLAY, BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)) {
	});

	public static final OverlayFluid OVERLAY = Registry.register(BuiltInRegistries.FLUID, "fabric-rendering-fluids-v1-testmod:overlay", new OverlayFluid.Still());
	public static final OverlayFluid OVERLAY_FLOWING = Registry.register(BuiltInRegistries.FLUID, "fabric-rendering-fluids-v1-testmod:overlay_flowing", new OverlayFluid.Flowing());

	public static final LiquidBlock OVERLAY_BLOCK = Registry.register(BuiltInRegistries.BLOCK, "fabric-rendering-fluids-v1-testmod:overlay", new LiquidBlock(OVERLAY, BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)) {
	});

	public static final UnregisteredFluid UNREGISTERED = Registry.register(BuiltInRegistries.FLUID, "fabric-rendering-fluids-v1-testmod:unregistered", new UnregisteredFluid.Still());
	public static final UnregisteredFluid UNREGISTERED_FLOWING = Registry.register(BuiltInRegistries.FLUID, "fabric-rendering-fluids-v1-testmod:unregistered_flowing", new UnregisteredFluid.Flowing());

	public static final LiquidBlock UNREGISTERED_BLOCK = Registry.register(BuiltInRegistries.BLOCK, "fabric-rendering-fluids-v1-testmod:unregistered", new LiquidBlock(UNREGISTERED, BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)) {
	});

	public static final CustomFluid CUSTOM = Registry.register(BuiltInRegistries.FLUID, "fabric-rendering-fluids-v1-testmod:custom", new CustomFluid.Still());
	public static final CustomFluid CUSTOM_FLOWING = Registry.register(BuiltInRegistries.FLUID, "fabric-rendering-fluids-v1-testmod:custom_flowing", new CustomFluid.Flowing());

	public static final LiquidBlock CUSTOM_BLOCK = Registry.register(BuiltInRegistries.BLOCK, "fabric-rendering-fluids-v1-testmod:custom", new LiquidBlock(CUSTOM, BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)) {
	});
}
