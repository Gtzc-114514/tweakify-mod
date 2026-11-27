package com.gtzc.tweakify.client.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 附魔描述门控：默认总是追加；开启“仅潜行”后要求按住潜行键。
 */
class EnchantmentDescriptionRulesTest {

	@Test
	void appendsWhenFeatureEnabled() {
		assertTrue(EnchantmentDescriptionRules.shouldAppend(true, false, false));
		assertTrue(EnchantmentDescriptionRules.shouldAppend(true, false, true));
	}

	@Test
	void sneakOnlyRequiresSneaking() {
		assertTrue(EnchantmentDescriptionRules.shouldAppend(true, true, true));
		assertFalse(EnchantmentDescriptionRules.shouldAppend(true, true, false));
	}

	@Test
	void featureOffNeverAppends() {
		assertFalse(EnchantmentDescriptionRules.shouldAppend(false, false, true));
	}
}
