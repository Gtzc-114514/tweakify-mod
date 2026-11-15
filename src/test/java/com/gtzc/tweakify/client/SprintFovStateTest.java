package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 疾跑 FOV 保留强度：0% 完全去掉加成，100% 保持原版，结果不低于原版下限 0.1。
 */
class SprintFovStateTest {

	@Test
	void keepPercentScalesRemovedBonus() {
		assertEquals(1.0f, SprintFovState.adjust(1.15f, 1.0f, 0), 1.0e-5f);
		assertEquals(1.075f, SprintFovState.adjust(1.15f, 1.0f, 50), 1.0e-5f);
		assertEquals(1.15f, SprintFovState.adjust(1.15f, 1.0f, 100), 1.0e-5f);
	}

	@Test
	void effectScaleScalesRemovedBonus() {
		assertEquals(1.075f, SprintFovState.adjust(1.15f, 0.5f, 0), 1.0e-5f,
			"effectScale 为 0.5 时只去掉一半加成");
	}

	@Test
	void resultNeverBelowVanillaFloor() {
		assertEquals(0.1f, SprintFovState.adjust(0.05f, 1.0f, 0), 1.0e-5f);
	}

	@Test
	void percentIsClamped() {
		assertEquals(1.15f, SprintFovState.adjust(1.15f, 1.0f, 999), 1.0e-5f);
		assertEquals(1.0f, SprintFovState.adjust(1.15f, 1.0f, -20), 1.0e-5f);
	}
}
