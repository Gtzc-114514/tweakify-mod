package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 攻击范围显示方式的枚举必须与配置里的支持清单一致，未知值回退到平面模式。
 */
class AttackRangeDisplayModeTest {
	@Test
	void codesMatchSupportedList() {
		List<String> codes = Arrays.stream(AttackRangeDisplayMode.values())
			.map(AttackRangeDisplayMode::code)
			.toList();
		assertEquals(TweakifyModConfig.SUPPORTED_ATTACK_RANGE_DISPLAY_MODES, codes);
	}

	@Test
	void unknownCodeFallsBackToFlat() {
		assertEquals(AttackRangeDisplayMode.FLAT, AttackRangeDisplayMode.fromCode("unknown"));
	}
}
