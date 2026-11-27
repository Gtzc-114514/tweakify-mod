package com.gtzc.tweakify.client.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 聊天框选：鼠标 x 到字符插入位置的映射。
 *
 * <p>`charIndexAt` 是纯函数（输入只有前缀宽度表与文本），不依赖 Minecraft 实例；
 * 框选状态机与渲染路径仍由实机回归覆盖。
 */
class ChatSelectionStateTest {

	// 等宽字符：每个字符宽 5，前缀宽度数组为 [0,5,10,15]。
	private static final int[] ASCII_WIDTHS = {0, 5, 10, 15};

	@Test
	void nonPositiveXSnapsToStart() {
		assertEquals(0, ChatSelectionState.charIndexAt(ASCII_WIDTHS, "abc", -10));
		assertEquals(0, ChatSelectionState.charIndexAt(ASCII_WIDTHS, "abc", 0));
	}

	@Test
	void characterMidpointSplitsInsertion() {
		// 左半 < 2.5 → 0；右半 ≥ 2.5 → 1。
		assertEquals(0, ChatSelectionState.charIndexAt(ASCII_WIDTHS, "abc", 2.4));
		assertEquals(1, ChatSelectionState.charIndexAt(ASCII_WIDTHS, "abc", 2.5));
		assertEquals(1, ChatSelectionState.charIndexAt(ASCII_WIDTHS, "abc", 7.4));
		assertEquals(2, ChatSelectionState.charIndexAt(ASCII_WIDTHS, "abc", 7.5));
	}

	@Test
	void xBeyondTextSnapsToEnd() {
		assertEquals(3, ChatSelectionState.charIndexAt(ASCII_WIDTHS, "abc", 100));
	}

	@Test
	void emptyTextAlwaysReturnsZero() {
		assertEquals(0, ChatSelectionState.charIndexAt(new int[] {0}, "", 10));
	}

	@Test
	void surrogatePairIsNotSplit() {
		// "a😀b"：a 宽 5、emoji 宽 9、b 宽 5；前缀宽度为 [0,5,14,14,19]。
		int[] widths = {0, 5, 14, 14, 19};
		String text = "a😀b";
		assertEquals(0, ChatSelectionState.charIndexAt(widths, text, 2.4));
		assertEquals(1, ChatSelectionState.charIndexAt(widths, text, 2.5));
		// emoji 左半 → emoji 起点 1；右半 → 下一个码点起点 3（不允许拆成半个代理对）。
		assertEquals(1, ChatSelectionState.charIndexAt(widths, text, 9.4));
		assertEquals(3, ChatSelectionState.charIndexAt(widths, text, 9.5));
		assertEquals(4, ChatSelectionState.charIndexAt(widths, text, 100));
	}
}
