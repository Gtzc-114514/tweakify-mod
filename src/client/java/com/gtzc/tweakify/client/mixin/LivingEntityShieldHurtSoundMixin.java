package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.state.ShieldHurtSoundRules;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MC-105068：举盾完全挡下攻击时仍播放玩家受伤音效。
 *
 * <p>26.3 的 hurtServer 已把格挡后的伤害扣到 0，但仍走到 playHurtSound；
 * 这里只在该调用点检查“触发格挡且剩余伤害为 0”，跳过受伤音效，
 * 盾牌的格挡/禁用音效仍由原版路径播放。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityShieldHurtSoundMixin {
	@Unique
	private static final AtomicBoolean TWEAKIFY$SHIELD_HURT_SOUND_PROBE = new AtomicBoolean();

	@WrapOperation(method = "hurtServer", require = 0,
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/LivingEntity;playHurtSound("
				+ "Lnet/minecraft/world/damagesource/DamageSource;)V"))
	private void tweakify$suppressFullyBlockedHurtSound(LivingEntity instance, DamageSource source,
			Operation<Void> original, @Local(argsOnly = true) float damage,
			@Local(index = 7) boolean blocked) {
		TweakifyModConfig config = TweakifyMod.config();
		boolean enabled = config.fix.shieldBlockHurtSoundFix;
		if (enabled) {
			HookProbe.markOnce(TWEAKIFY$SHIELD_HURT_SOUND_PROBE, "shieldBlockHurtSoundFix");
		}
		boolean playersOnly = config.fix.shieldBlockHurtSoundFixPlayersOnly;
		if (ShieldHurtSoundRules.shouldPlayHurtSound(enabled, playersOnly,
			instance instanceof Player, blocked, damage)) {
			original.call(instance, source);
		}
	}
}
