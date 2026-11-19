package com.gtzc.tweakify.client;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置界面搜索关键词的匹配区间计算。
 *
 * <p>过滤串来自 MaLiLib 的搜索框：已经转成小写，多个关键词用 {@code |} 分隔，
 * 匹配规则是“子串命中”。这里把规则换算成显示文本中的字符区间，供条目控件分段着色；
 * 区间互不重叠且按起点排序，渲染时顺序推进即可。
 */
final class ConfigSearchHighlighter {
	private ConfigSearchHighlighter() {
	}

	// 显示文本中被关键词命中的区间（左闭右开）。
	static List<Range> ranges(String text, String filter) {
		if (text == null || text.isEmpty() || filter == null || filter.isEmpty()) {
			return List.of();
		}
		List<Range> ranges = new ArrayList<>();
		for (String token : filter.split("\\|")) {
			if (token.isEmpty()) {
				continue;
			}
			// 用 regionMatches 逐个位置比较：不依赖小写转换后的长度，
			// 避免个别字符（如 İ）转小写后长度变化导致区间错位。
			for (int index = 0; index + token.length() <= text.length(); index++) {
				if (text.regionMatches(true, index, token, 0, token.length())) {
					ranges.add(new Range(index, index + token.length()));
					index += token.length() - 1;
				}
			}
		}
		return merge(ranges);
	}

	// 多个关键词可能命中同一段或相邻的文字：合并成连续区间，避免重复绘制。
	private static List<Range> merge(List<Range> ranges) {
		if (ranges.size() <= 1) {
			return List.copyOf(ranges);
		}
		ranges.sort((left, right) -> Integer.compare(left.start(), right.start()));
		List<Range> merged = new ArrayList<>();
		Range current = ranges.get(0);
		for (int i = 1; i < ranges.size(); i++) {
			Range next = ranges.get(i);
			if (next.start() <= current.end()) {
				current = new Range(current.start(), Math.max(current.end(), next.end()));
			} else {
				merged.add(current);
				current = next;
			}
		}
		merged.add(current);
		return List.copyOf(merged);
	}

	record Range(int start, int end) {
	}
}
