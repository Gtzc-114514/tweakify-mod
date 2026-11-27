package com.gtzc.tweakify.client.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 冰霜行者跳跃修复：未骑乘、未落地才放宽；jumpOnly 时只放宽上升段。
 */
class FrostWalkerJumpRulesTest {

	@Test
	void disabledNeverRelaxes() {
		assertFalse(FrostWalkerJumpRules.shouldRelax(false, false, false, false, 0.5));
	}

	@Test
	void passengerOrGroundKeepsVanilla() {
		assertFalse(FrostWalkerJumpRules.shouldRelax(true, false, true, false, 0.5));
		assertFalse(FrostWalkerJumpRules.shouldRelax(true, false, false, true, 0.5));
	}

	@Test
	void airborneRelaxesInBothDirectionsByDefault() {
		assertTrue(FrostWalkerJumpRules.shouldRelax(true, false, false, false, 0.5));
		assertTrue(FrostWalkerJumpRules.shouldRelax(true, false, false, false, -0.5));
	}

	@Test
	void jumpOnlyRequiresAscending() {
		assertTrue(FrostWalkerJumpRules.shouldRelax(true, true, false, false, 0.2));
		assertFalse(FrostWalkerJumpRules.shouldRelax(true, true, false, false, 0.0));
		assertFalse(FrostWalkerJumpRules.shouldRelax(true, true, false, false, -0.2));
	}
}
