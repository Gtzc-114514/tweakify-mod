package com.gtzc.tweakify.client.config;

import com.gtzc.tweakify.client.ui.TweakifyModConfigScreen;
import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;

/**
 * 配置界面的默认标签（关闭“标签记忆”时使用）。
 * 复用界面已有的 tweakify-mod.gui.tab.* 翻译，无需额外语言键。
 */
public enum ConfigGuiTab implements CodeOptionEntry {
	ALL(TweakifyModConfig.CONFIG_TAB_ALL, "tweakify-mod.gui.tab.all",
		TweakifyModConfigScreen.ConfigTab.ALL),
	GENERAL(TweakifyModConfig.CONFIG_TAB_GENERAL, "tweakify-mod.gui.tab.general",
		TweakifyModConfigScreen.ConfigTab.GENERAL),
	FUNCTIONS(TweakifyModConfig.CONFIG_TAB_FUNCTIONS, "tweakify-mod.gui.tab.functions",
		TweakifyModConfigScreen.ConfigTab.FUNCTIONS),
	DISPLAY(TweakifyModConfig.CONFIG_TAB_DISPLAY, "tweakify-mod.gui.tab.display",
		TweakifyModConfigScreen.ConfigTab.DISPLAY),
	MODIFIED(TweakifyModConfig.CONFIG_TAB_MODIFIED, "tweakify-mod.gui.tab.modified",
		TweakifyModConfigScreen.ConfigTab.MODIFIED),
	FIXING(TweakifyModConfig.CONFIG_TAB_FIX, "tweakify-mod.gui.tab.fix",
		TweakifyModConfigScreen.ConfigTab.FIXING),
	OPTIMIZATION(TweakifyModConfig.CONFIG_TAB_OPTIMIZATION, "tweakify-mod.gui.tab.optimization",
		TweakifyModConfigScreen.ConfigTab.OPTIMIZATION),
	TESTING(TweakifyModConfig.CONFIG_TAB_TESTING, "tweakify-mod.gui.tab.testing",
		TweakifyModConfigScreen.ConfigTab.TESTING),
	// 收藏标签：默认标签也可设为“收藏”（关闭标签记忆时使用）。
	FAVORITES(TweakifyModConfig.CONFIG_TAB_FAVORITES, "tweakify-mod.gui.tab.favorites",
		TweakifyModConfigScreen.ConfigTab.FAVORITES);

	private final String code;
	private final String translationKey;
	private final TweakifyModConfigScreen.ConfigTab tab;

	ConfigGuiTab(String code, String translationKey, TweakifyModConfigScreen.ConfigTab tab) {
		this.code = code;
		this.translationKey = translationKey;
		this.tab = tab;
	}

	@Override
	public String code() {
		return this.code;
	}

	@Override
	public String translationKey() {
		return this.translationKey;
	}

	public TweakifyModConfigScreen.ConfigTab tab() {
		return this.tab;
	}

	public static ConfigGuiTab fromCode(String code) {
		for (ConfigGuiTab tab : values()) {
			if (tab.code.equals(code)) {
				return tab;
			}
		}
		return ALL;
	}

	@Override
	public IConfigOptionListEntry fromString(String value) {
		return fromCode(value);
	}
}
