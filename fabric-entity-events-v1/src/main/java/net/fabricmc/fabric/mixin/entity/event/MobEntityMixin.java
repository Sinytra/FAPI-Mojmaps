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

package net.fabricmc.fabric.mixin.entity.event;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

@Mixin(Mob.class)
public class MobEntityMixin {
	@ModifyArg(method = "convertTo", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;spawnEntityAndPassengers(Lnet/minecraft/world/entity/Entity;)V"))
	private Entity afterEntityConverted(Entity converted, @Local(argsOnly = true) ConversionParams conversionContext) {
		ServerLivingEntityEvents.MOB_CONVERSION.invoker().onConversion((Mob) (Object) this, (Mob) converted, conversionContext);
		return converted;
	}
}
