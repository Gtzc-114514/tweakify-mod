package com.gtzc.tweakify.client.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 雪傀儡群系免疫：可限定只在主世界生效。
 */
class SnowGolemBiomeRulesTest {

	@Test
	void disabledNeverSuppresses() {
		assertFalse(SnowGolemBiomeRules.shouldSuppressMelting(false, false, true));
	}

	@Test
	void allDimensionsWhenNotRestricted() {
		assertTrue(SnowGolemBiomeRules.shouldSuppressMelting(true, false, true));
		assertTrue(SnowGolemBiomeRules.shouldSuppressMelting(true, false, false));
	}

	@Test
	void overworldOnlySkipsOtherDimensions() {
		assertTrue(SnowGolemBiomeRules.shouldSuppressMelting(true, true, true));
		assertFalse(SnowGolemBiomeRules.shouldSuppressMelting(true, true, false));
	}
}
