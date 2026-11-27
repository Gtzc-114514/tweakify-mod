package com.gtzc.tweakify.client.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 配置界面默认标签：代码与翻译键一一对应，未知代码回退到“全部”。
 * code → 标签页的映射由 ui 包负责，见 {@code TweakifyModConfigScreenTest}。
 */
class ConfigGuiTabTest {

	@Test
	void everyCodeMapsBackToItself() {
		for (ConfigGuiTab tab : ConfigGuiTab.values()) {
			assertEquals(tab, ConfigGuiTab.fromCode(tab.code()));
			assertTrue(tab.translationKey().startsWith("tweakify-mod.gui.tab."),
				"translation key should reuse the tab labels: " + tab.translationKey());
		}
	}

	@Test
	void unknownCodeFallsBackToAll() {
		assertEquals(ConfigGuiTab.ALL, ConfigGuiTab.fromCode("not-a-tab"));
		assertEquals(ConfigGuiTab.ALL, ConfigGuiTab.fromCode(null));
	}
}
