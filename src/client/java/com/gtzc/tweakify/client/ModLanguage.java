package com.gtzc.tweakify.client;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;

/**
 * 模组界面可选语言。显示名固定为各语言的自称，便于在任何当前语言下识别。
 */
public enum ModLanguage implements CodeOptionEntry {
	ZH_CN("zh_cn", "中文"),
	EN_US("en_us", "English"),
	RU_RU("ru_ru", "Русский"),
	FR_FR("fr_fr", "Français");

	private final String code;
	private final String displayName;

	ModLanguage(String code, String displayName) {
		this.code = code;
		this.displayName = displayName;
	}

	@Override
	public String code() {
		return this.code;
	}

	public static ModLanguage fromCode(String code) {
		for (ModLanguage language : values()) {
			if (language.code.equals(code)) {
				return language;
			}
		}
		return ZH_CN;
	}

	// 语言名不做翻译：任何界面语言下都显示各语言的自称。
	@Override
	public String getDisplayName() {
		return this.displayName;
	}

	@Override
	public IConfigOptionListEntry fromString(String value) {
		return fromCode(value);
	}
}
