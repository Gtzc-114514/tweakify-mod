package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 药水时间显示的两条纯逻辑：低时长过滤与文字配色分档。
 * 不依赖渲染上下文，避免为了测分档去搭 HUD。
 */
class StatusEffectTimerRendererTest {

	@Test
	void thresholdZeroShowsEverything() {
		assertTrue(StatusEffectTimerRenderer.shouldShowForThreshold(1200, 0));
		assertTrue(StatusEffectTimerRenderer.shouldShowForThreshold(0, 0),
			"无尽/刚结束的效果交给绘制层处理");
	}

	@Test
	void thresholdKeepsOnlyShortEffects() {
		assertTrue(StatusEffectTimerRenderer.shouldShowForThreshold(200, 10), "10 秒正好命中");
		assertTrue(StatusEffectTimerRenderer.shouldShowForThreshold(100, 10));
		assertFalse(StatusEffectTimerRenderer.shouldShowForThreshold(201, 10),
			"超过阈值 1 tick 就不再显示");
		assertTrue(StatusEffectTimerRenderer.shouldShowForThreshold(-1, 10), "无尽效果放行");
	}

	@Test
	void autoColorStepsByRemainingSeconds() {
		assertEquals(0xFFFF5555, StatusEffectTimerRenderer.timerColorArgb("auto", 200), "10 秒内红色");
		assertEquals(0xFFFFFF55, StatusEffectTimerRenderer.timerColorArgb("auto", 400), "30 秒内黄色");
		assertEquals(0xFFFFFFFF, StatusEffectTimerRenderer.timerColorArgb("auto", 800));
	}

	@Test
	void fixedColorsIgnoreRemainingTime() {
		assertEquals(0xFFFFFFFF, StatusEffectTimerRenderer.timerColorArgb("white", 40));
		assertEquals(0xFFFFFF55, StatusEffectTimerRenderer.timerColorArgb("yellow", 40));
		assertEquals(0xFFFF5555, StatusEffectTimerRenderer.timerColorArgb("red", 800));
		assertEquals(0xFFFFFFFF, StatusEffectTimerRenderer.timerColorArgb("unknown", 800),
			"未知模式回退 auto，40 秒仍为白色");
	}
}
