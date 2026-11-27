package com.gtzc.tweakify.client.config;

import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;

/**
 * 单层基岩标记颜色：金色（默认）/ 白 / 红 / 绿 / 蓝 / 青。
 * 显示名走语言文件，切换语言后无需重启。
 */
public enum BedrockMarkerColor implements CodeOptionEntry {
	GOLD(TweakifyModConfig.BEDROCK_COLOR_GOLD,
		"tweakify-mod.config.general.option.highlightSingleLayerBedrockColor.gold", 0xFFFFC800),
	WHITE(TweakifyModConfig.BEDROCK_COLOR_WHITE,
		"tweakify-mod.config.general.option.highlightSingleLayerBedrockColor.white", 0xFFFFFFFF),
	RED(TweakifyModConfig.BEDROCK_COLOR_RED,
		"tweakify-mod.config.general.option.highlightSingleLayerBedrockColor.red", 0xFFFF5555),
	GREEN(TweakifyModConfig.BEDROCK_COLOR_GREEN,
		"tweakify-mod.config.general.option.highlightSingleLayerBedrockColor.green", 0xFF55FF55),
	BLUE(TweakifyModConfig.BEDROCK_COLOR_BLUE,
		"tweakify-mod.config.general.option.highlightSingleLayerBedrockColor.blue", 0xFF5555FF),
	CYAN(TweakifyModConfig.BEDROCK_COLOR_CYAN,
		"tweakify-mod.config.general.option.highlightSingleLayerBedrockColor.cyan", 0xFF55FFFF);

	private final String code;
	private final String translationKey;
	private final int argb;

	BedrockMarkerColor(String code, String translationKey, int argb) {
		this.code = code;
		this.translationKey = translationKey;
		this.argb = argb;
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
		return this.argb;
	}

	public static BedrockMarkerColor fromCode(String code) {
		for (BedrockMarkerColor color : values()) {
			if (color.code.equals(code)) {
				return color;
			}
		}
		return GOLD;
	}

	// 把配置里的 code 换成 ARGB；未知值回退金色。
	public static int argbOf(String code) {
		return fromCode(code).argb();
	}

	@Override
	public IConfigOptionListEntry fromString(String value) {
		return fromCode(value);
	}
}
