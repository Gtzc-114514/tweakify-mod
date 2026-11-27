package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.SpectatorOverlayState;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MC-217716：进入旁观者模式后仍渲染反胃与下界传送门覆盖层。
 *
 * <p>26.3 在 extractPlayerState 中把两类效果强度写入 PlayerRenderState；
 * 这里在方法返回时对旁观者清零，GameRenderer 的 max(portal, nausea) 随之为 0，
 * 不会进入屏幕扭曲分支。其它游戏模式与配置关闭时完全不介入。
 */
@Mixin(LevelExtractor.class)
public abstract class LevelExtractorSpectatorOverlayMixin {
	@Unique
	private static final AtomicBoolean TWEAKIFY$SPECTATOR_OVERLAY_PROBE = new AtomicBoolean();

	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "extractPlayerState", at = @At("RETURN"), require = 0)
	private void tweakify$hideSpectatorOverlays(Camera camera, DeltaTracker deltaTracker, float worldPartialTicks,
			PlayerRenderState state, CallbackInfo ci) {
		TweakifyModConfig config = TweakifyMod.config();
		boolean enabled = config != null && config.fix.spectatorOverlayFix;
		boolean spectator = this.minecraft.player != null && this.minecraft.player.isSpectator();
		if (!SpectatorOverlayState.shouldHideOverlays(enabled, spectator)) {
			return;
		}
		HookProbe.markOnce(TWEAKIFY$SPECTATOR_OVERLAY_PROBE, "spectatorOverlayFix");
		if (config.fix.spectatorOverlayHidePortal) {
			state.portalEffectIntensity = SpectatorOverlayState.intensity(enabled, spectator,
				state.portalEffectIntensity);
		}
		if (config.fix.spectatorOverlayHideNausea) {
			state.nauseaEffectIntensity = SpectatorOverlayState.intensity(enabled, spectator,
				state.nauseaEffectIntensity);
		}
		if (config.fix.spectatorOverlayHideSpinning) {
			state.spinningEffectAngle = SpectatorOverlayState.intensity(enabled, spectator,
				state.spinningEffectAngle);
		}
	}
}
