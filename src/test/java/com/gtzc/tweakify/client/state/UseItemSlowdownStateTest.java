package com.gtzc.tweakify.client.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 使用物品减速保留比例：0% 完全取消，100% 与原版相同，中间线性插值。
 */
class UseItemSlowdownStateTest {

	@Test
	void disabledKeepsOriginal() {
		assertEquals(0.2f, UseItemSlowdownState.multiplier(false, 0, 0.2f), 1.0e-6f);
	}

	@Test
	void zeroPercentRemovesSlowdown() {
		assertEquals(1.0f, UseItemSlowdownState.multiplier(true, 0, 0.2f), 1.0e-6f);
	}

	@Test
	void hundredPercentKeepsOriginal() {
		assertEquals(0.2f, UseItemSlowdownState.multiplier(true, 100, 0.2f), 1.0e-6f);
	}

	@Test
	void halfPercentHalvesTheSlowdown() {
		// 原版 0.2 → 取消一半减速 → 0.6
		assertEquals(0.6f, UseItemSlowdownState.multiplier(true, 50, 0.2f), 1.0e-6f);
	}

	@Test
	void percentIsClamped() {
		assertEquals(1.0f, UseItemSlowdownState.multiplier(true, -50, 0.2f), 1.0e-6f);
		assertEquals(0.2f, UseItemSlowdownState.multiplier(true, 999, 0.2f), 1.0e-6f);
	}
}
