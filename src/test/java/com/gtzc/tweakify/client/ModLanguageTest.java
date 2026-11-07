package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 语言枚举的解析与循环切换。
 */
class ModLanguageTest {

	@Test
	void parsesCodesAndFallsBack() {
		assertEquals(ModLanguage.ZH_CN, ModLanguage.fromCode("zh_cn"));
		assertEquals(ModLanguage.EN_US, ModLanguage.fromCode("en_us"));
		assertEquals(ModLanguage.RU_RU, ModLanguage.fromCode("ru_ru"));
		assertEquals(ModLanguage.FR_FR, ModLanguage.fromCode("fr_fr"));
		assertEquals(ModLanguage.ZH_CN, ModLanguage.fromCode("unsupported"));
	}

	@Test
	void cyclesInBothDirections() {
		assertEquals(ModLanguage.EN_US, ModLanguage.ZH_CN.cycle(true));
		assertEquals(ModLanguage.FR_FR, ModLanguage.ZH_CN.cycle(false));
		assertEquals(ModLanguage.ZH_CN, ModLanguage.FR_FR.cycle(true));
	}

	@Test
	void fromStringUsesCode() {
		assertEquals(ModLanguage.RU_RU, ModLanguage.ZH_CN.fromString("ru_ru"));
	}

	// 支持语言清单只有一份：配置里的清单必须与枚举完全一致。
	@Test
	void codesMatchSupportedLanguageList() {
		List<String> codes = Arrays.stream(ModLanguage.values()).map(ModLanguage::code).toList();
		assertEquals(TweakifyModConfig.SUPPORTED_LANGUAGES, codes);
	}
}
