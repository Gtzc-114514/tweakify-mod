package com.gtzc.tweakify.client.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 创造飞行穿墙的判定：开关、创造模式与飞行必须同时成立。
 */
class CreativeFlightNoclipTest {
	@Test
	void onlyActiveWhenEveryConditionHolds() {
		assertTrue(CreativeFlightNoclip.isActive(true, true, true));
	}

	@Test
	void disabledSwitchNeverActive() {
		assertFalse(CreativeFlightNoclip.isActive(false, true, true));
	}

	@Test
	void nonCreativeOrNonFlyingNeverActive() {
		assertFalse(CreativeFlightNoclip.isActive(true, false, true));
		assertFalse(CreativeFlightNoclip.isActive(true, true, false));
	}
}
