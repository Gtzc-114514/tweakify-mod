package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.SpawnFeatureState;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 生成规则：
 * 1. 关闭指定生物生成：自然/结构/巡逻/增援/鸡骑士/袭击/唤魔者召唤/刷怪笼等
 *    世界生成调用 {@code checkSpawnRules} 时直接拒绝；关闭“允许玩家生成”后玩家来源也会拒绝；
 * 2. 和平模式怪物：对列表中的生物绕过和平难度的生成限制。
 */
@Mixin(SpawnPlacements.class)
public class SpawnPlacementsMixin {
	// 生成判定是纯检查；用返回值替换而不是 HEAD 取消，兼容其它模组对方法的包装。
	@ModifyReturnValue(method = "checkSpawnRules", at = @At("RETURN"))
	private static boolean tweakify$blockDisabledSpawn(boolean original, EntityType<?> type,
			ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config != null && SpawnFeatureState.shouldBlockSpawn(
			config.functions.disableSpecifiedMobSpawn, config.functions.disabledSpawnAllowPlayerSpawn,
			config.isSpawnDisabled(type), reason, type)) {
			return false;
		}
		return original;
	}

	// 原版：!isAllowedInPeaceful && 和平难度 -> 不允许生成。
	// 这里对列表中的生物返回 true，让后续放置检查继续执行。
	@Redirect(method = "checkSpawnRules", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/EntityType;isAllowedInPeaceful()Z"))
	private static boolean tweakify$allowPeacefulSpawn(EntityType<?> type) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config != null && SpawnFeatureState.shouldAllowPeacefulSpawn(
			config.functions.peacefulModeMonsters, config.functions.peacefulModeMonsterSpawn,
			config.isPeacefulModeMonster(type))) {
			return true;
		}
		return type.isAllowedInPeaceful();
	}
}
