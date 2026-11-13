package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.SpawnFeatureState;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.SpawnData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 刷怪笼自定义 spawn rules 的提前和平判定：
 * 原版在 {@code BaseSpawner.serverTick} 中直接读取难度，早于
 * {@code SpawnPlacements} 与 {@code EntityType.create}，列表内生物会被提前跳过。
 * 这里只改该处难度，后续自定义规则的位置/光照检查仍会执行。
 */
@Mixin(BaseSpawner.class)
public class BaseSpawnerPeacefulMixin {
	@Shadow
	private SpawnData nextSpawnData;

	@Redirect(method = "serverTick", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/server/level/ServerLevel;getDifficulty()Lnet/minecraft/world/Difficulty;"))
	private Difficulty tweakify$allowCustomRulePeacefulSpawn(ServerLevel level) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null) {
			return level.getDifficulty();
		}
		EntityType<?> type = SpawnFeatureState.spawnDataType(this.nextSpawnData);
		if (type == null) {
			return level.getDifficulty();
		}
		return SpawnFeatureState.peacefulSpawnDifficulty(level.getDifficulty(),
			config.functions.peacefulModeMonsters, config.functions.peacefulModeMonsterSpawn,
			config.isPeacefulModeMonster(type));
	}
}
