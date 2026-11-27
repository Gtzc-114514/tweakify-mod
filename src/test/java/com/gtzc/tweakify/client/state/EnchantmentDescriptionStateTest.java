package com.gtzc.tweakify.client.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 附魔描述门控：默认总是追加；开启“仅潜行”后要求按住潜行键。
 */
class EnchantmentDescriptionStateTest {

	@Test
	void appendsWhenFeatureEnabled() {
		assertTrue(EnchantmentDescriptionState.shouldAppend(true, false, false));
		assertTrue(EnchantmentDescriptionState.shouldAppend(true, false, true));
	}

	@Test
	void sneakOnlyRequiresSneaking() {
		assertTrue(EnchantmentDescriptionState.shouldAppend(true, true, true));
		assertFalse(EnchantmentDescriptionState.shouldAppend(true, true, false));
	}

	@Test
	void featureOffNeverAppends() {
		assertFalse(EnchantmentDescriptionState.shouldAppend(false, false, true));
	}
}
