package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.SpawnFeatureState;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 和平模式怪物：
 * 1. 列表中的生物不会被和平难度自动 discard；
 * 2. 列表中的生物在和平难度下不会锁定玩家目标。
 */
@Mixin(Mob.class)
public class MobPeacefulMixin {
	@Shadow
	private LivingEntity target;

	@Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
	private void tweakify$blockPeacefulTarget(LivingEntity newTarget, CallbackInfo ci) {
		if (shouldPreventPlayerTarget((Mob) (Object) this, newTarget)) {
			this.target = null;
			ci.cancel();
		}
	}

	@ModifyReturnValue(method = "getTarget", at = @At("RETURN"))
	private LivingEntity tweakify$hidePeacefulTarget(LivingEntity original) {
		return shouldPreventPlayerTarget((Mob) (Object) this, original) ? null : original;
	}

	@ModifyReturnValue(method = "getTargetUnchecked", at = @At("RETURN"))
	private LivingEntity tweakify$hidePeacefulTargetUnchecked(LivingEntity original) {
		return shouldPreventPlayerTarget((Mob) (Object) this, original) ? null : original;
	}

	// 原版 checkDespawn：和平难度下 !isAllowedInPeaceful 的实体会被 discard。
	// 列表中的生物返回 true，保留下来，其余实体保持原版。
	@Redirect(method = "checkDespawn", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/EntityType;isAllowedInPeaceful()Z"))
	private boolean tweakify$keepPeacefulMonster(EntityType<?> type) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config != null && SpawnFeatureState.shouldStayInPeaceful(
			config.functions.peacefulModeMonsters, config.isPeacefulModeMonster(type))) {
			return true;
		}
		return type.isAllowedInPeaceful();
	}

	private static boolean shouldPreventPlayerTarget(Mob mob, LivingEntity target) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || target == null) {
			return false;
		}
		return SpawnFeatureState.shouldPreventAttack(config.functions.peacefulModeMonsters,
			config.functions.peacefulModeMonsterNoAttack,
			mob.level().getDifficulty() == Difficulty.PEACEFUL,
			config.isPeacefulModeMonster(mob.getType()),
			target instanceof Player);
	}
}
