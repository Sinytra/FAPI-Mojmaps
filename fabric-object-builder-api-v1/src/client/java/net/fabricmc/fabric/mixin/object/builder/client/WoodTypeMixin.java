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

package net.fabricmc.fabric.mixin.object.builder.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.fabricmc.fabric.impl.object.builder.client.SignTypeTextureHelper;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.WoodType;

@Mixin(WoodType.class)
abstract class WoodTypeMixin {
	@Inject(method = "register", at = @At("RETURN"))
	private static void onReturnRegister(WoodType type, CallbackInfoReturnable<WoodType> cir) {
		if (SignTypeTextureHelper.shouldAddTextures) {
			final ResourceLocation identifier = ResourceLocation.parse(type.name());
			Sheets.SIGN_MATERIALS.put(type, Sheets.createSignMaterial(identifier));
			Sheets.HANGING_SIGN_MATERIALS.put(type, Sheets.createHangingSignMaterial(identifier));
		}
	}
}
