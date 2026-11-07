package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BoatStepHeightStateTest {
	private static final float EPSILON = 1.0e-6f;

	@Test
	void raisesZeroStepToDirtPathHeight() {
		assertEquals(BoatStepHeightState.DIRT_PATH_STEP,
			BoatStepHeightState.stepHeight(true, 0.0f), EPSILON);
	}

	@Test
	void keepsLargerExistingStep() {
		assertEquals(0.2f, BoatStepHeightState.stepHeight(true, 0.2f), EPSILON,
			"其它模组设置更大步高时不能被压回 1/16");
	}

	@Test
	void disabledKeepsOriginalValue() {
		assertEquals(0.0f, BoatStepHeightState.stepHeight(false, 0.0f), EPSILON);
		assertEquals(0.5f, BoatStepHeightState.stepHeight(false, 0.5f), EPSILON);
	}
}
