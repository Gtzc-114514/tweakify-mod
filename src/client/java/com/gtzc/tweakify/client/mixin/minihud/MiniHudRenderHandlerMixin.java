package com.gtzc.tweakify.client.mixin.minihud;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.minihud.event.RenderHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * MiniHUD 掉帧优化：原版每 50ms 在渲染线程重新计算全部 HUD 行。
 * 这里把刷新间隔放宽到 150ms，仍然留在渲染线程执行，避免后台线程与
 * MiniHUD 的 InfoLineChunkCache / 客户端状态发生并发访问。
 */
@Mixin(RenderHandler.class)
public class MiniHudRenderHandlerMixin {
	@Shadow
	private void updateLines() {
		throw new AssertionError("Mixin shadow not applied");
	}

	@Unique
	private static final long TWEAKIFY$UPDATE_INTERVAL_NANOS = 150_000_000L;

	@Unique
	private static volatile long tweakify$lastUpdateNanos;

	@Redirect(method = "onExtractGuiOverlayPost", at = @At(value = "INVOKE",
		target = "Lfi/dy/masa/minihud/event/RenderHandler;updateLines()V"))
	private void tweakify$throttleLineUpdate(RenderHandler handler) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.optimization.miniHudFpsOptimization) {
			this.updateLines();
			return;
		}

		long now = System.nanoTime();
		if (now - tweakify$lastUpdateNanos < TWEAKIFY$UPDATE_INTERVAL_NANOS) {
			return;
		}
		tweakify$lastUpdateNanos = now;
		this.updateLines();
	}
}
