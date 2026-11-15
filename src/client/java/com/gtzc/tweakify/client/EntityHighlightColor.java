package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;

/**
 * 实体高亮的描边颜色：vanilla 保留原版队伍色，其余为固定调色板。
 * 显示名走语言文件，切换语言后无需重启。
 */
public enum EntityHighlightColor implements CodeOptionEntry {
	VANILLA(TweakifyModConfig.ENTITY_HIGHLIGHT_COLOR_VANILLA,
		"tweakify-mod.config.highlight.option.entityHighlightColor.vanilla", 0),
	WHITE(TweakifyModConfig.ENTITY_HIGHLIGHT_COLOR_WHITE,
		"tweakify-mod.config.highlight.option.entityHighlightColor.white", 0xFFFFFF),
	RED(TweakifyModConfig.ENTITY_HIGHLIGHT_COLOR_RED,
		"tweakify-mod.config.highlight.option.entityHighlightColor.red", 0xFF5555),
	GREEN(TweakifyModConfig.ENTITY_HIGHLIGHT_COLOR_GREEN,
		"tweakify-mod.config.highlight.option.entityHighlightColor.green", 0x55FF55),
	BLUE(TweakifyModConfig.ENTITY_HIGHLIGHT_COLOR_BLUE,
		"tweakify-mod.config.highlight.option.entityHighlightColor.blue", 0x5555FF),
	YELLOW(TweakifyModConfig.ENTITY_HIGHLIGHT_COLOR_YELLOW,
		"tweakify-mod.config.highlight.option.entityHighlightColor.yellow", 0xFFFF55),
	CYAN(TweakifyModConfig.ENTITY_HIGHLIGHT_COLOR_CYAN,
		"tweakify-mod.config.highlight.option.entityHighlightColor.cyan", 0x55FFFF),
	MAGENTA(TweakifyModConfig.ENTITY_HIGHLIGHT_COLOR_MAGENTA,
		"tweakify-mod.config.highlight.option.entityHighlightColor.magenta", 0xFF55FF);

	private final String code;
	private final String translationKey;
	private final int rgb;

	EntityHighlightColor(String code, String translationKey, int rgb) {
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

	// vanilla 返回 0，调用方据此保留原版队伍色。
	public int rgb() {
		return this.rgb;
	}

	public static EntityHighlightColor fromCode(String code) {
		for (EntityHighlightColor color : values()) {
			if (color.code.equals(code)) {
				return color;
			}
		}
		return VANILLA;
	}

	// 配置代码 → 不透明 ARGB；vanilla 或未知值返回 0。
	public static int argbOf(String code) {
		int rgb = fromCode(code).rgb();
		return rgb == 0 ? 0 : 0xFF000000 | rgb;
	}

	@Override
	public IConfigOptionListEntry fromString(String value) {
		return fromCode(value);
	}
}
