package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MC-159163：只有开启修复且目标是本地玩家时才移除 POSE 同步数据。
 */
class SneakAnimationStateTest {

	@Test
	void stripsOnlyForLocalPlayerWhenEnabled() {
		assertTrue(SneakAnimationState.shouldStripPose(true, true));
	}

	@Test
	void keepsPoseWhenDisabled() {
		assertFalse(SneakAnimationState.shouldStripPose(false, true));
		assertFalse(SneakAnimationState.shouldStripPose(false, false));
	}

	@Test
	void keepsPoseForOtherEntities() {
		assertFalse(SneakAnimationState.shouldStripPose(true, false));
	}

	@Test
	void onlyOnGroundGatesTheFix() {
		assertTrue(SneakAnimationState.shouldStripPose(true, true, true, true));
		assertFalse(SneakAnimationState.shouldStripPose(true, true, true, false));
		assertTrue(SneakAnimationState.shouldStripPose(true, false, true, false));
	}

	@Test
	void onlyWhenSneakingGatesTheFix() {
		assertTrue(SneakAnimationState.shouldStripPose(true, false, true, true, true, true));
		assertFalse(SneakAnimationState.shouldStripPose(true, false, true, true, true, false));
		assertTrue(SneakAnimationState.shouldStripPose(true, false, false, true, true, false));
	}
}
