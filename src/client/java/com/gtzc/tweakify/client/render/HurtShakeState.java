package com.gtzc.tweakify.client.render;

/**
 * 受伤抖动状态：为“保留死亡镜头倾斜”提供与原版一致的死亡视角角度。
 * 原版在 bobHurt 里先按死亡时间施加镜头倾斜，再叠加受伤抖动；
 * 完全取消 bobHurt 会连死亡倾斜一起清掉，这里把死亡部分单独算出来。
 */
public final class HurtShakeState {
	private HurtShakeState() {
	}

	// 原版公式：40 - 8000 / (min(deathTime, 20) + 200)。
	public static float deathTiltDegrees(float deathTime) {
		float clamped = Math.min(deathTime, 20.0f);
		return 40.0f - 8000.0f / (clamped + 200.0f);
	}
}
