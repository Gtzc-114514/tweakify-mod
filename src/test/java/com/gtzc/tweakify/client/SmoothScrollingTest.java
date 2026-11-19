package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 创造栏网格的行/像素换算：连续位置必须拆成“起始行 + 亚行像素偏移”，
 * 且偏移始终落在 [0, 槽位高度)，否则整个物品格会错位或跳到相邻行。
 */
class SmoothScrollingTest {
	private static final int SLOT_SIZE = 18;

	@Test
	void layoutSplitsRowAndShift() {
		assertEquals(new SmoothScrolling.CreativeScrollLayout(0, 0),
			SmoothScrolling.creativeLayout(0.0, SLOT_SIZE));
		assertEquals(new SmoothScrolling.CreativeScrollLayout(0, 7),
			SmoothScrolling.creativeLayout(0.4, SLOT_SIZE));
		assertEquals(new SmoothScrolling.CreativeScrollLayout(1, 9),
			SmoothScrolling.creativeLayout(1.5, SLOT_SIZE));
		assertEquals(new SmoothScrolling.CreativeScrollLayout(2, 0),
			SmoothScrolling.creativeLayout(2.0, SLOT_SIZE));
	}

	// 偏移四舍五入到整格时必须进位，否则会出现“第 0 行偏移 18 像素”这种非法布局。
	@Test
	void shiftRoundsUpIntoNextRow() {
		assertEquals(new SmoothScrolling.CreativeScrollLayout(1, 0),
			SmoothScrolling.creativeLayout(0.99, SLOT_SIZE));
	}

	@Test
	void negativePositionClampsToTop() {
		assertEquals(new SmoothScrolling.CreativeScrollLayout(0, 0),
			SmoothScrolling.creativeLayout(-0.5, SLOT_SIZE));
	}

	@Test
	void shiftNeverReachesSlotHeight() {
		for (int step = 0; step <= 200; step++) {
			SmoothScrolling.CreativeScrollLayout layout =
				SmoothScrolling.creativeLayout(step / 20.0, SLOT_SIZE);
			assertEquals(true, layout.shiftPixels() >= 0 && layout.shiftPixels() < SLOT_SIZE,
				"shift out of range at step " + step);
		}
	}

	// 分组展开/收起恢复顶部行时保留亚行偏移：丢掉小数部分会让整片格子跳一下。
	@Test
	void restoredScrollPositionKeepsSubRowOffset() {
		assertEquals(0.35f, SmoothScrolling.restoredScrollPosition(3, 0.5, 10), 1.0e-6);
		assertEquals(0.0f, SmoothScrolling.restoredScrollPosition(3, 0.5, 0), 1.0e-6);
		assertEquals(1.0f, SmoothScrolling.restoredScrollPosition(99, 0.0, 10), 1.0e-6);
		assertEquals(0.2f, SmoothScrolling.restoredScrollPosition(2, Double.NaN, 10), 1.0e-6);
		assertEquals(0.0f, SmoothScrolling.restoredScrollPosition(-5, 0.0, 10), 1.0e-6);
	}

	// 槽位实际行会被原版 scrollTo 四舍五入：记录值与渲染行不一致时预告行必须停画，
	// 否则会出现“上下两排同样物品”。
	@Test
	void creativeMenuRowResetsToUnknown() {
		SmoothScrolling.setCreativeMenuRow(7);
		assertEquals(7, SmoothScrolling.creativeMenuRow());
		SmoothScrolling.resetCreativeGridState();
		assertEquals(-1, SmoothScrolling.creativeMenuRow(), "重置后应回到未记录状态");
		assertEquals(0, SmoothScrolling.creativeAppliedRow());
	}

	// 精确显示索引：网格生效时用 menuRow*9 + 槽位号，未知时返回 -1 让调用方回退启发式。
	// 图标格/成员格与预告行必须用它判同一份索引，否则会闪烁。
	@Test
	void creativeDisplayIndexUsesMenuRowWhenActive() {
		SmoothScrolling.resetCreativeGridState();
		assertEquals(-1, SmoothScrolling.creativeDisplayIndex(3), "未记录行时不能给出假索引");
		SmoothScrolling.setCreativeMenuRow(2);
		SmoothScrolling.updateCreativeGridState(2, 0, 10);
		assertEquals(2 * 9 + 3, SmoothScrolling.creativeDisplayIndex(3));
		// 背包/快捷栏槽位不在物品格内：必须回退（-1），不能算出越界索引。
		assertEquals(-1, SmoothScrolling.creativeDisplayIndex(45));
		assertEquals(-1, SmoothScrolling.creativeDisplayIndex(-1));
		SmoothScrolling.resetCreativeGridState();
	}

	// 只有物品格内才允许做坐标补偿：背包/快捷槽在格子下方，必须保持原始坐标。
	@Test
	void insideCreativeGridCoversGridOnly() {
		int left = 100;
		int top = 50;
		assertTrue(SmoothScrolling.insideCreativeGrid(left + 9, top + 18, left, top));
		assertTrue(SmoothScrolling.insideCreativeGrid(left + 170, top + 107, left, top));
		assertFalse(SmoothScrolling.insideCreativeGrid(left + 8, top + 50, left, top));
		assertFalse(SmoothScrolling.insideCreativeGrid(left + 171, top + 50, left, top));
		assertFalse(SmoothScrolling.insideCreativeGrid(left + 100, top + 17, left, top));
		assertFalse(SmoothScrolling.insideCreativeGrid(left + 100, top + 108, left, top));
		// 快捷槽所在的一行（y=112 起）不在格子内，不能被亚行偏移影响。
		assertFalse(SmoothScrolling.insideCreativeGrid(left + 20, top + 120, left, top));
	}
}
