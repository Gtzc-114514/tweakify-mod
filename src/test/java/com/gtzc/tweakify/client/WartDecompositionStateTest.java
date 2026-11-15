package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 疣块分解产出数量：夹紧到 1 ~ 9。
 */
class WartDecompositionStateTest {

	@Test
	void configuredOutputIsClamped() {
		assertEquals(9, WartDecompositionState.outputCount(9));
		assertEquals(1, WartDecompositionState.outputCount(0));
		assertEquals(9, WartDecompositionState.outputCount(99));
	}
}
