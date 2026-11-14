package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;

/**
 * 耐久文字配色：自动按剩余耐久分档，或固定为白/黄/红。
 * 显示名走语言文件，切换语言后无需重启。
 */
public enum DurabilityColorMode implements CodeOptionEntry {
	AUTO(TweakifyModConfig.DURABILITY_COLOR_MODE_AUTO,
		"tweakify-mod.config.general.option.durabilityOverlayColorMode.auto"),
	WHITE(TweakifyModConfig.DURABILITY_COLOR_MODE_WHITE,
		"tweakify-mod.config.general.option.durabilityOverlayColorMode.white"),
	YELLOW(TweakifyModConfig.DURABILITY_COLOR_MODE_YELLOW,
		"tweakify-mod.config.general.option.durabilityOverlayColorMode.yellow"),
	RED(TweakifyModConfig.DURABILITY_COLOR_MODE_RED,
		"tweakify-mod.config.general.option.durabilityOverlayColorMode.red");

	private final String code;
	private final String translationKey;

	DurabilityColorMode(String code, String translationKey) {
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

	public static DurabilityColorMode fromCode(String code) {
		for (DurabilityColorMode mode : values()) {
			if (mode.code.equals(code)) {
				return mode;
			}
		}
		return AUTO;
	}

	@Override
	public IConfigOptionListEntry fromString(String value) {
		return fromCode(value);
	}
}
