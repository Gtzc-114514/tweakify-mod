package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 更好的进度：分页边界、页内位置与固定标签几何。
 */
class BetterAdvancementsTest {
	@Test
	void strideIncludesConfiguredSpacing() {
		assertEquals(28, BetterAdvancements.tabStride(), "配置未加载时使用原版间距");
		assertEquals(28, BetterAdvancements.pinnedTabStride());
		assertEquals(0, TweakifyModConfig.MIN_ADVANCEMENT_TAB_SPACING);
		assertEquals(8, TweakifyModConfig.MAX_ADVANCEMENT_TAB_SPACING);
		assertEquals(0, TweakifyModConfig.DEFAULT_ADVANCEMENT_TAB_SPACING);
	}

	@Test
	void paginationOnlyWhenAtLeastThreeTabsOverflow() {
		assertFalse(BetterAdvancements.isPaginated(2, 32, 100), "两个标签不分页");
		assertTrue(BetterAdvancements.isPaginated(3, 32, 100), "三个标签放不下时分页");
		assertFalse(BetterAdvancements.isPaginated(3, 32, 200), "放得下时不分页");
	}

	@Test
	void maxTabsAccountsForNavigationButtons() {
		int startX = 32;
		int endXTitle = 412;
		int paginated = BetterAdvancements.maxTabsPerPage(startX, endXTitle, true);
		int plain = BetterAdvancements.maxTabsPerPage(startX, endXTitle, false);
		assertEquals(11, paginated, "左右翻页按钮各占 23 像素并留 8 像素");
		assertEquals(13, plain);
		assertTrue(paginated < plain);
	}

	@Test
	void pageAndPositionWrapByMaxTabs() {
		assertEquals(3, BetterAdvancements.pageCount(23, 11));
		assertEquals(2, BetterAdvancements.pageCount(22, 11));
		assertEquals(1, BetterAdvancements.pageCount(0, 11));
		assertEquals(0, BetterAdvancements.pageOf(10, 11));
		assertEquals(1, BetterAdvancements.pageOf(11, 11));
		assertEquals(2, BetterAdvancements.pageOf(22, 11));

		assertEquals(1, BetterAdvancements.positionOnPage(0, 11, 0, true), "页内位置从 1 开始");
		assertEquals(2, BetterAdvancements.positionOnPage(12, 11, 1, true));
		assertEquals(5, BetterAdvancements.positionOnPage(5, 11, 0, false), "不分页时沿用原索引");

		assertTrue(BetterAdvancements.visibleOnPage(12, 11, 1, true));
		assertFalse(BetterAdvancements.visibleOnPage(12, 11, 0, true));
		assertTrue(BetterAdvancements.visibleOnPage(12, 11, 0, false));
	}

	@Test
	void tabAndPinnedGeometryStayStable() {
		assertEquals(32, BetterAdvancements.tabX(32, 0, false));
		assertEquals(59, BetterAdvancements.tabX(32, 1, true), "翻页按钮 23 像素 + 4 像素间隙");
		assertEquals(32 + 27 + 28, BetterAdvancements.tabX(32, 2, true));

		assertEquals(10, BetterAdvancements.maxPinnedTabs(52, 400));
		assertEquals(76, BetterAdvancements.pinnedTabY(52, 0));
		assertEquals(132, BetterAdvancements.pinnedTabY(52, 2));
	}

	@Test
	void defaultsMatchRequestedBehaviour() {
		TweakifyModConfig config = new TweakifyModConfig();
		assertFalse(config.modified.betterAdvancements, "默认不替换原版进度界面");
		assertTrue(config.modified.betterAdvancementsPagination);
		assertTrue(config.modified.betterAdvancementsPinTabs);
		assertTrue(config.modified.betterAdvancementsRememberTab);
		assertFalse(config.modified.betterAdvancementsDisableFade);
		assertTrue(config.modified.betterAdvancementsCopyId);
		assertEquals(0, config.modified.betterAdvancementsTabSpacing);
		assertEquals(0, config.modified.betterAdvancementsPinnedTabSpacing);
	}
}
