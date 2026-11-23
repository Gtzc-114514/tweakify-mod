package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 挖掘惩罚保留强度：0% 完全去掉，100% 保持原版，中间线性插值。
 */
class MiningPenaltyStateTest {

	@Test
	void underwaterInterpolatesTowardsOne() {
		assertEquals(1.0, MiningPenaltyState.underwaterMultiplier(0.2, 0), 1.0e-9);
		assertEquals(0.6, MiningPenaltyState.underwaterMultiplier(0.2, 50), 1.0e-9);
		assertEquals(0.2, MiningPenaltyState.underwaterMultiplier(0.2, 100), 1.0e-9);
	}

	@Test
	void swimmingOnlyGateRequiresSwimming() {
		assertTrue(MiningPenaltyState.shouldRemoveUnderwaterPenalty(false, false));
		assertTrue(MiningPenaltyState.shouldRemoveUnderwaterPenalty(true, true));
		assertFalse(MiningPenaltyState.shouldRemoveUnderwaterPenalty(true, false));
	}

	@Test
	void underwaterKeepsBuffAboveOne() {
		assertEquals(1.5, MiningPenaltyState.underwaterMultiplier(1.5, 0), 1.0e-9);
	}

	@Test
	void airInterpolatesDivisorTowardsOne() {
		assertEquals(1.0f, MiningPenaltyState.airDivisor(5.0f, 0), 1.0e-6f);
		assertEquals(3.0f, MiningPenaltyState.airDivisor(5.0f, 50), 1.0e-6f);
		assertEquals(5.0f, MiningPenaltyState.airDivisor(5.0f, 100), 1.0e-6f);
		assertEquals(1.0f, MiningPenaltyState.airDivisor(1.0f, 0), 1.0e-6f);
	}

	@Test
	void keepPercentIsClamped() {
		assertEquals(0.2, MiningPenaltyState.underwaterMultiplier(0.2, 999), 1.0e-9,
			"高于上限时夹紧到 100%（保持原版）");
		assertEquals(5.0f, MiningPenaltyState.airDivisor(5.0f, 999), 1.0e-6f);
		assertEquals(1.0, MiningPenaltyState.underwaterMultiplier(0.2, -50), 1.0e-9,
			"低于下限时夹紧到 0%（完全去掉）");
		assertEquals(1.0f, MiningPenaltyState.airDivisor(5.0f, -50), 1.0e-6f);
	}
}
