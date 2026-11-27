package com.gtzc.tweakify.client.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MC-105068：开启时完全格挡不再播放受伤音效；关闭、未格挡与部分格挡保持原版。
 */
class ShieldHurtSoundStateTest {

	@Test
	void disabledAlwaysPlaysHurtSound() {
		assertTrue(ShieldHurtSoundState.shouldPlayHurtSound(false, true, 0.0F));
		assertTrue(ShieldHurtSoundState.shouldPlayHurtSound(false, true, 2.0F));
	}

	@Test
	void enabledSuppressesFullyBlockedHurtSound() {
		assertFalse(ShieldHurtSoundState.shouldPlayHurtSound(true, true, 0.0F));
	}

	@Test
	void enabledKeepsPartialBlockAndUnblockedHurtSound() {
		assertTrue(ShieldHurtSoundState.shouldPlayHurtSound(true, true, 1.0F));
		assertTrue(ShieldHurtSoundState.shouldPlayHurtSound(true, false, 0.0F));
	}

	@Test
	void playersOnlyScopeLeavesMobsAlone() {
		assertTrue(ShieldHurtSoundState.shouldPlayHurtSound(true, true, false, true, 0.0F));
		assertFalse(ShieldHurtSoundState.shouldPlayHurtSound(true, true, true, true, 0.0F));
	}
}
