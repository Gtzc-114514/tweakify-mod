package com.gtzc.tweakify.client.config;

import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;

/**
 * 配置界面的默认标签（关闭“标签记忆”时使用）。
 * 复用界面已有的 tweakify-mod.gui.tab.* 翻译，无需额外语言键。
 *
 * <p>只承载 code 与翻译键；code → 标签页的映射由界面侧
 * （{@code TweakifyModConfigScreen.tabOfCode}）负责，避免 config 包反向依赖 ui 包。
 */
public enum ConfigGuiTab implements CodeOptionEntry {
	ALL(TweakifyModConfig.CONFIG_TAB_ALL, "tweakify-mod.gui.tab.all"),
	GENERAL(TweakifyModConfig.CONFIG_TAB_GENERAL, "tweakify-mod.gui.tab.general"),
	FUNCTIONS(TweakifyModConfig.CONFIG_TAB_FUNCTIONS, "tweakify-mod.gui.tab.functions"),
	DISPLAY(TweakifyModConfig.CONFIG_TAB_DISPLAY, "tweakify-mod.gui.tab.display"),
	MODIFIED(TweakifyModConfig.CONFIG_TAB_MODIFIED, "tweakify-mod.gui.tab.modified"),
	FIXING(TweakifyModConfig.CONFIG_TAB_FIX, "tweakify-mod.gui.tab.fix"),
	OPTIMIZATION(TweakifyModConfig.CONFIG_TAB_OPTIMIZATION, "tweakify-mod.gui.tab.optimization"),
	TESTING(TweakifyModConfig.CONFIG_TAB_TESTING, "tweakify-mod.gui.tab.testing"),
	// 收藏标签：默认标签也可设为“收藏”（关闭标签记忆时使用）。
	FAVORITES(TweakifyModConfig.CONFIG_TAB_FAVORITES, "tweakify-mod.gui.tab.favorites");

	private final String code;
	private final String translationKey;

	ConfigGuiTab(String code, String translationKey) {
		this.code = code;
		this.translationKey = translationKey;
	}

	@Override
	public String code() {
		return this.code;
	}

	@Override
	public String translationKey() {
		return this.translationKey;
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
