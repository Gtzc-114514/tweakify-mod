package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;

/**
 * 装备耐久显示的显示内容：耐久百分比 / 剩余耐久数值。
 * 显示名走语言文件，切换语言后无需重启。
 */
public enum DurabilityDisplayMode implements CodeOptionEntry {
	PERCENT(TweakifyModConfig.DURABILITY_DISPLAY_PERCENT,
		"tweakify-mod.config.general.option.durabilityDisplayMode.percent"),
	VALUE(TweakifyModConfig.DURABILITY_DISPLAY_VALUE,
		"tweakify-mod.config.general.option.durabilityDisplayMode.value");

	private final String code;
	private final String translationKey;

	DurabilityDisplayMode(String code, String translationKey) {
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

	public static DurabilityDisplayMode fromCode(String code) {
		for (DurabilityDisplayMode mode : values()) {
			if (mode.code.equals(code)) {
				return mode;
			}
		}
		return PERCENT;
	}

	@Override
	public IConfigOptionListEntry fromString(String value) {
		return fromCode(value);
	}
}
