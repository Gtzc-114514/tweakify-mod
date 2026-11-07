package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.renderer.fog.environment.LavaFogEnvironment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 禁用屏幕覆盖层：禁用岩浆雾（同 WaterFogEnvironmentMixin，让其不适用后退回大气雾）。
 */
@Mixin(LavaFogEnvironment.class)
public class LavaFogEnvironmentMixin {
	@Inject(method = "isApplicable", at = @At("HEAD"), cancellable = true)
	private void tweakify$disableLavaFog(FogType fogType, Entity entity, CallbackInfoReturnable<Boolean> cir) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config != null && config.disableOverlayEffects && config.disableLavaFog) {
			cir.setReturnValue(false);
		}
	}
}
