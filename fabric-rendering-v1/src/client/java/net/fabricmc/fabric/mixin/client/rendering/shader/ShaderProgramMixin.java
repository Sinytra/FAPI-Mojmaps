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

package net.fabricmc.fabric.mixin.client.rendering.shader;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.shaders.Program;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import net.fabricmc.fabric.impl.client.rendering.FabricShaderProgram;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;

@Mixin(ShaderInstance.class)
abstract class ShaderProgramMixin {
	@Shadow
	@Final
	private String name;

	// Allow loading FabricShaderPrograms from arbitrary namespaces.
	@WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/resources/ResourceLocation;withDefaultNamespace(Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;"), allow = 1)
	private ResourceLocation modifyId(String id, Operation<ResourceLocation> original) {
		if ((Object) this instanceof FabricShaderProgram) {
			return FabricShaderProgram.rewriteAsId(id, name);
		}

		return original.call(id);
	}

	// Allow loading shader stages from arbitrary namespaces.
	@ModifyVariable(method = "getOrCreate", at = @At("STORE"), ordinal = 1)
	private static String modifyStageId(String id, ResourceProvider factory, Program.Type type, String name) {
		if (name.contains(String.valueOf(ResourceLocation.NAMESPACE_SEPARATOR))) {
			return FabricShaderProgram.rewriteAsId(id, name).toString();
		}

		return id;
	}

	@WrapOperation(method = "getOrCreate", at = @At(value = "INVOKE", target = "Lnet/minecraft/resources/ResourceLocation;withDefaultNamespace(Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;"), allow = 1)
	private static ResourceLocation allowNoneMinecraftId(String id, Operation<ResourceLocation> original) {
		if (id.contains(String.valueOf(ResourceLocation.NAMESPACE_SEPARATOR))) {
			return ResourceLocation.parse(id);
		}

		return original.call(id);
	}
}
