package com.gtzc.tweakify.client.config;

import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;

/**
 * 附魔等级数字样式：plain（锋利 3）/ parentheses（锋利 (3)）。
 * 显示名走语言文件，切换语言后无需重启。
 */
public enum EnchantmentLevelStyle implements CodeOptionEntry {
	PLAIN(TweakifyModConfig.ENCHANTMENT_LEVEL_STYLE_PLAIN,
		"tweakify-mod.config.general.option.enchantmentLevelArabicStyle.plain"),
	PARENTHESES(TweakifyModConfig.ENCHANTMENT_LEVEL_STYLE_PARENTHESES,
		"tweakify-mod.config.general.option.enchantmentLevelArabicStyle.parentheses");

	private final String code;
	private final String translationKey;

	EnchantmentLevelStyle(String code, String translationKey) {
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

	public static EnchantmentLevelStyle fromCode(String code) {
		for (EnchantmentLevelStyle style : values()) {
			if (style.code.equals(code)) {
				return style;
			}
		}
		return PLAIN;
	}

	// 直接拼在附魔名称后面的后缀：plain 为 " 3"，parentheses 为 " (3)"。
	public static String suffix(String code, int level) {
		return fromCode(code) == PARENTHESES ? " (" + level + ")" : " " + level;
	}

	@Override
	public IConfigOptionListEntry fromString(String value) {
		return fromCode(value);
	}
}
