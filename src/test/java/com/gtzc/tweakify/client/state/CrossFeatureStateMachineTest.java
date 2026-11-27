package com.gtzc.tweakify.client.state;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.TweakifyModTestSupport;
import com.gtzc.tweakify.client.ui.FastScroll;
import com.gtzc.tweakify.client.ui.SmoothScrollAnimator;
import com.gtzc.tweakify.client.ui.SmoothScrolling;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.decoration.ArmorStand.ArmorStandPose;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 三条跨功能状态机路径：
 * 消耗品优化 × 副手回退、快速滚动 × 创造栏布局、盔甲架姿态 × 红石上升沿。
 */
class CrossFeatureStateMachineTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@BeforeEach
	void resetState() {
		OffhandFallbackState.resetForTest();
	}

	// 进食等待窗口与副手回退标记互不干扰：前者跨 tick 保留，后者只在写入它的 tick 生效。
	@Test
	void consumableWaitWindowOutlivesOffhandBlockWindow() {
		withConfig(offhandConfig(true), () -> {
			ConsumableOptimizationState state = new ConsumableOptimizationState();
			long now = 1_000L;
			long wait = ConsumableOptimizationState.adaptiveServerWaitMillis(400);
			state.startServerWait(now, wait);

			TickClock.advance();
			OffhandFallbackState.updateFromInput(true, true, true, true);
			assertTrue(state.isWaitingForServer(now + wait), "进食窗口跨 tick 保留");
			assertTrue(OffhandFallbackState.shouldBlockOffhandFallback(), "同一 tick 内副手拦截生效");

			TickClock.advance();
			assertTrue(state.isWaitingForServer(now + wait), "副手标记过期不影响进食窗口");
			assertFalse(OffhandFallbackState.shouldBlockOffhandFallback(), "副手拦截只在写入它的 tick 生效");
		});
	}

	// 放大后的滚轮增量进入创造栏的「行」动画：整行、半行进位与越界夹紧都保持合法。
	@Test
	void fastScrollMultipliesCreativeRowsAndKeepsSubRowCarry() {
		// 一格滚轮 × 10 倍 → 10 行，整行滚动时无亚行偏移。
		SmoothScrollAnimator whole = creativeAnimator();
		whole.addDelta(-FastScroll.scaleAmount(-1.0D, true, 10, true), 20.0D);
		settle(whole, 20.0D);
		SmoothScrolling.CreativeScrollLayout wholeLayout =
			SmoothScrolling.creativeLayout(whole.position(), SmoothScrolling.CREATIVE_SLOT_SIZE);
		assertEquals(10, wholeLayout.row());
		assertEquals(0, wholeLayout.shiftPixels());

		// 半格 × 3 倍 → 1.5 行：进位到第 1 行，偏移半格（9 像素），不越界。
		SmoothScrollAnimator half = creativeAnimator();
		half.addDelta(-FastScroll.scaleAmount(-0.5D, true, 3, true), 20.0D);
		settle(half, 20.0D);
		SmoothScrolling.CreativeScrollLayout halfLayout =
			SmoothScrolling.creativeLayout(half.position(), SmoothScrolling.CREATIVE_SLOT_SIZE);
		assertEquals(1, halfLayout.row());
		assertEquals(9, halfLayout.shiftPixels());

		// 放大后超过可滚动行数：夹到最大行，偏移仍为 0。
		SmoothScrollAnimator clamped = creativeAnimator();
		clamped.addDelta(-FastScroll.scaleAmount(-1.0D, true, 10, true), 6.0D);
		settle(clamped, 6.0D);
		SmoothScrolling.CreativeScrollLayout clampedLayout =
			SmoothScrolling.creativeLayout(clamped.position(), SmoothScrolling.CREATIVE_SLOT_SIZE);
		assertEquals(6, clampedLayout.row());
		assertEquals(0, clampedLayout.shiftPixels());
	}

	// 同一 tick 内的潜行右键与红石上升沿各循环一次；持续通电不再切换。
	@Test
	void armorStandSneakCycleAndRedstoneEdgeAdvanceOnceEachInSameTick() {
		List<ArmorStandPose> presets = ArmorStandPoses.resolve(List.of("default", "sitting", "waving"));
		ArmorStandRedstoneTrigger trigger = new ArmorStandRedstoneTrigger();
		ArmorStandPose pose = presets.get(0);

		assertFalse(trigger.update(false), "首个 tick 只播种电平");
		// 同一 tick：潜行右键循环一次，红石上升沿再循环一次。
		pose = ArmorStandPoses.next(presets, pose);
		assertTrue(trigger.update(true));
		pose = ArmorStandPoses.next(presets, pose);
		assertEquals(presets.get(2), pose);

		// 持续通电不再切换；断电再通电是一次新的上升沿。
		assertFalse(trigger.update(true));
		assertFalse(trigger.update(false));
		assertTrue(trigger.update(true));
	}

	// 区块重载/重进世界后线路仍通电：首个 tick 只播种，不白跳一个姿态。
	@Test
	void seededPoweredArmorStandDoesNotAdvanceOnReload() {
		ArmorStandRedstoneTrigger trigger = new ArmorStandRedstoneTrigger();

		assertFalse(trigger.update(true), "重建后首次看到通电只记录电平");
		assertFalse(trigger.update(true), "线路保持通电不会反复切换");
	}

	// 与创造栏 mixin 相同的动画单位与吸附阈值（半像素）。
	private static SmoothScrollAnimator creativeAnimator() {
		SmoothScrollAnimator animator = new SmoothScrollAnimator();
		animator.setSnapDistance(0.5D / SmoothScrolling.CREATIVE_SLOT_SIZE);
		return animator;
	}

	// 大时间步长推进两帧：第一帧逼近目标，第二帧按吸附阈值对齐。
	private static void settle(SmoothScrollAnimator animator, double max) {
		animator.update(1.0D, max);
		animator.update(1.0D, max);
	}

	private static TweakifyModConfig offhandConfig(boolean blockOffhandFallback) {
		TweakifyModConfig config = new TweakifyModConfig();
		config.functions.blockOffhandFallback = blockOffhandFallback;
		return config;
	}

	private static void withConfig(TweakifyModConfig config, Runnable body) {
		TweakifyModConfig previous = TweakifyMod.config();
		TweakifyModTestSupport.installConfig(config);
		try {
			body.run();
		} finally {
			TweakifyModTestSupport.installConfig(previous);
		}
	}
}
