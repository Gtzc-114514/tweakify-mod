package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.TweakifyModTestSupport;
import com.gtzc.tweakify.config.TweakifyModConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 覆盖语言覆盖表的选择逻辑：跟随系统语言时完全不介入；
 * 关闭后按所选语言返回 tweakify-mod.* 文本，其它键与缺失键保持原版翻译。
 */
class LanguageOverrideStateTest {
	private final TweakifyModConfig originalConfig = TweakifyMod.config();

	@AfterEach
	void restoreConfig() {
		TweakifyModTestSupport.installConfig(originalConfig);
	}

	@Test
	void followsSystemLanguageByDefault() {
		TweakifyModTestSupport.installConfig(new TweakifyModConfig());

		assertNull(LanguageOverrideState.translate("tweakify-mod.gui.title.configs"),
			"follow-system is on by default");
	}

	@Test
	void overridesSelectedLanguage() {
		TweakifyModConfig config = new TweakifyModConfig();
		config.general.followSystemLanguage = false;
		config.general.language = "en_us";
		TweakifyModTestSupport.installConfig(config);

		assertEquals("Tweakify Mod Configs - %s",
			LanguageOverrideState.translate("tweakify-mod.gui.title.configs"));
	}

	@Test
	void overridesRussianAndFrench() {
		TweakifyModConfig config = new TweakifyModConfig();
		config.general.followSystemLanguage = false;
		TweakifyModTestSupport.installConfig(config);

		config.general.language = "ru_ru";
		assertEquals("Настройки Tweakify Mod - %s",
			LanguageOverrideState.translate("tweakify-mod.gui.title.configs"));

		config.general.language = "fr_fr";
		assertEquals("Configuration Tweakify Mod - %s",
			LanguageOverrideState.translate("tweakify-mod.gui.title.configs"));
	}

	@Test
	void ignoresOtherModsAndUnknownKeys() {
		TweakifyModConfig config = new TweakifyModConfig();
		config.general.followSystemLanguage = false;
		config.general.language = "en_us";
		TweakifyModTestSupport.installConfig(config);

		assertNull(LanguageOverrideState.translate("modmenu.descriptionTranslation.tweakify-mod"));
		assertNull(LanguageOverrideState.translate("some.other.key"));
		assertNull(LanguageOverrideState.translate(null));
	}
}
