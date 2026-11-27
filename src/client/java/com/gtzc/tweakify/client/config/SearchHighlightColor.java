package com.gtzc.tweakify.client.config;

import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;

/**
 * 配置界面搜索高亮颜色。
 * 显示名走语言文件；渲染时只取 RGB，透明度由界面统一决定。
 */
public enum SearchHighlightColor implements CodeOptionEntry {
	GOLD(TweakifyModConfig.SEARCH_HIGHLIGHT_GOLD,
		"tweakify-mod.config.general.option.searchHighlightColor.gold", 0xFFD54F),
	YELLOW(TweakifyModConfig.SEARCH_HIGHLIGHT_YELLOW,
		"tweakify-mod.config.general.option.searchHighlightColor.yellow", 0xFFFF55),
	GREEN(TweakifyModConfig.SEARCH_HIGHLIGHT_GREEN,
		"tweakify-mod.config.general.option.searchHighlightColor.green", 0x55FF55),
	AQUA(TweakifyModConfig.SEARCH_HIGHLIGHT_AQUA,
		"tweakify-mod.config.general.option.searchHighlightColor.aqua", 0x55FFFF),
	RED(TweakifyModConfig.SEARCH_HIGHLIGHT_RED,
		"tweakify-mod.config.general.option.searchHighlightColor.red", 0xFF5555),
	WHITE(TweakifyModConfig.SEARCH_HIGHLIGHT_WHITE,
		"tweakify-mod.config.general.option.searchHighlightColor.white", 0xFFFFFF);

	private final String code;
	private final String translationKey;
	private final int rgb;

	SearchHighlightColor(String code, String translationKey, int rgb) {
		this.code = code;
		this.translationKey = translationKey;
		this.rgb = rgb;
	}

	@Override
	public String code() {
		return this.code;
	}

	@Override
	public String translationKey() {
		return this.translationKey;
	}

	public int argb() {
		return 0xFF000000 | this.rgb;
	}

	// 按配置代码取颜色；未知代码回退到默认金色。
	public static int argbOf(String code) {
		for (SearchHighlightColor color : values()) {
			if (color.code.equals(code)) {
				return color.argb();
			}
		}
		return GOLD.argb();
	}

	public static SearchHighlightColor fromCode(String code) {
		for (SearchHighlightColor color : values()) {
			if (color.code.equals(code)) {
				return color;
			}
		}
		return GOLD;
	}

	@Override
	public IConfigOptionListEntry fromString(String value) {
		return fromCode(value);
	}
}
