package com.gtzc.tweakify.client.config;

import com.gtzc.tweakify.config.TweakifyModConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 物品分组排序：代码往返、翻译键前缀、未知代码回退原顺序。
 */
class ItemGroupSortTest {

	@Test
	void codesRoundTripAndUnknownFallsBackToDefault() {
		for (ItemGroupSort sort : ItemGroupSort.values()) {
			assertEquals(sort, ItemGroupSort.fromCode(sort.code()));
			assertEquals(sort, sort.fromString(sort.code()));
			assertTrue(sort.translationKey()
					.startsWith("tweakify-mod.config.general.option.itemGroupsSort."),
				"unexpected translation key: " + sort.translationKey());
		}
		assertEquals(TweakifyModConfig.ITEM_GROUP_SORT_DEFAULT, ItemGroupSort.DEFAULT.code());
		assertEquals(TweakifyModConfig.ITEM_GROUP_SORT_ALPHABETICAL, ItemGroupSort.ALPHABETICAL.code());
		assertEquals(ItemGroupSort.DEFAULT, ItemGroupSort.fromCode("not-a-sort"));
		assertEquals(ItemGroupSort.DEFAULT, ItemGroupSort.fromCode(null));
	}
}
