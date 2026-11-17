package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * MC-79545：关闭时保留原值；开启时把溢出为负数的经验需求提升到 1，
 * 正数（包括极大正数）保持不变。
 */
class ExperienceBarStateTest {

	@Test
	void disabledKeepsOriginalValue() {
		assertEquals(-1, ExperienceBarState.xpNeededForNextLevel(false, -1));
		assertEquals(0, ExperienceBarState.xpNeededForNextLevel(false, 0));
		assertEquals(7, ExperienceBarState.xpNeededForNextLevel(false, 7));
	}

	@Test
	void enabledKeepsPositiveValues() {
		assertEquals(7, ExperienceBarState.xpNeededForNextLevel(true, 7));
		assertEquals(112, ExperienceBarState.xpNeededForNextLevel(true, 112));
		assertEquals(Integer.MAX_VALUE, ExperienceBarState.xpNeededForNextLevel(true, Integer.MAX_VALUE));
	}

	@Test
	void enabledLiftsNonPositiveValuesToVisibleSentinel() {
		assertEquals(1, ExperienceBarState.xpNeededForNextLevel(true, 0));
		assertEquals(1, ExperienceBarState.xpNeededForNextLevel(true, -1));
		assertEquals(1, ExperienceBarState.xpNeededForNextLevel(true, Integer.MIN_VALUE));
	}
}
