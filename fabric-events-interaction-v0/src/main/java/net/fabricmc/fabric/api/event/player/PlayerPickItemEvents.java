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

package net.fabricmc.fabric.api.event.player;

import org.jetbrains.annotations.Nullable;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Contains events triggered by server players requesting to pick items from the world.
 */
public final class PlayerPickItemEvents {
	private PlayerPickItemEvents() { }

	/**
	 * Called when a player requests to pick the item for a block at a given position.
	 */
	public static final Event<PlayerPickItemEvents.PickItemFromBlock> BLOCK = EventFactory.createArrayBacked(PlayerPickItemEvents.PickItemFromBlock.class, callbacks -> (player, pos, state, includeData) -> {
		for (PickItemFromBlock callback : callbacks) {
			ItemStack stack = callback.onPickItemFromBlock(player, pos, state, includeData);

			if (stack != null) {
				return stack;
			}
		}

		return null;
	});

	/**
	 * Called when a player requests to pick the item for a given entity.
	 */
	public static final Event<PlayerPickItemEvents.PickItemFromEntity> ENTITY = EventFactory.createArrayBacked(PlayerPickItemEvents.PickItemFromEntity.class, callbacks -> (player, entity, includeData) -> {
		for (PickItemFromEntity callback : callbacks) {
			ItemStack stack = callback.onPickItemFromEntity(player, entity, includeData);

			if (stack != null) {
				return stack;
			}
		}

		return null;
	});

	@FunctionalInterface
	public interface PickItemFromBlock {
		/**
		 * Determines the pick item stack to give to a player that is attempting to pick an item from a block.
		 *
		 * @param player the player attempting to pick an item from a block
		 * @param pos the position of the block being picked from
		 * @param state the state of the block being picked from
		 * @param includeData whether block entity data should be included in the pick item stack
		 * @return a pick item stack to give to the player, or {@code null} if the default pick item stack should be given
		 */
		@Nullable
		ItemStack onPickItemFromBlock(ServerPlayer player, BlockPos pos, BlockState state, boolean includeData);
	}

	@FunctionalInterface
	public interface PickItemFromEntity {
		/**
		 * Determines the pick item stack to give to a player that is attempting to pick an item from a entity.
		 *
		 * @param player the player attempting to pick an item from a entity
		 * @param entity the entity being picked from
		 * @param includeData whether additional data should be included in the pick item stack; unused in vanilla
		 * @return a pick item stack to give to the player, or {@code null} if the default pick item stack should be given
		 */
		@Nullable
		ItemStack onPickItemFromEntity(ServerPlayer player, Entity entity, boolean includeData);
	}
}
