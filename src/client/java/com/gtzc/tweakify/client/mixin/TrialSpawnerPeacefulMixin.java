package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.SpawnFeatureState;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 试炼刷怪笼的入口和平判断没有具体实体类型可用；
 * 只有生成池类型全部命中列表且功能/生成子开关开启时才放行，
 * 避免部分命中时状态机进入 ACTIVE 却无法完成。
 */
@Mixin(TrialSpawner.class)
public class TrialSpawnerPeacefulMixin {
	@Redirect(method = "canSpawnInLevel", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/server/level/ServerLevel;getDifficulty()Lnet/minecraft/world/Difficulty;"))
	private Difficulty tweakify$allowPeacefulTrialSpawner(ServerLevel level) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null) {
			return level.getDifficulty();
		}
		if (level.getDifficulty() == Difficulty.PEACEFUL) {
			TrialSpawner self = (TrialSpawner) (Object) this;
			boolean allListed = SpawnFeatureState.allSpawnPotentialsListed(
				self.activeConfig().spawnPotentialsDefinition(), config::isPeacefulModeMonster);
			if (SpawnFeatureState.shouldAllowPeacefulSpawner(
				config.functions.peacefulModeMonsters, config.functions.peacefulModeMonsterSpawn, allListed)) {
				return Difficulty.NORMAL;
			}
		}
		return level.getDifficulty();
	}
}
