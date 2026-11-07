package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.NightVisionCurve;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
	// 受伤抖动由 bobHurt 单独施加在视角矩阵上，取消它不会影响行走晃动（bobView）。
	@Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
	private void tweakify$disableHurtShake(CallbackInfo ci) {
		if (TweakifyMod.CONFIG != null && TweakifyMod.CONFIG.disableHurtShake) {
			ci.cancel();
		}
	}

	// 夜视淡出：原版在剩余时间内用正弦值闪烁，改为按剩余时间平滑淡出。
	@Inject(method = "nightVisionScale", at = @At("HEAD"), cancellable = true)
	private static void tweakify$fadeOutNightVision(LivingEntity entity, float partialTick,
												CallbackInfoReturnable<Float> cir) {
		if (TweakifyMod.CONFIG == null || !TweakifyMod.CONFIG.nightVisionFadeOut) {
			return;
		}
		MobEffectInstance effect = entity.getEffect(MobEffects.NIGHT_VISION);
		if (effect == null) {
			return;
		}
		int duration = TweakifyMod.CONFIG.nightVisionFadeOutDuration;
		if (duration <= 0 || !effect.endsWithin(duration)) {
			return;
		}
		float remaining = effect.getDuration() - partialTick;
		float t = Mth.clamp(remaining / (float) duration, 0.0f, 1.0f);
		if (NightVisionCurve.fromCode(TweakifyMod.CONFIG.nightVisionFadeOutCurve) == NightVisionCurve.LINEAR) {
			cir.setReturnValue(t);
			return;
		}
		// smoothstep：两端变化更慢，避免淡出开始/结束时出现明显的折角。
		cir.setReturnValue(t * t * (3.0f - 2.0f * t));
	}
}
