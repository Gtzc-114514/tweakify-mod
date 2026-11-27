package com.gtzc.tweakify.client.state;

import com.gtzc.tweakify.config.TweakifyModConfig;

/**
 * 禁用床太远：0 表示不限制距离（旧行为），大于 0 时只放宽到指定格数。
 */
public final class BedDistanceRules {
	private BedDistanceRules() {
	}

	public static boolean shouldAllowSleep(boolean enabled, boolean vanillaInRange, double distanceSq,
										   int maxDistance) {
		if (!enabled) {
			return vanillaInRange;
		}
		if (vanillaInRange) {
			return true;
		}
		if (maxDistance <= 0) {
			return true;
		}
		int limit = Math.min(TweakifyModConfig.MAX_BED_TOO_FAR_DISTANCE, maxDistance);
		return distanceSq <= (double) limit * limit;
	}
}
