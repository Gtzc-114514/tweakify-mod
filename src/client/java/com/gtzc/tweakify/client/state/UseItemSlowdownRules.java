package com.gtzc.tweakify.client.state;

import com.gtzc.tweakify.config.TweakifyModConfig;

/**
 * 使用物品减速的保留比例：0% = 完全取消（旧行为），100% = 与原版相同。
 */
public final class UseItemSlowdownRules {
	private UseItemSlowdownRules() {
	}

	public static float multiplier(boolean enabled, int keepPercent, float original) {
		if (!enabled) {
			return original;
		}
		int keep = Math.max(TweakifyModConfig.MIN_USE_ITEM_SLOWDOWN_KEEP_PERCENT,
			Math.min(TweakifyModConfig.MAX_USE_ITEM_SLOWDOWN_KEEP_PERCENT, keepPercent));
		if (keep >= TweakifyModConfig.MAX_USE_ITEM_SLOWDOWN_KEEP_PERCENT) {
			return original;
		}
		if (keep <= 0) {
			return 1.0F;
		}
		return (float) (original + (1.0 - original) * (keep / 100.0));
	}
}
