package com.gtzc.tweakify.client.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * MC-4490：开启时潜行偏移下移 0.1；关闭时保留原值。
 */
class FishingLineRulesTest {

	@Test
	void disabledKeepsVanillaOffset() {
		assertEquals(-0.1875F, FishingLineRules.crouchOffset(false, -0.1875F), 1.0e-6F);
	}

	@Test
	void enabledLowersCrouchOffsetByPointOne() {
		assertEquals(-0.2875F, FishingLineRules.crouchOffset(true, -0.1875F), 1.0e-6F);
	}

	@Test
	void customOffsetIsClamped() {
		assertEquals(-0.3875F, FishingLineRules.crouchOffset(true, 0.2, -0.1875F), 1.0e-6F);
		assertEquals(-0.1875F, FishingLineRules.crouchOffset(true, -1.0, -0.1875F), 1.0e-6F);
		assertEquals(-0.6875F, FishingLineRules.crouchOffset(true, 10.0, -0.1875F), 1.0e-6F);
	}
}
