package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 覆盖副手回退的拦截判定：按住期间主手耗尽、松键复位、换物品解除、
 * 副手未受限不拦、实体交互开关不在此层参与等场景。
 * 会话内的按下/松开边界由 {@link KeyHoldSessionTest} 统一覆盖。
 */
class OffhandFallbackStateTest {
	@BeforeEach
	void resetState() {
		OffhandFallbackState.resetForTest();
	}

	// --- 拦截判定 ---

	@Test
	void blocksWhenMainHandRanOutWhileRestricted() {
		// 按住 + 按下时主手有物品 + 现在主手空 + 副手受限 -> 拦截。
		assertTrue(OffhandFallbackState.computeBlockThisTick(true, true, true, true));
	}

	@Test
	void doesNotBlockWhenKeyReleased() {
		assertFalse(OffhandFallbackState.computeBlockThisTick(false, true, true, true));
	}

	@Test
	void doesNotBlockWhenPressedWithEmptyMainHand() {
		// 按下时主手就是空的：不属于“用完接替”场景。
		assertFalse(OffhandFallbackState.computeBlockThisTick(true, false, true, true));
	}

	@Test
	void doesNotBlockWhenMainHandStillHasItem() {
		assertFalse(OffhandFallbackState.computeBlockThisTick(true, true, false, true));
	}

	@Test
	void doesNotBlockWhenOffhandNotRestricted() {
		assertFalse(OffhandFallbackState.computeBlockThisTick(true, true, true, false));
	}

	// --- 组合序列 ---

	@Test
	void fullSequencePressHoldRunOutRelease() {
		KeyHoldSession session = new KeyHoldSession();

		// 按下：主手有物品。
		boolean mainHandHadItem = session.update(true, true);
		assertFalse(OffhandFallbackState.computeBlockThisTick(true, mainHandHadItem, false, true));

		// 按住：主手物品用完变空，副手受限 -> 进入拦截窗口。
		mainHandHadItem = session.update(true, false);
		assertTrue(OffhandFallbackState.computeBlockThisTick(true, mainHandHadItem, true, true));

		// 继续按住，但玩家换上了新的主手物品 -> 解除拦截。
		mainHandHadItem = session.update(true, true);
		assertFalse(OffhandFallbackState.computeBlockThisTick(true, mainHandHadItem, false, true));

		// 松开 -> 会话复位。
		mainHandHadItem = session.update(false, false);
		assertFalse(mainHandHadItem);
	}

	// --- 命中类型拦截判定 ---

	@Test
	void alwaysCancelsMiss() {
		// 无命中时走的是副手物品使用（吃、拉弓等），不受两个交互开关影响。
		assertTrue(OffhandFallbackState.shouldCancelHit(OffhandFallbackState.HitTarget.MISS, false, false, false, false));
	}

	@Test
	void blockInteractionFollowsItsSwitch() {
		assertTrue(OffhandFallbackState.shouldCancelHit(OffhandFallbackState.HitTarget.BLOCK, true, false, false, false));
		assertFalse(OffhandFallbackState.shouldCancelHit(OffhandFallbackState.HitTarget.BLOCK, false, true, false, false));
	}

	@Test
	void blockSwitchOffStillBlocksOffhandBlockPlacement() {
		// 拦截方块交互关闭时：副手是方块物品且命中的不是可交互方块 -> 仍拦截（副手回推不丢）。
		assertTrue(OffhandFallbackState.shouldCancelHit(OffhandFallbackState.HitTarget.BLOCK, false, false, true, false));
		// 命中门、拉杆等可交互方块 -> 放行交互。
		assertFalse(OffhandFallbackState.shouldCancelHit(OffhandFallbackState.HitTarget.BLOCK, false, false, true, true));
		// 副手不是方块物品 -> 放行。
		assertFalse(OffhandFallbackState.shouldCancelHit(OffhandFallbackState.HitTarget.BLOCK, false, false, false, false));
		// 副手方块物品未命中交互方块时，即使实体开关开启也不影响方块判定。
		assertTrue(OffhandFallbackState.shouldCancelHit(OffhandFallbackState.HitTarget.BLOCK, false, true, true, false));
	}

	@Test
	void entityInteractionFollowsItsSwitch() {
		assertTrue(OffhandFallbackState.shouldCancelHit(OffhandFallbackState.HitTarget.ENTITY, false, true, true, true));
		assertFalse(OffhandFallbackState.shouldCancelHit(OffhandFallbackState.HitTarget.ENTITY, true, false, false, true));
	}

	@Test
	void nullTargetIsTreatedAsMiss() {
		assertTrue(OffhandFallbackState.shouldCancelHit(null, false, false, false, false));
	}

	// 查询入口自带开关判定：没有配置时不能拦截。
	@Test
	void shouldBlockOffhandFallbackIsFalseWithoutConfig() {
		withConfig(null, () -> assertFalse(OffhandFallbackState.shouldBlockOffhandFallback()));
	}

	// 三层保证的核心契约：副手回退标记只在写入它的那个 tick 内生效。
	@Test
	void markBlocksOnlyInTheTickItWasWritten() {
		withConfig(offhandConfig(true), () -> {
			TickClock.advance();
			OffhandFallbackState.updateFromInput(true, true, true, true);
			assertTrue(OffhandFallbackState.shouldBlockOffhandFallback(), "same tick must block");

			TickClock.advance();
			assertFalse(OffhandFallbackState.shouldBlockOffhandFallback(), "next tick must not block");
		});
	}

	// 第 2 层保证：总开关关闭时，即使标记残留也不拦截。
	@Test
	void disabledSwitchNeverBlocks() {
		withConfig(offhandConfig(false), () -> {
			TickClock.advance();
			OffhandFallbackState.updateFromInput(true, true, true, true);
			assertFalse(OffhandFallbackState.shouldBlockOffhandFallback());
		});
	}

	private static TweakifyModConfig offhandConfig(boolean blockOffhandFallback) {
		TweakifyModConfig config = new TweakifyModConfig();
		config.blockOffhandFallback = blockOffhandFallback;
		return config;
	}

	// 保存并恢复全局配置：范围限定在单个测试内，不受测试执行顺序影响。
	private static void withConfig(TweakifyModConfig config, Runnable body) {
		TweakifyModConfig previous = TweakifyMod.CONFIG;
		TweakifyMod.CONFIG = config;
		try {
			body.run();
		} finally {
			TweakifyMod.CONFIG = previous;
		}
	}
}
