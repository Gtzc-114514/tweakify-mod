package com.gtzc.tweakify.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 存档时长右对齐：文字右边界与内容区右边界留出固定内边距。
 */
class WorldPlayTimeRendererTest {

	@Test
	void rightAlignedTextStaysInsideContent() {
		assertEquals(76, WorldPlayTimeRenderer.xForRightAligned(100, 20));
	}
}
