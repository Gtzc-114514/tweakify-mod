package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 挖掘进度保留：总开关、同一方块与距离阈值三层判定。
 */
class KeepMiningProgressStateTest {

	@Test
	void masterSwitchAndSameBlockAreRequired() {
		assertFalse(KeepMiningProgressState.shouldKeep(false, true, 0.0, 0));
		assertFalse(KeepMiningProgressState.shouldKeep(true, false, 0.0, 0));
		assertTrue(KeepMiningProgressState.shouldKeep(true, true, 100.0, 0));
	}

	@Test
	void zeroDistanceMeansNoLimit() {
		assertTrue(KeepMiningProgressState.shouldKeep(true, true, 9999.0, 0));
	}

	@Test
	void distanceThresholdIsInclusive() {
		assertTrue(KeepMiningProgressState.shouldKeep(true, true, 9.0, 3), "3 格边界命中");
		assertFalse(KeepMiningProgressState.shouldKeep(true, true, 9.01, 3));
	}

	@Test
	void distanceIsClampedToMax() {
		assertTrue(KeepMiningProgressState.shouldKeep(true, true, 256.0, 999),
			"阈值夹紧到 16 格，16² = 256 命中");
		assertFalse(KeepMiningProgressState.shouldKeep(true, true, 256.01, 999));
	}
}
