package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 创造飞行穿墙的判定：必须同时满足开关、客户端、飞行、创造与不在载具上。
 */
class CreativeFlightNoclipTest {
	@Test
	void onlyBypassesWhenEveryConditionHolds() {
		assertTrue(CreativeFlightNoclip.shouldBypass(true, true, true, true, false));
	}

	@Test
	void disabledSwitchBlocksBypass() {
		assertFalse(CreativeFlightNoclip.shouldBypass(false, true, true, true, false));
	}

	@Test
	void serverSideNeverBypasses() {
		assertFalse(CreativeFlightNoclip.shouldBypass(true, false, true, true, false),
			"服务端仍按原版碰撞模拟");
	}

	@Test
	void nonFlyingOrNonCreativeNeverBypasses() {
		assertFalse(CreativeFlightNoclip.shouldBypass(true, true, false, true, false));
		assertFalse(CreativeFlightNoclip.shouldBypass(true, true, true, false, false));
	}

	@Test
	void passengersNeverBypass() {
		assertFalse(CreativeFlightNoclip.shouldBypass(true, true, true, true, true));
	}
}
