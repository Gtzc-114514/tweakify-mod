package com.gtzc.tweakify.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MC-268420：开启时攻击强度为 0 不画进度精灵；关闭或仍有进度时保持原版。
 */
class CooldownIndicatorStateTest {

	@Test
	void disabledAlwaysDrawsProgressBlock() {
		assertTrue(CooldownIndicatorState.shouldDrawProgress(false, 0.0F));
		assertTrue(CooldownIndicatorState.shouldDrawProgress(false, 0.5F));
	}

	@Test
	void enabledSkipsZeroProgress() {
		assertFalse(CooldownIndicatorState.shouldDrawProgress(true, 0.0F));
	}

	@Test
	void enabledKeepsPositiveProgress() {
		assertTrue(CooldownIndicatorState.shouldDrawProgress(true, 0.01F));
		assertTrue(CooldownIndicatorState.shouldDrawProgress(true, 1.0F));
	}

	@Test
	void minimumProgressSkipsNarrowBars() {
		assertFalse(CooldownIndicatorState.shouldDrawProgress(true, 5, 0.2F));
		assertTrue(CooldownIndicatorState.shouldDrawProgress(true, 5, 0.4F));
		assertTrue(CooldownIndicatorState.shouldDrawProgress(false, 5, 0.0F));
	}
}
