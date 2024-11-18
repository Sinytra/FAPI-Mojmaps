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

package net.fabricmc.fabric.mixin.attachment;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import net.fabricmc.fabric.impl.attachment.AttachmentTargetImpl;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.Level;

@Mixin(Level.class)
abstract class WorldMixin implements AttachmentTargetImpl {
	@Shadow
	public abstract boolean isClientSide();

	@Shadow
	public abstract RegistryAccess registryAccess();

	@Override
	public boolean fabric_shouldTryToSync() {
		return !this.isClientSide();
	}

	@Override
	public RegistryAccess fabric_getDynamicRegistryManager() {
		return registryAccess();
	}
}
