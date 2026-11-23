package com.gtzc.tweakify.client;

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
	void rangeMatchesConfigConstants() {
		assertEquals(50, TweakifyModConfig.MIN_ITEM_COUNT_SCALE);
		assertEquals(200, TweakifyModConfig.MAX_ITEM_COUNT_SCALE);
	}
}
