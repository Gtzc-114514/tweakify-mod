package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.renderer.fog.environment.PowderedSnowFogEnvironment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 禁用屏幕覆盖层：禁用细雪雾（同 WaterFogEnvironmentMixin，让其不适用后退回大气雾）。
 */
@Mixin(PowderedSnowFogEnvironment.class)
public class PowderedSnowFogEnvironmentMixin {
	@Inject(method = "isApplicable", at = @At("HEAD"), cancellable = true)
	private void tweakify$disablePowderSnowFog(FogType fogType, Entity entity, CallbackInfoReturnable<Boolean> cir) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config != null && config.disableOverlayEffects && config.disablePowderSnowFog) {
			cir.setReturnValue(false);
		}
	}
}
