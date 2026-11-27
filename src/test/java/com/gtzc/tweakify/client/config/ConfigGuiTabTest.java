package com.gtzc.tweakify.client.config;

import com.gtzc.tweakify.client.ui.TweakifyModConfigScreen;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 配置界面默认标签：代码与界面标签一一对应，未知代码回退到“全部”。
 */
class ConfigGuiTabTest {

	@Test
	void everyCodeMapsToATab() {
		for (ConfigGuiTab tab : ConfigGuiTab.values()) {
			assertEquals(tab, ConfigGuiTab.fromCode(tab.code()));
			assertTrue(tab.translationKey().startsWith("tweakify-mod.gui.tab."),
				"translation key should reuse the tab labels: " + tab.translationKey());
			assertTrue(tab.tab() != null);
		}
	}

	@Test
	void unknownCodeFallsBackToAll() {
		assertEquals(ConfigGuiTab.ALL, ConfigGuiTab.fromCode("not-a-tab"));
		assertEquals(ConfigGuiTab.ALL, ConfigGuiTab.fromCode(null));
	}

	@Test
	void codesCoverAllConfigTabs() {
		assertEquals(TweakifyModConfigScreen.ConfigTab.values().length, ConfigGuiTab.values().length);
	}
}
