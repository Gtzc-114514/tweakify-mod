package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 手持物品信息的背景枚举必须与配置里的支持清单一致，未知值回退到跟随原版。
 */
class HeldItemInfoBackgroundTest {
	@Test
	void codesMatchSupportedList() {
		List<String> codes = Arrays.stream(HeldItemInfoBackground.values())
			.map(HeldItemInfoBackground::code)
			.toList();
		assertEquals(TweakifyModConfig.SUPPORTED_HELD_ITEM_INFO_BACKGROUNDS, codes);
	}

	@Test
	void unknownCodeFallsBackToVanilla() {
		assertEquals(HeldItemInfoBackground.VANILLA, HeldItemInfoBackground.fromCode("unknown"));
	}

	@Test
	void parsesKnownCodes() {
		assertEquals(HeldItemInfoBackground.ALWAYS, HeldItemInfoBackground.fromCode("always"));
		assertEquals(HeldItemInfoBackground.NEVER, HeldItemInfoBackground.fromCode("never"));
	}
}
