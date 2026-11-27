package com.gtzc.tweakify.client.state;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CobwebSlowdownRulesTest {
	private static final double EPSILON = 1.0e-9;

	@Test
	void horizontalFreeKeepsVerticalPenalty() {
		Vec3 multiplier = new Vec3(0.25, 0.05, 0.25);
		Vec3 result = CobwebSlowdownRules.horizontalFree(multiplier);

		assertEquals(1.0, result.x, EPSILON);
		assertEquals(0.05, result.y, EPSILON);
		assertEquals(1.0, result.z, EPSILON);
	}

	@Test
	void weavingMultiplierKeepsItsStrongerVerticalPenalty() {
		Vec3 result = CobwebSlowdownRules.horizontalFree(new Vec3(0.5, 0.25, 0.5));

		assertEquals(1.0, result.x, EPSILON);
		assertEquals(0.25, result.y, EPSILON);
		assertEquals(1.0, result.z, EPSILON);
	}
}
