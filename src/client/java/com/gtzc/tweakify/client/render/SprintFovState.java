package com.gtzc.tweakify.client.render;

import com.gtzc.tweakify.config.TweakifyModConfig;

/**
 * 禁用疾跑 FOV：原版冲刺会附加移速修饰符，getFieldOfViewModifier 按移速比抬高视野。
 * 这里按配置百分比扣掉这部分加成：0 = 完全禁用（旧行为），100 = 保留原版。
 */
public final class SprintFovState {
	private static final float SPRINT_FOV_BONUS = 0.15f;

	private SprintFovState() {
	}

	public static float adjust(float original, float effectScale, int keepPercent) {
		int clamped = Math.max(TweakifyModConfig.MIN_SPRINT_FOV_INTENSITY,
			Math.min(TweakifyModConfig.MAX_SPRINT_FOV_INTENSITY, keepPercent));
		float removed = effectScale * SPRINT_FOV_BONUS * (100 - clamped) / 100.0f;
		return Math.max(0.1f, original - removed);
	}
}
