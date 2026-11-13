package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 屏幕特效开关：
 * 1. 传送门镜头旋转：只把提交给渲染的强度清零，HUD 覆盖层仍由 LocalPlayer 的强度独立控制；
 * 2. 水下/岩浆覆盖层：直接清空对应的渲染状态。
 */
@Mixin(LevelExtractor.class)
public class LevelExtractorOverlayMixin {
	@Inject(method = "extractPlayerState", at = @At("TAIL"))
	private void tweakify$adjustPlayerOverlays(Camera camera, DeltaTracker deltaTracker, float partialTick,
											   PlayerRenderState state, CallbackInfo ci) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null) {
			return;
		}
		if (config.modified.disableNetherPortalEffect && config.modified.disableNetherPortalCamera) {
			state.portalEffectIntensity = 0.0f;
		}
		if (!config.modified.disableOverlayEffects) {
			return;
		}
		if (config.modified.disableWaterOverlay) {
			state.waterOverlay = null;
		}
		Minecraft client = Minecraft.getInstance();
		if (config.modified.disableLavaOverlay && client.player != null && client.player.isInLava()) {
			state.blockOverlay = null;
		}
	}
}
