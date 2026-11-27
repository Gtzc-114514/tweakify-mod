package com.gtzc.tweakify.client.ui;

import com.gtzc.tweakify.config.TweakifyModConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 快速滚动的纯逻辑：开关、Shift 与倍率夹紧。
 */
class FastScrollTest {
	@Test
	void disabledKeepsOriginalAmount() {
		assertEquals(1.0D, FastScroll.scaleAmount(1.0D, false, 4, true), 1.0e-9);
	}

	@Test
	void withoutShiftKeepsOriginalAmount() {
		assertEquals(1.0D, FastScroll.scaleAmount(1.0D, true, 4, false), 1.0e-9);
	}

	@Test
	void shiftMultipliesAmount() {
		assertEquals(4.0D, FastScroll.scaleAmount(1.0D, true, 4, true), 1.0e-9);
		assertEquals(-28.0D, FastScroll.scaleAmount(-7.0D, true, 4, true), 1.0e-9);
	}

	@Test
	void multiplierIsClampedToConfiguredRange() {
		assertEquals(TweakifyModConfig.MIN_FAST_SCROLL_MULTIPLIER,
			FastScroll.scaleAmount(1.0D, true, 0, true), 1.0e-9);
		assertEquals(TweakifyModConfig.MAX_FAST_SCROLL_MULTIPLIER,
			FastScroll.scaleAmount(1.0D, true, 999, true), 1.0e-9);
	}

	@Test
	void zeroAmountStaysZero() {
		assertEquals(0.0D, FastScroll.scaleAmount(0.0D, true, 8, true), 1.0e-9);
	}
}
