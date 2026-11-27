package com.gtzc.tweakify.client.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MC-121706：只有开启修复且有目标时才强制更新视线。
 */
class SkeletonAimingStateTest {

	@Test
	void forcesLookOnlyWhenEnabledAndTargeted() {
		assertTrue(SkeletonAimingState.shouldForceLook(true, true));
	}

	@Test
	void keepsVanillaWhenDisabled() {
		assertFalse(SkeletonAimingState.shouldForceLook(false, true));
		assertFalse(SkeletonAimingState.shouldForceLook(false, false));
	}

	@Test
	void keepsVanillaWithoutTarget() {
		assertFalse(SkeletonAimingState.shouldForceLook(true, false));
	}
}
