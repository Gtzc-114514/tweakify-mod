package com.gtzc.tweakify.client.render;

/**
 * 冷却指示器闪烁修复（MC-268420）的纯函数。
 *
 * <p>高攻速下切换物品时攻击强度会瞬间归零，原版仍以宽度 0 绘制进度精灵，
 * 形成一帧闪烁。开启时只在进度大于 0 时绘制，关闭时完全保持原版。
 */
public final class CooldownIndicatorState {
	private CooldownIndicatorState() {
	}

	public static boolean shouldDrawProgress(boolean enabled, float attackStrengthScale) {
		return shouldDrawProgress(enabled, 0, attackStrengthScale);
	}

	// 带最小像素进度的版本：低于阈值的背景条也跳过。
	public static boolean shouldDrawProgress(boolean enabled, int minProgress, float attackStrengthScale) {
		if (!enabled) {
			return true;
		}
		return attackStrengthScale * 17.0F > Math.max(0, minProgress);
	}
}
