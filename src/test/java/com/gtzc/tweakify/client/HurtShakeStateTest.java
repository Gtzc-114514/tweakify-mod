package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 受伤抖动的死亡倾斜：公式必须与原版 bobHurt 一致，否则“保留死亡镜头倾斜”会得到错误角度。
 */
class HurtShakeStateTest {

	@Test
	void deathTiltMatchesVanillaCurve() {
		assertEquals(0.0f, HurtShakeState.deathTiltDegrees(0.0f), 1.0e-5f);
		assertEquals(40.0f - 8000.0f / 220.0f, HurtShakeState.deathTiltDegrees(20.0f), 1.0e-5f);
	}

	@Test
	void deathTimeIsClampedToTwentyTicks() {
		assertEquals(HurtShakeState.deathTiltDegrees(20.0f), HurtShakeState.deathTiltDegrees(40.0f), 1.0e-5f,
			"deathTime 超过 20 后与原版一样不再变化");
	}
}
