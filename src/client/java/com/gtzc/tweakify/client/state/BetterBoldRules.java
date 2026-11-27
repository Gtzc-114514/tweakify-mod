package com.gtzc.tweakify.client.state;

import com.gtzc.tweakify.config.TweakifyModConfig;

/**
 * 更好的粗体：把原版 1.0 像素偏移替换为配置百分比（默认 50% = 0.5 像素）。
 */
public final class BetterBoldRules {
	private BetterBoldRules() {
	}

	public static float offset(boolean enabled, int percent, float original) {
		if (!enabled) {
			return original;
		}
		int clamped = Math.max(TweakifyModConfig.MIN_BETTER_BOLD_OFFSET_PERCENT,
			Math.min(TweakifyModConfig.MAX_BETTER_BOLD_OFFSET_PERCENT, percent));
		return clamped / 100.0F;
	}
}
