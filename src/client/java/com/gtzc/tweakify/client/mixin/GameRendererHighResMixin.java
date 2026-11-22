package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.BetterScreenshotManager;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * F9 4K 离屏渲染：统计真正完成的 GameRenderer 帧，避免低帧率下按 tick
 * 计数过早抓取刚分配、尚未绘制的 4K 目标。
 */
@Mixin(GameRenderer.class)
public class GameRendererHighResMixin {
	@Inject(method = "render", at = @At("TAIL"))
	private void tweakify$countRenderedFrame(CallbackInfo ci) {
		BetterScreenshotManager.onHighResolutionFrameRendered();
	}
}
