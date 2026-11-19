package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 搜索高亮区间：与 MaLiLib 的过滤规则保持一致（大小写不敏感、子串命中、| 分隔多个关键词），
 * 区间必须有序且互不重叠，否则分段渲染会重复或漏画文字。
 */
class ConfigSearchHighlighterTest {

	@Test
	void singleKeywordMarksEveryOccurrence() {
		List<ConfigSearchHighlighter.Range> ranges = ConfigSearchHighlighter.ranges("itemGroupsShowItems", "group");

		assertEquals(List.of(new ConfigSearchHighlighter.Range(4, 9)), ranges);
	}

	@Test
	void matchingIsCaseInsensitive() {
		List<ConfigSearchHighlighter.Range> ranges = ConfigSearchHighlighter.ranges("SmoothScroll", "smooth");

		assertEquals(List.of(new ConfigSearchHighlighter.Range(0, 6)), ranges);
	}

	@Test
	void pipeSeparatedKeywordsAreAllHighlighted() {
		List<ConfigSearchHighlighter.Range> ranges = ConfigSearchHighlighter.ranges("scrollListMemory", "scroll|memory");

		assertEquals(List.of(
			new ConfigSearchHighlighter.Range(0, 6),
			new ConfigSearchHighlighter.Range(10, 16)), ranges);
	}

	@Test
	void overlappingKeywordsAreMerged() {
		List<ConfigSearchHighlighter.Range> ranges = ConfigSearchHighlighter.ranges("abcdef", "abc|cde");

		assertEquals(List.of(new ConfigSearchHighlighter.Range(0, 5)), ranges);
	}

	// 相邻的两个命中段颜色相同，合并成一段可以减少绘制次数。
	@Test
	void adjacentKeywordsAreMerged() {
		List<ConfigSearchHighlighter.Range> ranges = ConfigSearchHighlighter.ranges("smoothScroll", "smooth|scroll");

		assertEquals(List.of(new ConfigSearchHighlighter.Range(0, 12)), ranges);
	}

	@Test
	void chineseKeywordIsHighlighted() {
		List<ConfigSearchHighlighter.Range> ranges = ConfigSearchHighlighter.ranges("平滑滚动与右键拖动", "滚动");

		assertEquals(List.of(new ConfigSearchHighlighter.Range(2, 4)), ranges);
	}

	@Test
	void emptyInputProducesNoRanges() {
		assertTrue(ConfigSearchHighlighter.ranges("scrollListMemory", "").isEmpty());
		assertTrue(ConfigSearchHighlighter.ranges("scrollListMemory", null).isEmpty());
		assertTrue(ConfigSearchHighlighter.ranges("", "scroll").isEmpty());
		assertTrue(ConfigSearchHighlighter.ranges(null, "scroll").isEmpty());
	}

	@Test
	void nonMatchingKeywordProducesNoRanges() {
		assertTrue(ConfigSearchHighlighter.ranges("scrollListMemory", "zzz").isEmpty());
	}

	// 遍历每个位置而不是只找一次：同名片段在名称里可能出现多次。
	@Test
	void everyOccurrenceIsMarked() {
		List<ConfigSearchHighlighter.Range> ranges = ConfigSearchHighlighter.ranges("abXab", "ab");

		assertEquals(List.of(
			new ConfigSearchHighlighter.Range(0, 2),
			new ConfigSearchHighlighter.Range(3, 5)), ranges);
	}

	// 过滤串里出现空片段（例如结尾多写一个 |）时不应抛异常，也不产生空区间。
	@Test
	void emptyKeywordSegmentsAreIgnored() {
		List<ConfigSearchHighlighter.Range> ranges = ConfigSearchHighlighter.ranges("smooth", "smooth|");

		assertEquals(List.of(new ConfigSearchHighlighter.Range(0, 6)), ranges);
	}
}
