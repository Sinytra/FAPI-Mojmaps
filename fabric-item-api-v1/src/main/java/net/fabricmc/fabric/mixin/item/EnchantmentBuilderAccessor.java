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

package net.fabricmc.fabric.mixin.item;

import java.util.List;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Enchantment.Builder.class)
public interface EnchantmentBuilderAccessor {
	@Accessor("definition")
	Enchantment.EnchantmentDefinition getDefinition();

	@Accessor("exclusiveSet")
	HolderSet<Enchantment> getExclusiveSet();

	@Accessor("effectMapBuilder")
	DataComponentMap.Builder getEffectMap();

	@Invoker("getEffectsList")
	<E> List<E> invokeGetEffectsList(DataComponentType<List<E>> type);
}
