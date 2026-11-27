package com.gtzc.tweakify.client.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * MC-8187：只有开启、偏移为 -1 且基础尺寸为 1 时才把偏移改为 0。
 */
class SaplingGrowthStateTest {

	@Test
	void movesTwoByTwoOffsetToZero() {
		assertEquals(0, SaplingGrowthState.fixedOffset(true, -1, 1));
	}

	@Test
	void disabledKeepsOffset() {
		assertEquals(-1, SaplingGrowthState.fixedOffset(false, -1, 1));
	}

	@Test
	void leavesOtherSizesAndOffsetsUnchanged() {
		assertEquals(-2, SaplingGrowthState.fixedOffset(true, -2, 1));
		assertEquals(-1, SaplingGrowthState.fixedOffset(true, -1, 2));
		assertEquals(0, SaplingGrowthState.fixedOffset(true, 0, 1));
	}
}
