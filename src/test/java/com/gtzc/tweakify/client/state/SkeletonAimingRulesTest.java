package com.gtzc.tweakify.client.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MC-121706：只有开启修复且有目标时才强制更新视线。
 */
class SkeletonAimingRulesTest {

	@Test
	void forcesLookOnlyWhenEnabledAndTargeted() {
		assertTrue(SkeletonAimingRules.shouldForceLook(true, true));
	}

	@Test
	void keepsVanillaWhenDisabled() {
		assertFalse(SkeletonAimingRules.shouldForceLook(false, true));
		assertFalse(SkeletonAimingRules.shouldForceLook(false, false));
	}

	@Test
	void keepsVanillaWithoutTarget() {
		assertFalse(SkeletonAimingRules.shouldForceLook(true, false));
	}
}
