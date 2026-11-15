package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 禁用床太远：0 = 不限制（旧行为），>0 只放宽到指定格数。
 */
class BedDistanceStateTest {

	@Test
	void disabledKeepsVanillaDecision() {
		assertTrue(BedDistanceState.shouldAllowSleep(false, true, 100.0, 8));
		assertFalse(BedDistanceState.shouldAllowSleep(false, false, 1.0, 8));
	}

	@Test
	void vanillaInRangeAlwaysAllows() {
		assertTrue(BedDistanceState.shouldAllowSleep(true, true, 9999.0, 8));
	}

	@Test
	void zeroDistanceMeansUnlimited() {
		assertTrue(BedDistanceState.shouldAllowSleep(true, false, 9999.0, 0));
	}

	@Test
	void distanceThresholdIsInclusive() {
		assertTrue(BedDistanceState.shouldAllowSleep(true, false, 64.0, 8));
		assertFalse(BedDistanceState.shouldAllowSleep(true, false, 64.01, 8));
	}

	@Test
	void distanceIsClampedToMax() {
		assertTrue(BedDistanceState.shouldAllowSleep(true, false, 128.0 * 128.0, 9999));
		assertFalse(BedDistanceState.shouldAllowSleep(true, false, 128.0 * 128.0 + 1.0, 9999));
	}
}
