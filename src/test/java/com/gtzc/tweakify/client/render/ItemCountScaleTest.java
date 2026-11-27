package com.gtzc.tweakify.client.render;

import com.gtzc.tweakify.config.TweakifyModConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 物品数量缩放：锚点保持原版右下角，缩放百分比默认 100。
 */
class ItemCountScaleTest {
	@Test
	void anchorsStayAtVanillaBottomRight() {
		assertEquals(17.0F, ItemCountScale.anchorX(0));
		assertEquals(27.0F, ItemCountScale.anchorX(10));
		// 原版文字 y + 9，加上行高得到底边锚点。
		assertEquals(20.0F, ItemCountScale.anchorY(2, 9));
		assertEquals(11.0F, ItemCountScale.anchorY(2, 0));
	}

	@Test
	void defaultsToVanillaScaleWithoutConfig() {
		assertEquals(100, ItemCountScale.currentPercent(), "配置未加载时保持原版字号");
		TweakifyModConfig config = new TweakifyModConfig();
		assertEquals(TweakifyModConfig.DEFAULT_ITEM_COUNT_SCALE, config.modified.itemCountScale);
		assertEquals(100, config.modified.itemCountScale);
	}

	@Test
	void scaleIsCappedByTextWidth() {
		assertEquals(2.0F, ItemCountScale.effectiveScale(200, 6), 1.0E-4F, "一位数允许 200%");
		assertEquals(17.0F / 12.0F, ItemCountScale.effectiveScale(200, 12), 1.0E-4F,
			"两位数放大到格子宽度上限");
		assertEquals(1.0F, ItemCountScale.effectiveScale(200, 18), 1.0E-4F,
			"三位数保持原版字号，不遮挡左侧格子");
		assertEquals(1.0F, ItemCountScale.effectiveScale(200, 17), 1.0E-4F);
		assertEquals(0.5F, ItemCountScale.effectiveScale(50, 12), 1.0E-4F, "缩小不受宽度上限影响");
	}

	@Test
	void rangeMatchesConfigConstants() {
		assertEquals(50, TweakifyModConfig.MIN_ITEM_COUNT_SCALE);
		assertEquals(200, TweakifyModConfig.MAX_ITEM_COUNT_SCALE);
	}
}
