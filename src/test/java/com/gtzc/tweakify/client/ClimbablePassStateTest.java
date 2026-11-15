package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 经过可攀爬方块不减速：可限定只在潜行时跳过水平夹紧。
 */
class ClimbablePassStateTest {

	@Test
	void disabledNeverSkips() {
		assertFalse(ClimbablePassState.shouldSkipClamp(false, false, true));
	}

	@Test
	void allMovementWhenNotRestricted() {
		assertTrue(ClimbablePassState.shouldSkipClamp(true, false, false));
		assertTrue(ClimbablePassState.shouldSkipClamp(true, false, true));
	}

	@Test
	void sneakOnlyRequiresSneaking() {
		assertTrue(ClimbablePassState.shouldSkipClamp(true, true, true));
		assertFalse(ClimbablePassState.shouldSkipClamp(true, true, false));
	}
}
