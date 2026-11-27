package com.gtzc.tweakify.client.config;

import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;

/**
 * 手持物品信息的背景可见性：
 * vanilla 跟随“文字背景”无障碍选项 / always 始终画背景 / never 从不画。
 * 显示名走语言文件，切换语言后无需重启。
 */
public enum HeldItemInfoBackground implements CodeOptionEntry {
	VANILLA(TweakifyModConfig.HELD_ITEM_INFO_BACKGROUND_VANILLA,
		"tweakify-mod.config.general.option.heldItemInfoBackground.vanilla"),
	ALWAYS(TweakifyModConfig.HELD_ITEM_INFO_BACKGROUND_ALWAYS,
		"tweakify-mod.config.general.option.heldItemInfoBackground.always"),
	NEVER(TweakifyModConfig.HELD_ITEM_INFO_BACKGROUND_NEVER,
		"tweakify-mod.config.general.option.heldItemInfoBackground.never");

	private final String code;
	private final String translationKey;

	HeldItemInfoBackground(String code, String translationKey) {
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

	public static HeldItemInfoBackground fromCode(String code) {
		for (HeldItemInfoBackground background : values()) {
			if (background.code.equals(code)) {
				return background;
			}
		}
		return VANILLA;
	}

	@Override
	public IConfigOptionListEntry fromString(String value) {
		return fromCode(value);
	}
}
