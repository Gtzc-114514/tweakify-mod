package com.gtzc.tweakify.client.render;

import com.gtzc.tweakify.config.TweakifyModConfig;

/**
 * MiniHUD 掉帧优化：把刷新间隔（毫秒）换算成纳秒，便于注入点比较。
 */
public final class MiniHudState {
	private MiniHudState() {
	}

	public static long intervalNanos(int intervalMs) {
		int clamped = Math.max(TweakifyModConfig.MIN_MINIHUD_REFRESH_INTERVAL_MS,
			Math.min(TweakifyModConfig.MAX_MINIHUD_REFRESH_INTERVAL_MS, intervalMs));
		return clamped * 1_000_000L;
	}
}
