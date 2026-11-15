package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;

/**
 * 切换物品不打断挖掘：判定当前挖掘进度是否继续保留。
 * 距离限制只约束“同一个方块”，0 表示不限制（与旧行为一致）。
 */
public final class KeepMiningProgressState {
	private KeepMiningProgressState() {
	}

	public static boolean shouldKeep(boolean enabled, boolean sameBlock, double distanceSq, int maxDistance) {
		if (!enabled || !sameBlock) {
			return false;
		}
		if (maxDistance <= 0) {
			return true;
		}
		int limit = Math.min(TweakifyModConfig.MAX_KEEP_MINING_PROGRESS_DISTANCE, maxDistance);
		return distanceSq <= (double) limit * limit;
	}
}
