package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.fog.environment.PowderedSnowFogEnvironment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 禁用屏幕覆盖层：禁用细雪雾（同 WaterFogEnvironmentMixin，让其不适用后退回大气雾）。
 */
@Mixin(PowderedSnowFogEnvironment.class)
public class PowderedSnowFogEnvironmentMixin {
	@ModifyReturnValue(method = "isApplicable", at = @At("RETURN"))
	private boolean tweakify$disablePowderSnowFog(boolean original, FogType fogType, Entity entity) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		return config != null && config.disableOverlayEffects && config.disablePowderSnowFog ? false : original;
	}
}
