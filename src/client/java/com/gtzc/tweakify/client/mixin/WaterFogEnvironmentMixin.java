package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.fog.environment.WaterFogEnvironment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 禁用屏幕覆盖层：禁用水下雾。
 *
 * <p>26.3 的雾是独立于屏幕覆盖层的系统，FogRenderer 会挑选第一个 isApplicable 的
 * FogEnvironment 来设置雾距离与颜色；这里让它不适用，就会退回大气雾（即无水下雾）。
 */
@Mixin(WaterFogEnvironment.class)
public class WaterFogEnvironmentMixin {
	@ModifyReturnValue(method = "isApplicable", at = @At("RETURN"))
	private boolean tweakify$disableWaterFog(boolean original, FogType fogType, Entity entity) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		return config != null && config.disableOverlayEffects && config.disableWaterFog ? false : original;
	}
}
