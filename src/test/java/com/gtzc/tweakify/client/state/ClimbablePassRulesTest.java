package com.gtzc.tweakify.client.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 经过可攀爬方块不减速：可限定只在潜行时跳过水平夹紧。
 */
class ClimbablePassRulesTest {

	@Test
	void disabledNeverSkips() {
		assertFalse(ClimbablePassRules.shouldSkipClamp(false, false, true));
	}

	@Test
	void allMovementWhenNotRestricted() {
		assertTrue(ClimbablePassRules.shouldSkipClamp(true, false, false));
		assertTrue(ClimbablePassRules.shouldSkipClamp(true, false, true));
	}

	@Test
	void sneakOnlyRequiresSneaking() {
		assertTrue(ClimbablePassRules.shouldSkipClamp(true, true, true));
		assertFalse(ClimbablePassRules.shouldSkipClamp(true, true, false));
	}

	@Test
	void blockTypeTogglesAreIndependent() {
		assertTrue(ClimbablePassRules.allowsBlock(true, false, false, true, true, true));
		assertTrue(ClimbablePassRules.allowsBlock(false, true, false, true, true, true));
		assertTrue(ClimbablePassRules.allowsBlock(false, false, true, true, true, true));

		assertFalse(ClimbablePassRules.allowsBlock(true, false, false, false, true, true));
		assertFalse(ClimbablePassRules.allowsBlock(false, true, false, true, false, true));
		assertFalse(ClimbablePassRules.allowsBlock(false, false, true, true, true, false));
		assertFalse(ClimbablePassRules.allowsBlock(false, false, false, true, true, true));
	}
}
