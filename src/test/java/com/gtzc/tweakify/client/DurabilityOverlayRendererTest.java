package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 耐久文字的内容与颜色分档：
 * 百分比四舍五入、数值只显示剩余耐久，颜色按剩余耐久由绿转白、黄、红。
 */
class DurabilityOverlayRendererTest {
	private static final int GREEN = 0xFF55FF55;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int YELLOW = 0xFFFFFF55;
	private static final int RED = 0xFFFF5555;

	@Test
	void percentTextRoundsToWholePercent() {
		assertEquals("50%", DurabilityOverlayRenderer.percentText(50, 100));
		assertEquals("67%", DurabilityOverlayRenderer.percentText(2, 3));
		assertEquals("100%", DurabilityOverlayRenderer.percentText(9, 9));
		assertEquals("0%", DurabilityOverlayRenderer.percentText(0, 0));
	}

	@Test
	void remainingTextShowsRemainingDurabilityOnly() {
		assertEquals("73", DurabilityOverlayRenderer.remainingText(73));
		assertEquals("0", DurabilityOverlayRenderer.remainingText(0));
	}

	@Test
	void textFollowsDisplayMode() {
		assertEquals("50%", DurabilityOverlayRenderer.textFor(DurabilityDisplayMode.PERCENT, 50, 100));
		assertEquals("50", DurabilityOverlayRenderer.textFor(DurabilityDisplayMode.VALUE, 50, 100));
		assertEquals("50%", DurabilityOverlayRenderer.textFor(null, 50, 100),
			"an unknown mode should fall back to percentage");
	}

	@Test
	void removedBothModeFallsBackToPercent() {
		assertEquals(DurabilityDisplayMode.PERCENT, DurabilityDisplayMode.fromCode("both"),
			"the removed two-line mode must fall back to percentage");
	}

	@Test
	void colorChangesWithRemainingDurability() {
		assertEquals(GREEN, DurabilityOverlayRenderer.colorFor(100, 100));
		assertEquals(GREEN, DurabilityOverlayRenderer.colorFor(80, 100));
		assertEquals(WHITE, DurabilityOverlayRenderer.colorFor(75, 100), "75% is the low end of white");
		assertEquals(WHITE, DurabilityOverlayRenderer.colorFor(60, 100));
		assertEquals(YELLOW, DurabilityOverlayRenderer.colorFor(50, 100), "50% is the low end of yellow");
		assertEquals(YELLOW, DurabilityOverlayRenderer.colorFor(30, 100));
		assertEquals(RED, DurabilityOverlayRenderer.colorFor(25, 100), "25% and below is red");
		assertEquals(RED, DurabilityOverlayRenderer.colorFor(0, 100));
	}

	// 支持列表只有一份：配置里的清单必须与枚举完全一致，否则手工改配置会被错误清洗。
	@Test
	void codesMatchSupportedDisplayList() {
		List<String> codes = Arrays.stream(DurabilityDisplayMode.values())
			.map(DurabilityDisplayMode::code)
			.toList();
		assertEquals(TweakifyModConfig.SUPPORTED_DURABILITY_DISPLAYS, codes);
	}
}
