package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * MC-4490：开启时潜行偏移下移 0.1；关闭时保留原值。
 */
class FishingLineStateTest {

	@Test
	void disabledKeepsVanillaOffset() {
		assertEquals(-0.1875F, FishingLineState.crouchOffset(false, -0.1875F), 1.0e-6F);
	}

	@Test
	void enabledLowersCrouchOffsetByPointOne() {
		assertEquals(-0.2875F, FishingLineState.crouchOffset(true, -0.1875F), 1.0e-6F);
	}
}
