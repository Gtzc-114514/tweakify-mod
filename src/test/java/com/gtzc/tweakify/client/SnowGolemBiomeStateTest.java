package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 雪傀儡群系免疫：可限定只在主世界生效。
 */
class SnowGolemBiomeStateTest {

	@Test
	void disabledNeverSuppresses() {
		assertFalse(SnowGolemBiomeState.shouldSuppressMelting(false, false, true));
	}

	@Test
	void allDimensionsWhenNotRestricted() {
		assertTrue(SnowGolemBiomeState.shouldSuppressMelting(true, false, true));
		assertTrue(SnowGolemBiomeState.shouldSuppressMelting(true, false, false));
	}

	@Test
	void overworldOnlySkipsOtherDimensions() {
		assertTrue(SnowGolemBiomeState.shouldSuppressMelting(true, true, true));
		assertFalse(SnowGolemBiomeState.shouldSuppressMelting(true, true, false));
	}
}
