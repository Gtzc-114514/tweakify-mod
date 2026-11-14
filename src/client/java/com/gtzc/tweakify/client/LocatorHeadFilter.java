package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;

/**
 * 定位栏玩家过滤（参考 locator-heads 的 Advanced Player Filtering）：
 * 全部显示 / 仅显示名单内玩家 / 隐藏名单内玩家。
 */
public enum LocatorHeadFilter implements CodeOptionEntry {
	ALL(TweakifyModConfig.LOCATOR_HEAD_FILTER_ALL,
		"tweakify-mod.config.general.option.locatorBarHeadFilter.all"),
	INCLUDE(TweakifyModConfig.LOCATOR_HEAD_FILTER_INCLUDE,
		"tweakify-mod.config.general.option.locatorBarHeadFilter.include"),
	EXCLUDE(TweakifyModConfig.LOCATOR_HEAD_FILTER_EXCLUDE,
		"tweakify-mod.config.general.option.locatorBarHeadFilter.exclude");

	private final String code;
	private final String translationKey;

	LocatorHeadFilter(String code, String translationKey) {
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

	public static LocatorHeadFilter fromCode(String code) {
		for (LocatorHeadFilter filter : values()) {
			if (filter.code.equals(code)) {
				return filter;
			}
		}
		return ALL;
	}

	// 名单为空时的语义与参考实现一致：INCLUDE 全部隐藏，EXCLUDE 全部显示。
	public static boolean allows(String mode, boolean inList) {
		if (TweakifyModConfig.LOCATOR_HEAD_FILTER_INCLUDE.equals(mode)) {
			return inList;
		}
		if (TweakifyModConfig.LOCATOR_HEAD_FILTER_EXCLUDE.equals(mode)) {
			return !inList;
		}
		return true;
	}

	@Override
	public IConfigOptionListEntry fromString(String value) {
		return fromCode(value);
	}
}
