package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 覆盖飞行漂移清理的边界：没有对应方向输入但仍有速度时清零；
 * 主动移动或已经静止时都不应干预速度，垂直方向额外受子设置控制。
 */
class FlightDriftStateTest {

	@Test
	void stopsHorizontalDriftWhenNoInput() {
		assertTrue(FlightDriftState.shouldStopHorizontalDrift(false, true));
	}

	@Test
	void keepsMovingWhileHorizontalInputHeld() {
		assertFalse(FlightDriftState.shouldStopHorizontalDrift(true, true),
			"holding WASD is intentional movement, not drift");
	}

	@Test
	void ignoresAlreadyStoppedHorizontalMovement() {
		assertFalse(FlightDriftState.shouldStopHorizontalDrift(false, false),
			"no horizontal velocity needs no clearing");
	}

	@Test
	void stopsVerticalDriftWhenEnabledAndNoInput() {
		assertTrue(FlightDriftState.shouldStopVerticalDrift(false, true, true));
	}

	@Test
	void keepsVerticalInertiaByDefault() {
		assertFalse(FlightDriftState.shouldStopVerticalDrift(false, false, true),
			"vertical drift is only removed when the child setting is enabled");
	}

	@Test
	void keepsRisingOrDescendingWhileInputHeld() {
		assertFalse(FlightDriftState.shouldStopVerticalDrift(true, true, true),
			"holding jump or sneak is intentional vertical movement");
	}

	@Test
	void ignoresAlreadyStoppedVerticalMovement() {
		assertFalse(FlightDriftState.shouldStopVerticalDrift(false, true, false));
	}
}
