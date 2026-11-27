package com.gtzc.tweakify.client.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 疣块分解产出数量：夹紧到 1 ~ 9。
 */
class WartDecompositionRulesTest {

	@Test
	void configuredOutputIsClamped() {
		assertEquals(9, WartDecompositionRules.outputCount(9));
		assertEquals(1, WartDecompositionRules.outputCount(0));
		assertEquals(9, WartDecompositionRules.outputCount(99));
	}
}
