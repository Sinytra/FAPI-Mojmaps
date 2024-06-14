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

package net.fabricmc.fabric.impl.object.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;

public final class TradeOfferInternals {
	private static final Logger LOGGER = LoggerFactory.getLogger("fabric-object-builder-api-v1");

	private TradeOfferInternals() {
	}

	/**
	 * Make the rebalanced profession map modifiable, then copy all vanilla
	 * professions' trades to prevent modifications from propagating to the rebalanced one.
	 */
	private static void initVillagerTrades() {
		if (!(VillagerTrades.EXPERIMENTAL_TRADES instanceof HashMap)) {
			Map<VillagerProfession, Int2ObjectMap<VillagerTrades.ItemListing[]>> map = new HashMap<>(VillagerTrades.EXPERIMENTAL_TRADES);

			for (Map.Entry<VillagerProfession, Int2ObjectMap<VillagerTrades.ItemListing[]>> trade : VillagerTrades.TRADES.entrySet()) {
				if (!map.containsKey(trade.getKey())) map.put(trade.getKey(), trade.getValue());
			}

			VillagerTrades.EXPERIMENTAL_TRADES = map;
		}
	}

	// synchronized guards against concurrent modifications - Vanilla does not mutate the underlying arrays (as of 1.16),
	// so reads will be fine without locking.
	public static synchronized void registerVillagerOffers(VillagerProfession profession, int level, TradeOfferHelper.VillagerOffersAdder factory) {
		Objects.requireNonNull(profession, "VillagerProfession may not be null.");
		initVillagerTrades();
		registerOffers(VillagerTrades.TRADES.computeIfAbsent(profession, key -> new Int2ObjectOpenHashMap<>()), level, trades -> factory.onRegister(trades, false));
		registerOffers(VillagerTrades.EXPERIMENTAL_TRADES.computeIfAbsent(profession, key -> new Int2ObjectOpenHashMap<>()), level, trades -> factory.onRegister(trades, true));
	}

	public static synchronized void registerWanderingTraderOffers(int level, Consumer<List<VillagerTrades.ItemListing>> factory) {
		registerOffers(VillagerTrades.WANDERING_TRADER_TRADES, level, factory);
	}

	// Shared code to register offers for both villagers and wandering traders.
	private static void registerOffers(Int2ObjectMap<VillagerTrades.ItemListing[]> leveledTradeMap, int level, Consumer<List<VillagerTrades.ItemListing>> factory) {
		final List<VillagerTrades.ItemListing> list = new ArrayList<>();
		factory.accept(list);

		final VillagerTrades.ItemListing[] originalEntries = leveledTradeMap.computeIfAbsent(level, key -> new VillagerTrades.ItemListing[0]);
		final VillagerTrades.ItemListing[] addedEntries = list.toArray(new VillagerTrades.ItemListing[0]);

		final VillagerTrades.ItemListing[] allEntries = ArrayUtils.addAll(originalEntries, addedEntries);
		leveledTradeMap.put(level, allEntries);
	}

	public static void printRefreshOffersWarning() {
		Throwable loggingThrowable = new Throwable();
		LOGGER.warn("TradeOfferHelper#refreshOffers does not do anything, yet it was called! Stack trace:", loggingThrowable);
	}

	public static class WanderingTraderOffersBuilderImpl implements TradeOfferHelper.WanderingTraderOffersBuilder {
		private static final Object2IntMap<ResourceLocation> ID_TO_INDEX = Util.make(new Object2IntOpenHashMap<>(), idToIndex -> {
			idToIndex.put(BUY_ITEMS_POOL, 0);
			idToIndex.put(SELL_SPECIAL_ITEMS_POOL, 1);
			idToIndex.put(SELL_COMMON_ITEMS_POOL, 2);
		});

		private static final Map<ResourceLocation, VillagerTrades.ItemListing[]> DELAYED_MODIFICATIONS = new HashMap<>();

		/**
		 * Make the trade list modifiable.
		 */
		static void initWanderingTraderTrades() {
			if (!(VillagerTrades.EXPERIMENTAL_WANDERING_TRADER_TRADES instanceof ArrayList)) {
				VillagerTrades.EXPERIMENTAL_WANDERING_TRADER_TRADES = new ArrayList<>(VillagerTrades.EXPERIMENTAL_WANDERING_TRADER_TRADES);
			}
		}

		@Override
		public TradeOfferHelper.WanderingTraderOffersBuilder pool(ResourceLocation id, int count, VillagerTrades.ItemListing... factories) {
			if (factories.length == 0) throw new IllegalArgumentException("cannot add empty pool");
			if (count <= 0) throw new IllegalArgumentException("count must be positive");

			Objects.requireNonNull(id, "id cannot be null");

			if (ID_TO_INDEX.containsKey(id)) throw new IllegalArgumentException("pool id %s is already registered".formatted(id));

			Pair<VillagerTrades.ItemListing[], Integer> pool = Pair.of(factories, count);
			initWanderingTraderTrades();
			ID_TO_INDEX.put(id, VillagerTrades.EXPERIMENTAL_WANDERING_TRADER_TRADES.size());
			VillagerTrades.EXPERIMENTAL_WANDERING_TRADER_TRADES.add(pool);
			VillagerTrades.ItemListing[] delayedModifications = DELAYED_MODIFICATIONS.remove(id);

			if (delayedModifications != null) addOffersToPool(id, delayedModifications);

			return this;
		}

		@Override
		public TradeOfferHelper.WanderingTraderOffersBuilder addOffersToPool(ResourceLocation pool, VillagerTrades.ItemListing... factories) {
			if (!ID_TO_INDEX.containsKey(pool)) {
				DELAYED_MODIFICATIONS.compute(pool, (id, current) -> {
					if (current == null) return factories;

					return ArrayUtils.addAll(current, factories);
				});
				return this;
			}

			int poolIndex = ID_TO_INDEX.getInt(pool);
			initWanderingTraderTrades();
			Pair<VillagerTrades.ItemListing[], Integer> poolPair = VillagerTrades.EXPERIMENTAL_WANDERING_TRADER_TRADES.get(poolIndex);
			VillagerTrades.ItemListing[] modified = ArrayUtils.addAll(poolPair.getLeft(), factories);
			VillagerTrades.EXPERIMENTAL_WANDERING_TRADER_TRADES.set(poolIndex, Pair.of(modified, poolPair.getRight()));
			return this;
		}
	}
}
