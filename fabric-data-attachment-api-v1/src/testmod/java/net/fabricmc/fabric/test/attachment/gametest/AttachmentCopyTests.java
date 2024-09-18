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

package net.fabricmc.fabric.test.attachment.gametest;

import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.test.attachment.AttachmentTestMod;
import net.fabricmc.fabric.test.attachment.mixin.ZombieEntityAccessor;

public class AttachmentCopyTests implements FabricGameTest {
	// using a lambda type because serialization shouldn't play a role in this
	public static AttachmentType<IntSupplier> DUMMY = AttachmentRegistry.create(
			ResourceLocation.fromNamespaceAndPath(AttachmentTestMod.MOD_ID, "dummy")
	);
	public static AttachmentType<IntSupplier> COPY_ON_DEATH = AttachmentRegistry.<IntSupplier>builder()
			.copyOnDeath()
			.buildAndRegister(ResourceLocation.fromNamespaceAndPath(AttachmentTestMod.MOD_ID, "copy_test"));

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void testCrossWorldTeleport(GameTestHelper context) {
		MinecraftServer server = context.getLevel().getServer();
		ServerLevel overworld = server.overworld();
		ServerLevel end = server.getLevel(Level.END);
		// using overworld and end to avoid portal code related to the nether

		Entity entity = EntityType.PIG.create(overworld, EntitySpawnReason.SPAWN_ITEM_USE);
		Objects.requireNonNull(entity, "entity was null");
		entity.setAttached(DUMMY, () -> 10);
		entity.setAttached(COPY_ON_DEATH, () -> 10);

		Entity moved = entity.teleport(new TeleportTransition(end, entity, TeleportTransition.DO_NOTHING));
		if (moved == null) throw new GameTestAssertException("Cross-world teleportation failed");

		IntSupplier attached1 = moved.getAttached(DUMMY);
		IntSupplier attached2 = moved.getAttached(COPY_ON_DEATH);

		if (attached1 == null || attached1.getAsInt() != 10 || attached2 == null || attached2.getAsInt() != 10) {
			throw new GameTestAssertException("Attachment copying failed during cross-world teleportation");
		}

		moved.discard();
		context.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void testMobConversion(GameTestHelper context) {
		Zombie mob = context.spawn(EntityType.ZOMBIE, BlockPos.ZERO);
		mob.setAttached(DUMMY, () -> 42);
		mob.setAttached(COPY_ON_DEATH, () -> 42);

		ZombieEntityAccessor zombieEntityAccessor = (ZombieEntityAccessor) mob;
		zombieEntityAccessor.invokeConvertTo(EntityType.DROWNED);
		List<Drowned> drowned = context.getEntities(EntityType.DROWNED);

		if (drowned.size() != 1) {
			throw new GameTestAssertException("Conversion failed");
		}

		Drowned converted = drowned.getFirst();
		if (converted == null) throw new GameTestAssertException("Conversion failed");

		if (converted.hasAttached(DUMMY)) {
			throw new GameTestAssertException("Attachment shouldn't have been copied on mob conversion");
		}

		IntSupplier attached = converted.getAttached(COPY_ON_DEATH);

		if (attached == null || attached.getAsInt() != 42) {
			throw new GameTestAssertException("Attachment copying failed during mob conversion");
		}

		converted.discard();
		context.succeed();
	}
}
