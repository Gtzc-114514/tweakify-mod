package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;

/**
 * 剑穿草打生物：哪些手持物品生效。
 */
public enum CutThroughItemMode implements CodeOptionEntry {
	SWORD(TweakifyModConfig.CUT_THROUGH_ITEMS_SWORD,
		"tweakify-mod.config.general.option.cutThroughItems.sword"),
	SWORD_AND_AXE(TweakifyModConfig.CUT_THROUGH_ITEMS_SWORD_AND_AXE,
		"tweakify-mod.config.general.option.cutThroughItems.sword_and_axe"),
	ANY(TweakifyModConfig.CUT_THROUGH_ITEMS_ANY,
		"tweakify-mod.config.general.option.cutThroughItems.any");

	private final String code;
	private final String translationKey;

	CutThroughItemMode(String code, String translationKey) {
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

	public static CutThroughItemMode fromCode(String code) {
		for (CutThroughItemMode mode : values()) {
			if (mode.code.equals(code)) {
				return mode;
			}
		}
		return SWORD;
	}

	@Override
	public IConfigOptionListEntry fromString(String value) {
		return fromCode(value);
	}
}
