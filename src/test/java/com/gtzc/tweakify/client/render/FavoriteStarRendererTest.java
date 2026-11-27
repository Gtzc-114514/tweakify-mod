package com.gtzc.tweakify.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 配置行收藏星的几何边界：行宽不足时隐藏（返回 -1），而不是夹到重置按钮上方造成重叠。
 *
 * <p>与“重置按钮/控件预留”相关的布局断言在 ui 包的 {@code TweakifyModConfigScreenTest}。
 */
class FavoriteStarRendererTest {

	@Test
	void starIsHiddenWhenRowCannotFitIt() {
		int rowX = 10;
		int rowWidth = 200;
		int controlsRight = 100;
		int starX = FavoriteStarRenderer.rowStarX(controlsRight);
		int fitsWidth = starX + FavoriteStarRenderer.SIZE + 2 - rowX;
		assertEquals(starX, FavoriteStarRenderer.starXIfFits(controlsRight, rowX, fitsWidth),
			"star fits exactly when its right edge touches the row margin");
		assertEquals(-1, FavoriteStarRenderer.starXIfFits(controlsRight, rowX, fitsWidth - 1),
			"one pixel less must hide the star");
		assertEquals(-1, FavoriteStarRenderer.starXIfFits(9999, rowX, rowWidth),
			"buttons overflowing the row must hide the star");
		assertEquals(-1, FavoriteStarRenderer.starXIfFits(controlsRight, rowX, 20),
			"extremely narrow rows must hide the star");
	}
}
