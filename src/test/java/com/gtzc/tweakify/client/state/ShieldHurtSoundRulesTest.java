package com.gtzc.tweakify.client.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MC-105068：开启时完全格挡不再播放受伤音效；关闭、未格挡与部分格挡保持原版。
 */
class ShieldHurtSoundRulesTest {

	@Test
	void disabledAlwaysPlaysHurtSound() {
		assertTrue(ShieldHurtSoundRules.shouldPlayHurtSound(false, true, 0.0F));
		assertTrue(ShieldHurtSoundRules.shouldPlayHurtSound(false, true, 2.0F));
	}

	@Test
	void enabledSuppressesFullyBlockedHurtSound() {
		assertFalse(ShieldHurtSoundRules.shouldPlayHurtSound(true, true, 0.0F));
	}

	@Test
	void enabledKeepsPartialBlockAndUnblockedHurtSound() {
		assertTrue(ShieldHurtSoundRules.shouldPlayHurtSound(true, true, 1.0F));
		assertTrue(ShieldHurtSoundRules.shouldPlayHurtSound(true, false, 0.0F));
	}

	@Test
	void playersOnlyScopeLeavesMobsAlone() {
		assertTrue(ShieldHurtSoundRules.shouldPlayHurtSound(true, true, false, true, 0.0F));
		assertFalse(ShieldHurtSoundRules.shouldPlayHurtSound(true, true, true, true, 0.0F));
	}
}
