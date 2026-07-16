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

package net.fabricmc.fabric.mixin.tag;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableSet;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Either;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.fabricmc.fabric.impl.tag.TagRemovalInternals;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagFile;
import net.minecraft.tags.TagLoader;
import net.minecraft.util.DependencySorter;

@Mixin(TagLoader.class)
public class TagGroupLoaderMixin {
	private static final ThreadLocal<LinkedHashSet<?>> fabric$currentValues = ThreadLocal.withInitial(LinkedHashSet::new);

	@Inject(method = "load", at = @At(value = "INVOKE", target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V", shift = At.Shift.AFTER))
	private void loadRemoveEntries(ResourceManager resourceManager, CallbackInfoReturnable<Map<ResourceLocation, List<TagLoader.EntryWithSource>>> cir, @Local(ordinal = 0) ResourceLocation id, @Local TagFile parsedContents, @Local String sourceId) {
		ResourceLocation normalizedId = TagRemovalInternals.normalizeTagResourceId(id);

		for (TagEntry entry : parsedContents.remove()) {
			TagLoader.EntryWithSource entryWithSource = new TagLoader.EntryWithSource(entry, sourceId);
			TagRemovalInternals.addRemoveEntry(normalizedId, entryWithSource);
		}

		TagRemovalInternals.addTagSource(normalizedId, sourceId);
	}

	@WrapOperation(method = "build(Ljava/util/Map;)Ljava/util/Map;", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/DependencySorter;orderByDependencies(Ljava/util/function/BiConsumer;)V"))
	private void scopeIdAroundOrderByDependencies(DependencySorter<ResourceLocation, TagLoader.SortingEntry> sorter, BiConsumer<ResourceLocation, TagLoader.SortingEntry> consumer, Operation<Void> original) {
		original.call(sorter, (BiConsumer<ResourceLocation, TagLoader.SortingEntry>) (id, contents) -> {
			TagRemovalInternals.setTagId(id);

			try {
				consumer.accept(id, contents);
			} finally {
				TagRemovalInternals.clearTagId();
			}
		});
	}

	@ModifyArg(method = "build(Ljava/util/Map;)Ljava/util/Map;", at = @At(value = "INVOKE", target = "Ljava/util/Map;forEach(Ljava/util/function/BiConsumer;)V"))
	private BiConsumer<ResourceLocation, List<TagLoader.EntryWithSource>> addTagRemovalReferencesToDependencyTracker(BiConsumer<ResourceLocation, List<TagLoader.EntryWithSource>> forEach) {
		return (id, entries) -> {
			TagRemovalInternals.mergeAddedAndRemovedEntries(id, entries);
			forEach.accept(id, entries);
		};
	}

	@WrapOperation(method = "build(Lnet/minecraft/tags/TagEntry$Lookup;Ljava/util/List;)Lcom/mojang/datafixers/util/Either;", at = @At(value = "INVOKE", target = "Lnet/minecraft/tags/TagEntry;build(Lnet/minecraft/tags/TagEntry$Lookup;Ljava/util/function/Consumer;)Z"))
	private <T> boolean removeEntriesFromTags(TagEntry instance, TagEntry.Lookup<T> lookup, Consumer<T> output, Operation<Boolean> original, @Local TagLoader.EntryWithSource entry) {
		final LinkedHashSet<T> values = (LinkedHashSet<T>) fabric$currentValues.get();

		if (TagRemovalInternals.isEntryRemove(entry)) {
			instance.build(lookup, values::remove);
			return true;
		}

		return original.call(instance, lookup, (Consumer<T>) (values::add));
	}

	@Inject(method = "build(Lnet/minecraft/tags/TagEntry$Lookup;Ljava/util/List;)Lcom/mojang/datafixers/util/Either;", at = @At("HEAD"))
	private void initCurrentValues(TagEntry.Lookup<?> valueGetter, List<TagLoader.EntryWithSource> entries, CallbackInfoReturnable<Either<Collection<TagLoader.EntryWithSource>, Collection<?>>> cir) {
		fabric$currentValues.get().clear();
	}

	@Inject(method = "build(Lnet/minecraft/tags/TagEntry$Lookup;Ljava/util/List;)Lcom/mojang/datafixers/util/Either;", at = @At("RETURN"), cancellable = true)
	private void replaceRightReturn(TagEntry.Lookup<?> valueGetter, List<TagLoader.EntryWithSource> entries, CallbackInfoReturnable<Either<Collection<TagLoader.EntryWithSource>, Collection<?>>> cir) {
		Either<Collection<TagLoader.EntryWithSource>, Collection<?>> returnValue = cir.getReturnValue();

		if (returnValue != null && returnValue.right().isPresent()) {
			cir.setReturnValue(Either.right(ImmutableSet.copyOf(fabric$currentValues.get())));
		}

		fabric$currentValues.get().clear();
	}

	@Inject(method = "build(Ljava/util/Map;)Ljava/util/Map;", at = @At("RETURN"))
	private <T> void removeTagRemovalReferencesWhenFinished(Map<ResourceLocation, List<TagLoader.EntryWithSource>> builders, CallbackInfoReturnable<Map<ResourceLocation, List<T>>> cir) {
		TagRemovalInternals.removeTagRemovalReferences();
	}
}
