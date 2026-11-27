package com.gtzc.tweakify.client.config;

import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;

/**
 * 药水剩余时间文字配色：自动（接近结束时变黄/变红）/ 固定白 / 黄 / 红。
 * 显示名走语言文件，切换语言后无需重启。
 */
public enum StatusDurationColorMode implements CodeOptionEntry {
	AUTO(TweakifyModConfig.STATUS_COLOR_AUTO,
		"tweakify-mod.config.general.option.statusEffectDurationColorMode.auto"),
	WHITE(TweakifyModConfig.STATUS_COLOR_WHITE,
		"tweakify-mod.config.general.option.statusEffectDurationColorMode.white"),
	YELLOW(TweakifyModConfig.STATUS_COLOR_YELLOW,
		"tweakify-mod.config.general.option.statusEffectDurationColorMode.yellow"),
	RED(TweakifyModConfig.STATUS_COLOR_RED,
		"tweakify-mod.config.general.option.statusEffectDurationColorMode.red");

	private final String code;
	private final String translationKey;

	StatusDurationColorMode(String code, String translationKey) {
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

	public static StatusDurationColorMode fromCode(String code) {
		for (StatusDurationColorMode mode : values()) {
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
