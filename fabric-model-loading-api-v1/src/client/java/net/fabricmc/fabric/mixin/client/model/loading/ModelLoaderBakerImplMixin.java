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

package net.fabricmc.fabric.mixin.client.model.loading;

import java.util.function.Function;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import net.fabricmc.fabric.impl.client.model.loading.BakerImplHooks;
import net.fabricmc.fabric.impl.client.model.loading.ModelLoaderHooks;
import net.fabricmc.fabric.impl.client.model.loading.ModelLoadingEventDispatcher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;

@Mixin(targets = "net/minecraft/client/resources/model/ModelBakery$ModelBakerImpl")
abstract class ModelLoaderBakerImplMixin implements BakerImplHooks {
	@Shadow
	@Final
	private ModelBakery this$0;
	@Shadow
	@Final
	private Function<Material, TextureAtlasSprite> modelTextureGetter;

	@WrapOperation(method = "bake(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/client/resources/model/ModelState;)Lnet/minecraft/client/resources/model/BakedModel;", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/ModelBakery$ModelBakerImpl;bakeUncached(Lnet/minecraft/client/resources/model/UnbakedModel;Lnet/minecraft/client/resources/model/ModelState;)Lnet/minecraft/client/resources/model/BakedModel;"))
	private BakedModel wrapInnerBake(@Coerce ModelBaker self, UnbakedModel unbakedModel, ModelState settings, Operation<BakedModel> operation, ResourceLocation id) {
		ModelLoadingEventDispatcher dispatcher = ((ModelLoaderHooks) this.this$0).fabric_getDispatcher();
		unbakedModel = dispatcher.modifyModelBeforeBake(unbakedModel, id, null, modelTextureGetter, settings, self);
		BakedModel model = operation.call(self, unbakedModel, settings);
		return dispatcher.modifyModelAfterBake(model, id, null, unbakedModel, modelTextureGetter, settings, self);
	}

	@Override
	public Function<Material, TextureAtlasSprite> fabric_getTextureGetter() {
		return modelTextureGetter;
	}
}
