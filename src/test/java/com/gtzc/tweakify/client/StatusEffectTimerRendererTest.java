package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2f;
import org.junit.jupiter.api.Test;

import java.util.List;

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

	@Test
	void anchoredScaleKeepsTopRightAndRestoresMatrix() {
		Matrix3x2fStack pose = new Matrix3x2fStack(16);
		StatusEffectTimerRenderer.EffectScaleScope scope =
			StatusEffectTimerRenderer.pushAnchoredScale(pose, 320, 2.0f);
		try {
			Vector2f anchor = pose.transformPosition(new Vector2f(320.0f, 0.0f));
			assertEquals(320.0f, anchor.x, 1.0e-5, "右上角必须固定");
			assertEquals(0.0f, anchor.y, 1.0e-5);

			Vector2f left = pose.transformPosition(new Vector2f(295.0f, 17.0f));
			assertEquals(270.0f, left.x, 1.0e-5, "左侧按比例向左扩展");
			assertEquals(34.0f, left.y, 1.0e-5);
		} finally {
			scope.close();
		}

		Vector2f restored = pose.transformPosition(new Vector2f(7.0f, 9.0f));
		assertEquals(7.0f, restored.x, 1.0e-5, "close 后矩阵必须恢复");
		assertEquals(9.0f, restored.y, 1.0e-5);
	}

	@Test
	void effectScaleFollowsConfigAndClamps() {
		TweakifyModConfig config = new TweakifyModConfig();
		assertEquals(1.0f, StatusEffectTimerRenderer.effectScale(null), "未初始化时不缩放");

		config.functions.statusEffectDurationDisplay = false;
		config.functions.statusEffectDurationScale = 150;
		assertEquals(1.0f, StatusEffectTimerRenderer.effectScale(config), "功能关闭时不缩放");

		config.functions.statusEffectDurationDisplay = true;
		config.functions.statusEffectDurationScale = 100;
		assertEquals(1.0f, StatusEffectTimerRenderer.effectScale(config));
		config.functions.statusEffectDurationScale = 150;
		assertEquals(1.5f, StatusEffectTimerRenderer.effectScale(config));
		config.functions.statusEffectDurationScale = 10;
		assertEquals(0.5f, StatusEffectTimerRenderer.effectScale(config), "低于下限夹到 50%");
		config.functions.statusEffectDurationScale = 999;
		assertEquals(2.0f, StatusEffectTimerRenderer.effectScale(config), "高于上限夹到 200%");
	}

	// 缓存键任一变化（玩家/字体/GUI 宽度/演示模式）都必须失效，reset 后为空。
	@Test
	void timerCacheInvalidatesOnAnyLayoutKeyChange() {
		StatusEffectTimerRenderer.TimerCache cache = new StatusEffectTimerRenderer.TimerCache();
		Object player = new Object();
		Object font = new Object();
		StatusEffectTimerRenderer.CachedTimer timer = new StatusEffectTimerRenderer.CachedTimer(
			new StatusEffectTimerRenderer.TextPlacement("10", 87.0f, 17.0f, 1.0f, 4.5f), 0xFFFF5555);

		assertFalse(cache.matches(player, font, 100, false), "未更新前缓存无效");
		cache.update(List.of(timer), player, font, 100, false);
		assertTrue(cache.matches(player, font, 100, false));
		assertEquals(List.of(timer), cache.timers());

		assertFalse(cache.matches(new Object(), font, 100, false), "玩家变化失效");
		assertFalse(cache.matches(player, new Object(), 100, false), "字体变化失效");
		assertFalse(cache.matches(player, font, 101, false), "GUI 宽度变化失效");
		assertFalse(cache.matches(player, font, 100, true), "演示模式变化失效");

		cache.reset();
		assertFalse(cache.matches(player, font, 100, false));
		assertTrue(cache.timers().isEmpty());
	}

	// 缓存构建必须与逐条调用 placementFor 的旧路径完全一致（有益/有害两行）。
	@Test
	void cachedTimersMatchUncachedPlacementForBothRows() {
		StatusEffectTimerRenderer.EffectSnapshot beneficial =
			new StatusEffectTimerRenderer.EffectSnapshot(true, 200, true, false);
		StatusEffectTimerRenderer.EffectSnapshot harmful =
			new StatusEffectTimerRenderer.EffectSnapshot(true, 400, false, false);

		List<StatusEffectTimerRenderer.CachedTimer> timers = StatusEffectTimerRenderer.buildTimers(
			List.of(beneficial, harmful), String::length, 9, 100, false, 0, "auto", false);

		assertEquals(List.of(
			new StatusEffectTimerRenderer.CachedTimer(
				StatusEffectTimerRenderer.placementFor(String::length, 9, 200, false, 75, 1, false),
				StatusEffectTimerRenderer.timerColorArgb("auto", 200)),
			new StatusEffectTimerRenderer.CachedTimer(
				StatusEffectTimerRenderer.placementFor(String::length, 9, 400, false, 75, 27, false),
				StatusEffectTimerRenderer.timerColorArgb("auto", 400))), timers);
	}

	// 无尽效果占用槽位但不绘制；阈值外与不显示图标的效果不参与布局。
	@Test
	void infiniteEffectsReserveSlotsWhileThresholdAndHiddenIconsAreSkipped() {
		StatusEffectTimerRenderer.EffectSnapshot infinite =
			new StatusEffectTimerRenderer.EffectSnapshot(true, -1, true, true);
		StatusEffectTimerRenderer.EffectSnapshot finite =
			new StatusEffectTimerRenderer.EffectSnapshot(true, 200, true, false);
		StatusEffectTimerRenderer.EffectSnapshot longEffect =
			new StatusEffectTimerRenderer.EffectSnapshot(true, 400, true, false);
		StatusEffectTimerRenderer.EffectSnapshot hidden =
			new StatusEffectTimerRenderer.EffectSnapshot(false, 200, true, false);

		List<StatusEffectTimerRenderer.CachedTimer> timers = StatusEffectTimerRenderer.buildTimers(
			List.of(infinite, finite), String::length, 9, 100, false, 0, "auto", false);
		assertEquals(1, timers.size());
		assertEquals(StatusEffectTimerRenderer.placementFor(String::length, 9, 200, false, 50, 1, false),
			timers.get(0).placement(), "无尽效果占一个槽位但不绘制");

		assertTrue(StatusEffectTimerRenderer.buildTimers(List.of(longEffect), String::length, 9, 100,
			false, 10, "auto", false).isEmpty(), "阈值外效果被过滤");
		assertTrue(StatusEffectTimerRenderer.buildTimers(List.of(hidden), String::length, 9, 100,
			false, 0, "auto", false).isEmpty(), "不显示图标的效果不参与布局");
	}

	// 演示模式偏移与旧路径一致：两行整体下移 15 像素。
	@Test
	void demoOffsetShiftsRowsJustLikeTheUncachedPath() {
		StatusEffectTimerRenderer.EffectSnapshot beneficial =
			new StatusEffectTimerRenderer.EffectSnapshot(true, 200, true, false);
		StatusEffectTimerRenderer.EffectSnapshot harmful =
			new StatusEffectTimerRenderer.EffectSnapshot(true, 400, false, false);

		List<StatusEffectTimerRenderer.CachedTimer> timers = StatusEffectTimerRenderer.buildTimers(
			List.of(beneficial, harmful), String::length, 9, 100, true, 0, "auto", false);

		assertEquals(StatusEffectTimerRenderer.placementFor(String::length, 9, 200, false, 75, 16, false),
			timers.get(0).placement(), "有益效果随演示模式下移");
		assertEquals(StatusEffectTimerRenderer.placementFor(String::length, 9, 400, false, 75, 42, false),
			timers.get(1).placement(), "有害效果再下移一行");
	}

	// 文本格式在构建阶段完成：默认分:秒与“始终按秒”必须产出不同文本。
	@Test
	void cachedTextFollowsTheAlwaysSecondsFormat() {
		StatusEffectTimerRenderer.EffectSnapshot effect =
			new StatusEffectTimerRenderer.EffectSnapshot(true, 1250, true, false);

		StatusEffectTimerRenderer.CachedTimer normal = StatusEffectTimerRenderer.buildTimers(
			List.of(effect), String::length, 9, 100, false, 0, "auto", false).get(0);
		StatusEffectTimerRenderer.CachedTimer seconds = StatusEffectTimerRenderer.buildTimers(
			List.of(effect), String::length, 9, 100, false, 0, "auto", true).get(0);

		assertEquals("1:03", normal.placement().text());
		assertEquals("63", seconds.placement().text());
	}

	// 缓存更新后内容必须与同一输入的无缓存重建完全一致。
	@Test
	void cacheUpdatePublishesTheSameTimersAsAFreshBuild() {
		StatusEffectTimerRenderer.TimerCache cache = new StatusEffectTimerRenderer.TimerCache();
		Object player = new Object();
		Object font = new Object();
		List<StatusEffectTimerRenderer.EffectSnapshot> effects =
			List.of(new StatusEffectTimerRenderer.EffectSnapshot(true, 400, false, false));

		cache.update(StatusEffectTimerRenderer.buildTimers(effects, String::length, 9, 100, false, 0,
			"auto", false), player, font, 100, false);

		assertEquals(StatusEffectTimerRenderer.buildTimers(effects, String::length, 9, 100, false, 0,
			"auto", false), cache.timers(), "缓存内容与无缓存重建一致");
	}
}
