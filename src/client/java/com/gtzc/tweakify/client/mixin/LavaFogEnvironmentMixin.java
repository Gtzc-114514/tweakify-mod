package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.fog.environment.LavaFogEnvironment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 禁用屏幕覆盖层：禁用岩浆雾（同 WaterFogEnvironmentMixin，让其不适用后退回大气雾）。
 */
@Mixin(LavaFogEnvironment.class)
public class LavaFogEnvironmentMixin {
	@ModifyReturnValue(method = "isApplicable", at = @At("RETURN"))
	private boolean tweakify$disableLavaFog(boolean original, FogType fogType, Entity entity) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		return config != null && config.disableOverlayEffects && config.disableLavaFog ? false : original;
	}
}
