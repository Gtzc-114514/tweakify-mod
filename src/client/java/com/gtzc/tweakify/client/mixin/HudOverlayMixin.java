package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 屏幕特效开关：
 * 1. 禁用地狱门特效的覆盖层部分；
 * 2. 禁用细雪冰冻的白色描边覆盖层。
 */
@Mixin(Hud.class)
public class HudOverlayMixin {
	@Shadow
	@Final
	private static Identifier POWDER_SNOW_OUTLINE_LOCATION;

	// 紫色传送门覆盖层。
	@Inject(method = "extractPortalOverlay", at = @At("HEAD"), cancellable = true)
	private void tweakify$disablePortalOverlay(GuiGraphicsExtractor gui, float intensity, CallbackInfo ci) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config != null && config.modified.disableNetherPortalEffect && config.modified.disableNetherPortalOverlay) {
			ci.cancel();
		}
	}

	// 细雪冰冻的白色描边：只拦截细雪这一张贴图，保留南瓜头等其它相机覆盖层。
	@Inject(method = "extractTextureOverlay", at = @At("HEAD"), cancellable = true)
	private void tweakify$disablePowderSnowOverlay(GuiGraphicsExtractor gui, Identifier texture, float alpha,
												   CallbackInfo ci) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.modified.disableOverlayEffects || !config.modified.disablePowderSnowOverlay) {
			return;
		}
		if (POWDER_SNOW_OUTLINE_LOCATION.equals(texture)) {
			ci.cancel();
		}
	}
}
