package com.gtzc.tweakify.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.SpawnData;

import java.util.function.Predicate;

/**
 * 生成与和平模式判定的纯函数，便于单元测试与 mixin 共用。
 */
public final class SpawnFeatureState {
	private SpawnFeatureState() {
	}

	// 世界生成来源：自然、区块、结构、袭击/巡逻、增援、鸡骑士、唤魔者召唤与刷怪笼。
	public static boolean isWorldSpawnReason(EntitySpawnReason reason) {
		return reason == EntitySpawnReason.NATURAL
			|| reason == EntitySpawnReason.CHUNK_GENERATION
			|| reason == EntitySpawnReason.SPAWNER
			|| reason == EntitySpawnReason.TRIAL_SPAWNER
			|| reason == EntitySpawnReason.STRUCTURE
			|| reason == EntitySpawnReason.PATROL
			|| reason == EntitySpawnReason.REINFORCEMENT
			|| reason == EntitySpawnReason.JOCKEY
			|| reason == EntitySpawnReason.EVENT
			|| reason == EntitySpawnReason.MOB_SUMMONED;
	}

	// 玩家主动生成来源：刷怪蛋、命令、繁殖、桶与发射器。
	// TRIGGERED 同时包含玩家建造（傀儡/凋灵）与世界触发（骷髅陷阱/蠹虫等），
	// 无法安全归入任一来源，当前恒放行；需要时按具体调用点单独处理。
	public static boolean isPlayerSpawnReason(EntitySpawnReason reason) {
		return reason == EntitySpawnReason.SPAWN_ITEM_USE
			|| reason == EntitySpawnReason.COMMAND
			|| reason == EntitySpawnReason.BREEDING
			|| reason == EntitySpawnReason.BUCKET
			|| reason == EntitySpawnReason.DISPENSER;
	}

	// EVENT/MOB_SUMMONED 是宽来源，铁傀儡、流浪商人、末影龙等只在显式写入列表时拦截；
	// 其他世界来源仍按“列表命中”处理。
	public static boolean isEventSourceOptInType(EntityType<?> type) {
		return type == EntityTypes.IRON_GOLEM
			|| type == EntityTypes.WANDERING_TRADER
			|| type == EntityTypes.ENDER_DRAGON;
	}

	// 关闭指定生物生成：总开关与列表命中后拦截世界生成；
	// 玩家生成仅在“允许玩家生成”关闭时拦截。
	public static boolean shouldBlockSpawn(boolean featureEnabled, boolean allowPlayerSpawn,
									   boolean listed, EntitySpawnReason reason, EntityType<?> type) {
		if (!featureEnabled || !listed || reason == null) {
			return false;
		}
		if (isWorldSpawnReason(reason)) {
			if ((reason == EntitySpawnReason.EVENT || reason == EntitySpawnReason.MOB_SUMMONED)
				&& isEventSourceOptInType(type)) {
				return listed;
			}
			return true;
		}
		return isPlayerSpawnReason(reason) && !allowPlayerSpawn;
	}

	// 和平模式怪物：允许列表中的怪物在和平难度下生成。
	public static boolean shouldAllowPeacefulSpawn(boolean featureEnabled, boolean spawnEnabled, boolean listed) {
		return featureEnabled && spawnEnabled && listed;
	}

	// 试炼刷怪笼入口没有具体实体类型；只有生成池全部命中列表时才放行，
	// 避免部分命中时状态机进入 ACTIVE 却无法完成。
	public static boolean shouldAllowPeacefulSpawner(boolean featureEnabled, boolean spawnEnabled,
												 boolean allListed) {
		return featureEnabled && spawnEnabled && allListed;
	}

	// 解析 SpawnData 的实体 ID；失败时返回 null，调用方回退原版。
	public static EntityType<?> spawnDataType(SpawnData spawnData) {
		if (spawnData == null) {
			return null;
		}
		try {
			Identifier id = Identifier.tryParse(spawnData.getEntityToSpawn().getStringOr("id", ""));
			return id == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
		} catch (Exception | LinkageError e) {
			return null;
		}
	}

	// 试炼刷怪笼生成池必须全部命中列表；空池或未知实体按未命中处理。
	public static boolean allSpawnPotentialsListed(WeightedList<SpawnData> potentials,
												   Predicate<EntityType<?>> listed) {
		if (potentials == null || potentials.isEmpty() || listed == null) {
			return false;
		}
		for (Weighted<SpawnData> weighted : potentials.unwrap()) {
			EntityType<?> type = spawnDataType(weighted.value());
			if (type == null || !listed.test(type)) {
				return false;
			}
		}
		return true;
	}

	// 和平模式怪物：列表中的怪物不会被和平难度自动清除。
	public static boolean shouldStayInPeaceful(boolean featureEnabled, boolean listed) {
		return featureEnabled && listed;
	}

	// 和平模式怪物：列表中的怪物不会把玩家设为目标或攻击玩家。
	public static boolean shouldPreventAttack(boolean featureEnabled, boolean noAttackEnabled, boolean peaceful,
											  boolean listed, boolean targetIsPlayer) {
		return featureEnabled && noAttackEnabled && peaceful && listed && targetIsPlayer;
	}

	// 和平模式怪物：关闭“不攻击”后，列表中的怪物可在和平难度攻击/伤害玩家。
	// 必须同时确认当前是和平难度，避免影响普通/困难难度的伤害倍率。
	public static boolean shouldAllowPeacefulAggression(boolean featureEnabled, boolean noAttackEnabled,
													boolean peaceful, boolean listed) {
		return featureEnabled && !noAttackEnabled && peaceful && listed;
	}

	// 部分生物的自带 spawn rule 会再次硬编码“和平难度返回 false”。
	// 只在当前确实是和平、功能/生成子开关开启且列表命中时改为 NORMAL，
	// 普通/困难难度保持原值，不改变其它光照、空间与生物群系检查。
	public static Difficulty peacefulSpawnDifficulty(Difficulty current, boolean featureEnabled,
													boolean spawnEnabled, boolean listed) {
		if (current == Difficulty.PEACEFUL && shouldAllowPeacefulSpawn(featureEnabled, spawnEnabled, listed)) {
			return Difficulty.NORMAL;
		}
		return current;
	}
}
