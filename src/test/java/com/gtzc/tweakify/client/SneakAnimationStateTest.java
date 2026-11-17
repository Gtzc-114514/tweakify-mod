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
}
