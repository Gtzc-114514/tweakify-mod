package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.SpawnFeatureState;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 和平模式怪物：关闭“不攻击玩家”时，列表中的怪物造成的伤害不再被和平难度清零。
 *
 * <p>原版 {@code Player.hurtServer} 在和平难度下把所有伤害改成 0；
 * 这里只对列表中怪物造成的伤害返回普通难度，其它伤害保持原版和平规则。
 */
@Mixin(Player.class)
public class PlayerPeacefulDamageMixin {
	@Redirect(method = "hurtServer", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/server/level/ServerLevel;getDifficulty()Lnet/minecraft/world/Difficulty;"))
	private Difficulty tweakify$allowListedMobDamage(ServerLevel level, ServerLevel targetLevel,
													  DamageSource source, float amount) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config != null && level.getDifficulty() == Difficulty.PEACEFUL) {
			Entity attacker = source.getEntity();
			if (attacker instanceof Mob mob && SpawnFeatureState.shouldAllowPeacefulAggression(
				config.functions.peacefulModeMonsters, config.functions.peacefulModeMonsterNoAttack, true,
				config.isPeacefulModeMonster(mob.getType()))) {
				return Difficulty.NORMAL;
			}
		}
		return level.getDifficulty();
	}
}
