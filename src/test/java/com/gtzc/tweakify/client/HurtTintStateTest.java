package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 受伤变红的子设置：关闭红色 overlay 时，白闪进度要么保留原值，要么归零。
 */
class HurtTintStateTest {

	@Test
	void keepsWhiteFlashProgressWhenEnabled() {
		assertEquals(0.35f, HurtTintState.whiteFlashProgress(0.35f, true), 1.0e-6f);
	}

	@Test
	void dropsWhiteFlashProgressWhenDisabled() {
		assertEquals(0.0f, HurtTintState.whiteFlashProgress(0.35f, false), 1.0e-6f);
		assertEquals(0.0f, HurtTintState.whiteFlashProgress(0.0f, false), 1.0e-6f);
	}
}
