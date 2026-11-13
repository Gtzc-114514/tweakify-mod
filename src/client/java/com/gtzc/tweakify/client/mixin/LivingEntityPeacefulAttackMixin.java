package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.SpawnFeatureState;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 和平模式怪物：关闭“不攻击玩家”时，列表中的怪物在和平难度下仍然可以攻击玩家。
 *
 * <p>原版 {@code LivingEntity.canAttack} 对和平难度下的玩家直接返回 false；
 * 这里只对该检查使用的难度值做重定向，不影响其它攻击条件（隐身、旁观者等）。
 */
@Mixin(LivingEntity.class)
public class LivingEntityPeacefulAttackMixin {
	@Redirect(method = "canAttack", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/level/Level;getDifficulty()Lnet/minecraft/world/Difficulty;"))
	private Difficulty tweakify$allowConfiguredPeacefulAttack(Level level) {
		if ((Object) this instanceof Mob mob) {
			TweakifyModConfig config = TweakifyMod.CONFIG;
			if (config != null && SpawnFeatureState.shouldAllowPeacefulAggression(
				config.functions.peacefulModeMonsters, config.functions.peacefulModeMonsterNoAttack,
				level.getDifficulty() == Difficulty.PEACEFUL, config.isPeacefulModeMonster(mob.getType()))) {
				return Difficulty.NORMAL;
			}
		}
		return level.getDifficulty();
	}
}
