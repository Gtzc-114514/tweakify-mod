package com.gtzc.tweakify.client.state;

import com.gtzc.tweakify.config.TweakifyModConfig;

/**
 * 挖掘惩罚保留强度：0% 完全去掉惩罚，100% 保持原版。
 * 水下按乘法系数插值（原版 0.2 → 1.0），空中按除数插值（原版 5 → 1）。
 */
public final class MiningPenaltyState {
	private MiningPenaltyState() {
	}

	public static int clampKeepPercent(int keepPercent) {
		return Math.max(TweakifyModConfig.MIN_MINING_PENALTY_KEEP_PERCENT,
			Math.min(TweakifyModConfig.MAX_MINING_PENALTY_KEEP_PERCENT, keepPercent));
	}

	// 水下惩罚只在游泳时处理；未开启“仅游泳”时保持旧行为。
	public static boolean shouldRemoveUnderwaterPenalty(boolean swimmingOnly, boolean swimming) {
		return !swimmingOnly || swimming;
	}

	// 属性值 ≥ 1 时视为增益（如其它模组加成），保持原值不动。
	public static double underwaterMultiplier(double original, int keepPercent) {
		if (original >= 1.0) {
			return original;
		}
		double keep = clampKeepPercent(keepPercent) / 100.0;
		return original + (1.0 - original) * (1.0 - keep);
	}

	// 原版离地时把速度除以 5；保留百分比越高，除数越接近 5。
	public static float airDivisor(float original, int keepPercent) {
		if (original <= 1.0f) {
			return original;
		}
		double keep = clampKeepPercent(keepPercent) / 100.0;
		double removed = (original - 1.0) * (1.0 - keep);
		return (float) (original - removed);
	}
}
