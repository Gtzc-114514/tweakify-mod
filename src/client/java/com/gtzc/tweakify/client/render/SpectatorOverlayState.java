package com.gtzc.tweakify.client.render;

/**
 * 旁观者覆盖层修复（MC-217716）的纯函数。
 *
 * <p>旁观者模式不应显示反胃或下界传送门造成的屏幕扭曲；
 * 只在该模式下清零渲染状态，其它游戏模式保持原版。
 */
public final class SpectatorOverlayState {
	private SpectatorOverlayState() {
	}

	public static boolean shouldHideOverlays(boolean enabled, boolean spectator) {
		return enabled && spectator;
	}

	// 隐藏时把强度归零；否则保留原强度，供门户/反胃/旋转三个状态共用。
	public static float intensity(boolean enabled, boolean spectator, float original) {
		return shouldHideOverlays(enabled, spectator) ? 0.0F : original;
	}
}
