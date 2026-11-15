package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 附魔等级数字样式：plain 为“ 3”，parentheses 为“ (3)”；未知样式回退 plain。
 */
class EnchantmentLevelStyleTest {

	@Test
	void plainAppendsSpaceAndNumber() {
		assertEquals(" 3", EnchantmentLevelStyle.suffix("plain", 3));
	}

	@Test
	void parenthesesWrapNumber() {
		assertEquals(" (3)", EnchantmentLevelStyle.suffix("parentheses", 3));
	}

	@Test
	void unknownStyleFallsBackToPlain() {
		assertEquals(" 5", EnchantmentLevelStyle.suffix("not-a-style", 5));
		assertEquals(" 5", EnchantmentLevelStyle.suffix(null, 5));
	}
}
