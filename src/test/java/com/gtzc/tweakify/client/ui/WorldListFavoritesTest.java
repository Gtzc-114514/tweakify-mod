package com.gtzc.tweakify.client.ui;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 存档收藏排序：非世界项（加载头/提示行）最前、收藏项其次、
 * 同级按列表原始顺序（取消收藏后回到原位置）。
 */
class WorldListFavoritesTest {
	private static final Map<String, Integer> BASE = Map.of(
		"a", 0, "b", 1, "c", 2, "d", 3);

	@Test
	void favoritesComeFirstAndOrderIsStable() {
		List<String> entries = List.of("header", "a", "b", "c", "d");
		// header 是非世界项（rank 0），b/d 是收藏项（rank 1），其余 rank 2。
		Map<String, Integer> ranks = Map.of("header", 0, "b", 1, "d", 1);
		List<String> sorted = WorldListFavorites.sortByRankAndBase(entries,
			entry -> ranks.getOrDefault(entry, 2),
			entry -> BASE.getOrDefault(entry, 0));
		assertEquals(List.of("header", "b", "d", "a", "c"), sorted);
	}

	// 取消收藏后：同为 rank 2 的条目按原始顺序排列，被取消的存档回到原位置。
	@Test
	void unfavoritedWorldReturnsToBaseOrder() {
		// 收藏 c 之后的列表顺序：c 被置顶，其余保持原始顺序。
		List<String> displayed = List.of("c", "a", "b", "d");
		List<String> sorted = WorldListFavorites.sortByRankAndBase(displayed,
			entry -> 2,
			entry -> BASE.getOrDefault(entry, 0));
		assertEquals(List.of("a", "b", "c", "d"), sorted);
	}

	// 收藏 b 时：其他存档保持原始顺序，b 置顶。
	@Test
	void favoritingMovesOnlyTheFavoritedWorldToTop() {
		List<String> displayed = List.of("a", "b", "c", "d");
		List<String> sorted = WorldListFavorites.sortByRankAndBase(displayed,
			entry -> entry.equals("b") ? 1 : 2,
			entry -> BASE.getOrDefault(entry, 0));
		assertEquals(List.of("b", "a", "c", "d"), sorted);
	}

	@Test
	void rankMappingMatchesSortContract() {
		assertEquals(0, WorldListFavorites.rankFor(false, false), "non-world entries stay on top");
		assertEquals(0, WorldListFavorites.rankFor(false, true));
		assertEquals(1, WorldListFavorites.rankFor(true, true), "favorited worlds come first");
		assertEquals(2, WorldListFavorites.rankFor(true, false), "other worlds keep their order");
	}

	@Test
	void baseOrderRoundTripAndUnknownFallsToLast() {
		WorldListFavorites.updateBaseOrder(List.of("x", "y"));
		assertEquals(0, WorldListFavorites.baseIndexOf("x"));
		assertEquals(1, WorldListFavorites.baseIndexOf("y"));
		assertEquals(Integer.MAX_VALUE, WorldListFavorites.baseIndexOf("missing"));
		// 清理静态状态，避免影响其它用例。
		WorldListFavorites.updateBaseOrder(List.of());
	}

	@Test
	void emptyListStaysEmpty() {
		assertEquals(List.of(), WorldListFavorites.sortByRankAndBase(List.<String>of(),
			entry -> 2, entry -> 0));
	}
}
