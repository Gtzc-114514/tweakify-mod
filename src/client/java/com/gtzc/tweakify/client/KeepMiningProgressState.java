package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.world.item.Item;

/**
 * 切换物品不打断挖掘：判定当前挖掘进度是否继续保留。
 * 距离限制只约束“同一个方块”，0 表示不限制（与旧行为一致）。
 */
public final class KeepMiningProgressState {
	private static Item currentTool;

	private KeepMiningProgressState() {
	}

	public static boolean shouldKeep(boolean enabled, boolean sameBlock, double distanceSq, int maxDistance) {
		return shouldKeep(enabled, sameBlock, true, distanceSq, maxDistance);
	}

	public static boolean shouldKeep(boolean enabled, boolean sameBlock, boolean sameTool,
			double distanceSq, int maxDistance) {
		if (!enabled || !sameBlock || !sameTool) {
			return false;
		}
		if (maxDistance <= 0) {
			return true;
		}
		int limit = Math.min(TweakifyModConfig.MAX_KEEP_MINING_PROGRESS_DISTANCE, maxDistance);
		return distanceSq <= (double) limit * limit;
	}

	// 记录开始挖掘时的工具；没有记录时视为同一工具，避免首次调用误判。
	public static void recordTool(Item item) {
		currentTool = item;
	}

	public static boolean isSameTool(Item item) {
		return currentTool == null || currentTool == item;
	}
}
