package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.SpawnFeatureState;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.storage.LevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 地狱门随机刻生成僵尸猪灵：原版有两处独立难度判断，
 * 第一处直接跳过和平难度，第二处把难度作为随机概率上限。
 * 列表内包含僵尸猪灵且和平生成开启时，两处都按普通难度处理。
 */
@Mixin(NetherPortalBlock.class)
public class NetherPortalBlockPeacefulMixin {
	@Redirect(method = "randomTick", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/level/storage/LevelData;getDifficulty()Lnet/minecraft/world/Difficulty;"))
	private Difficulty tweakify$allowPeacefulPortalSpawn(LevelData data) {
		Difficulty current = data.getDifficulty();
		if (current != Difficulty.PEACEFUL) {
			return current;
		}
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null) {
			return current;
		}
		return SpawnFeatureState.peacefulSpawnDifficulty(current,
			config.functions.peacefulModeMonsters, config.functions.peacefulModeMonsterSpawn,
			config.isPeacefulModeMonster(EntityTypes.ZOMBIFIED_PIGLIN));
	}

	@Redirect(method = "randomTick", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/server/level/ServerLevel;getDifficulty()Lnet/minecraft/world/Difficulty;"))
	private Difficulty tweakify$allowPeacefulPortalChance(ServerLevel level) {
		Difficulty current = level.getDifficulty();
		if (current != Difficulty.PEACEFUL) {
			return current;
		}
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null) {
			return current;
		}
		return SpawnFeatureState.peacefulSpawnDifficulty(current,
			config.functions.peacefulModeMonsters, config.functions.peacefulModeMonsterSpawn,
			config.isPeacefulModeMonster(EntityTypes.ZOMBIFIED_PIGLIN));
	}
}
