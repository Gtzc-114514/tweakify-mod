package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;

/**
 * 物品分组内物品的排序方式：保持物品栏原顺序 / 按物品 ID 字母序。
 */
public enum ItemGroupSort implements CodeOptionEntry {
	DEFAULT(TweakifyModConfig.ITEM_GROUP_SORT_DEFAULT,
		"tweakify-mod.config.general.option.itemGroupsSort.default"),
	ALPHABETICAL(TweakifyModConfig.ITEM_GROUP_SORT_ALPHABETICAL,
		"tweakify-mod.config.general.option.itemGroupsSort.alphabetical");

	private final String code;
	private final String translationKey;

	ItemGroupSort(String code, String translationKey) {
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

	public static ItemGroupSort fromCode(String code) {
		for (ItemGroupSort sort : values()) {
			if (sort.code.equals(code)) {
				return sort;
			}
		}
		return DEFAULT;
	}

	@Override
	public IConfigOptionListEntry fromString(String value) {
		return fromCode(value);
	}
}
