package com.gtzc.tweakify.client;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.SpawnData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 生成控制与和平模式判定的纯函数：总开关、子开关、列表与目标条件缺一不可。
 */
class SpawnFeatureStateTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		// allSpawnPotentialsListed 会把 SpawnData 解析成注册表实体类型，需要先注册原版内容。
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}
	@Test
	void worldSpawnReasonsAreBlockable() {
		assertTrue(SpawnFeatureState.isWorldSpawnReason(EntitySpawnReason.NATURAL));
		assertTrue(SpawnFeatureState.isWorldSpawnReason(EntitySpawnReason.CHUNK_GENERATION));
		assertTrue(SpawnFeatureState.isWorldSpawnReason(EntitySpawnReason.SPAWNER));
		assertTrue(SpawnFeatureState.isWorldSpawnReason(EntitySpawnReason.TRIAL_SPAWNER));
		assertTrue(SpawnFeatureState.isWorldSpawnReason(EntitySpawnReason.STRUCTURE));
		assertTrue(SpawnFeatureState.isWorldSpawnReason(EntitySpawnReason.PATROL));
		assertTrue(SpawnFeatureState.isWorldSpawnReason(EntitySpawnReason.REINFORCEMENT));
		assertTrue(SpawnFeatureState.isWorldSpawnReason(EntitySpawnReason.JOCKEY));
		assertTrue(SpawnFeatureState.isWorldSpawnReason(EntitySpawnReason.EVENT));
		assertTrue(SpawnFeatureState.isWorldSpawnReason(EntitySpawnReason.MOB_SUMMONED));
		assertFalse(SpawnFeatureState.isWorldSpawnReason(EntitySpawnReason.SPAWN_ITEM_USE));
		assertFalse(SpawnFeatureState.isWorldSpawnReason(EntitySpawnReason.COMMAND));
		assertFalse(SpawnFeatureState.isWorldSpawnReason(EntitySpawnReason.BREEDING));
		assertFalse(SpawnFeatureState.isWorldSpawnReason(EntitySpawnReason.TRIGGERED));
	}

	@Test
	void playerSpawnReasonsAreRecognized() {
		assertTrue(SpawnFeatureState.isPlayerSpawnReason(EntitySpawnReason.SPAWN_ITEM_USE));
		assertTrue(SpawnFeatureState.isPlayerSpawnReason(EntitySpawnReason.COMMAND));
		assertTrue(SpawnFeatureState.isPlayerSpawnReason(EntitySpawnReason.BREEDING));
		assertTrue(SpawnFeatureState.isPlayerSpawnReason(EntitySpawnReason.BUCKET));
		assertTrue(SpawnFeatureState.isPlayerSpawnReason(EntitySpawnReason.DISPENSER));
		assertFalse(SpawnFeatureState.isPlayerSpawnReason(EntitySpawnReason.NATURAL));
		assertFalse(SpawnFeatureState.isPlayerSpawnReason(EntitySpawnReason.TRIGGERED));
	}

	@Test
	void blockSpawnRespectsPlayerSpawnToggle() {
		assertTrue(SpawnFeatureState.shouldBlockSpawn(true, true, true, EntitySpawnReason.NATURAL,
			EntityTypes.ZOMBIE));
		assertTrue(SpawnFeatureState.shouldBlockSpawn(true, false, true, EntitySpawnReason.STRUCTURE,
			EntityTypes.ZOMBIE));
		assertFalse(SpawnFeatureState.shouldBlockSpawn(false, false, true, EntitySpawnReason.NATURAL,
			EntityTypes.ZOMBIE));
		assertFalse(SpawnFeatureState.shouldBlockSpawn(true, false, false, EntitySpawnReason.NATURAL,
			EntityTypes.ZOMBIE));
		assertFalse(SpawnFeatureState.shouldBlockSpawn(true, true, true, EntitySpawnReason.COMMAND,
			EntityTypes.ZOMBIE));
		assertTrue(SpawnFeatureState.shouldBlockSpawn(true, false, true, EntitySpawnReason.COMMAND,
			EntityTypes.ZOMBIE));
		assertFalse(SpawnFeatureState.shouldBlockSpawn(true, false, true, EntitySpawnReason.LOAD,
			EntityTypes.ZOMBIE));
	}

	@Test
	void eventSourceOptInTypesOnlyBlockWhenListed() {
		for (EntityType<?> type : List.of(EntityTypes.IRON_GOLEM, EntityTypes.WANDERING_TRADER,
			EntityTypes.ENDER_DRAGON)) {
			assertTrue(SpawnFeatureState.isEventSourceOptInType(type));
			assertFalse(SpawnFeatureState.shouldBlockSpawn(true, true, false, EntitySpawnReason.EVENT, type));
			assertTrue(SpawnFeatureState.shouldBlockSpawn(true, true, true, EntitySpawnReason.EVENT, type));
			assertFalse(SpawnFeatureState.shouldBlockSpawn(true, true, false, EntitySpawnReason.MOB_SUMMONED, type));
			assertTrue(SpawnFeatureState.shouldBlockSpawn(true, true, true, EntitySpawnReason.MOB_SUMMONED, type));
		}
	}

	@Test
	void peacefulSpawnRequiresFeatureSpawnSwitchAndList() {
		assertTrue(SpawnFeatureState.shouldAllowPeacefulSpawn(true, true, true));
		assertFalse(SpawnFeatureState.shouldAllowPeacefulSpawn(false, true, true));
		assertFalse(SpawnFeatureState.shouldAllowPeacefulSpawn(true, false, true));
		assertFalse(SpawnFeatureState.shouldAllowPeacefulSpawn(true, true, false));
	}

	@Test
	void spawnerGateRequiresFeatureAndSpawnSwitch() {
		assertTrue(SpawnFeatureState.shouldAllowPeacefulSpawner(true, true, true));
		assertFalse(SpawnFeatureState.shouldAllowPeacefulSpawner(false, true, true));
		assertFalse(SpawnFeatureState.shouldAllowPeacefulSpawner(true, false, true));
		assertFalse(SpawnFeatureState.shouldAllowPeacefulSpawner(true, true, false));
	}

	@Test
	void trialSpawnPotentialsMustNotBeEmpty() {
		assertFalse(SpawnFeatureState.allSpawnPotentialsListed(null, type -> true));
		assertFalse(SpawnFeatureState.allSpawnPotentialsListed(WeightedList.of(), type -> true));
	}

	@Test
	void trialSpawnPotentialsMatchRealSpawnData() {
		SpawnData zombie = spawnData("minecraft:zombie");
		assertTrue(SpawnFeatureState.allSpawnPotentialsListed(
			WeightedList.of(zombie), type -> type == EntityTypes.ZOMBIE));
		assertFalse(SpawnFeatureState.allSpawnPotentialsListed(
			WeightedList.of(zombie), type -> type == EntityTypes.SKELETON));
		// 生成池部分命中按未命中处理，避免试炼刷怪笼进入 ACTIVE 后卡住。
		assertFalse(SpawnFeatureState.allSpawnPotentialsListed(
			WeightedList.of(zombie, spawnData("minecraft:skeleton")),
			type -> type == EntityTypes.ZOMBIE));
		assertFalse(SpawnFeatureState.allSpawnPotentialsListed(
			WeightedList.of(spawnData("minecraft:unknown_mob")), type -> true));
	}

	private static SpawnData spawnData(String entityId) {
		SpawnData data = new SpawnData();
		data.getEntityToSpawn().putString("id", entityId);
		return data;
	}

	@Test
	void peacefulStayRequiresFeatureAndList() {
		assertTrue(SpawnFeatureState.shouldStayInPeaceful(true, true));
		assertFalse(SpawnFeatureState.shouldStayInPeaceful(false, true));
		assertFalse(SpawnFeatureState.shouldStayInPeaceful(true, false));
	}

	@Test
	void preventAttackRequiresAllConditions() {
		assertTrue(SpawnFeatureState.shouldPreventAttack(true, true, true, true, true));
		assertFalse(SpawnFeatureState.shouldPreventAttack(false, true, true, true, true));
		assertFalse(SpawnFeatureState.shouldPreventAttack(true, false, true, true, true));
		assertFalse(SpawnFeatureState.shouldPreventAttack(true, true, false, true, true));
		assertFalse(SpawnFeatureState.shouldPreventAttack(true, true, true, false, true));
		assertFalse(SpawnFeatureState.shouldPreventAttack(true, true, true, true, false));
	}

	@Test
	void peacefulSpawnDifficultyOnlyChangesPeacefulForConfiguredMobs() {
		assertEquals(Difficulty.NORMAL, SpawnFeatureState.peacefulSpawnDifficulty(
			Difficulty.PEACEFUL, true, true, true));
		assertEquals(Difficulty.PEACEFUL, SpawnFeatureState.peacefulSpawnDifficulty(
			Difficulty.PEACEFUL, false, true, true));
		assertEquals(Difficulty.PEACEFUL, SpawnFeatureState.peacefulSpawnDifficulty(
			Difficulty.PEACEFUL, true, false, true));
		assertEquals(Difficulty.PEACEFUL, SpawnFeatureState.peacefulSpawnDifficulty(
			Difficulty.PEACEFUL, true, true, false));
		assertEquals(Difficulty.HARD, SpawnFeatureState.peacefulSpawnDifficulty(
			Difficulty.HARD, true, true, true));
	}

	@Test
	void peacefulAggressionOnlyAppliesInPeaceful() {
		assertTrue(SpawnFeatureState.shouldAllowPeacefulAggression(true, false, true, true));
		assertFalse(SpawnFeatureState.shouldAllowPeacefulAggression(false, false, true, true));
		assertFalse(SpawnFeatureState.shouldAllowPeacefulAggression(true, true, true, true));
		assertFalse(SpawnFeatureState.shouldAllowPeacefulAggression(true, false, false, true));
		assertFalse(SpawnFeatureState.shouldAllowPeacefulAggression(true, false, true, false));
	}
}
