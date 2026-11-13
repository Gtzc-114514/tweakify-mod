package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 覆盖挖掘冷却的拦截判定：仅「按住且已挖方块」时拦截。
 * 会话内的按下/松开边界由 {@link KeyHoldSessionTest} 统一覆盖。
 *
 * <p>三层保证的回归也在这里：标记只在写入它的那个 tick 内生效，
 * 开关关闭时即使标记残留也不拦截。
 */
class MiningCooldownStateTest {
	@BeforeEach
	void resetState() {
		MiningCooldownState.resetForTest();
	}

	@Test
	void blocksOnlyWhenHeldAndMined() {
		assertTrue(MiningCooldownState.computeBlockMiningThisTick(true, true));
		assertFalse(MiningCooldownState.computeBlockMiningThisTick(false, true));
		assertFalse(MiningCooldownState.computeBlockMiningThisTick(true, false));
	}

	// 查询入口自带开关判定：没有配置时不能拦截。
	@Test
	void shouldBlockIsFalseWithoutConfig() {
		withConfig(null, () -> {
			assertFalse(MiningCooldownState.shouldBlockMining());
			assertFalse(MiningCooldownState.shouldBlockAttack());
		});
	}

	// 三层保证的核心契约：标记只在写入它的那个 tick 内生效。
	@Test
	void markBlocksOnlyInTheTickItWasWritten() {
		withConfig(miningConfig(true, true), () -> {
			TickClock.advance();
			MiningCooldownState.updateFromInput(true);      // 按下左键
			MiningCooldownState.markBlockMined();           // 本 tick 挖掉方块
			MiningCooldownState.updateFromInput(true);      // 按住保持 -> 写入本次 tick 的标记
			assertTrue(MiningCooldownState.shouldBlockMining(), "same tick: mark must block mining");
			assertTrue(MiningCooldownState.shouldBlockAttack(), "same tick: mark must block attacks");

			TickClock.advance();                            // 进入下一 tick，状态机未更新
			assertFalse(MiningCooldownState.shouldBlockMining(),
				"next tick: stale mark must not block mining");
			assertFalse(MiningCooldownState.shouldBlockAttack(),
				"next tick: stale mark must not block attacks");
		});
	}

	// 第 2 层保证：总开关关闭时，即使标记残留也不拦截。
	@Test
	void disabledSwitchNeverBlocks() {
		withConfig(miningConfig(false, true), () -> {
			TickClock.advance();
			MiningCooldownState.updateFromInput(true);
			MiningCooldownState.markBlockMined();
			MiningCooldownState.updateFromInput(true);
			assertFalse(MiningCooldownState.shouldBlockMining());
			assertFalse(MiningCooldownState.shouldBlockAttack());
		});
	}

	// 攻击拦截依赖自己的子开关：miningCooldownAttacks 关闭时只拦挖掘。
	@Test
	void attackRequiresItsOwnSwitch() {
		withConfig(miningConfig(true, false), () -> {
			TickClock.advance();
			MiningCooldownState.updateFromInput(true);
			MiningCooldownState.markBlockMined();
			MiningCooldownState.updateFromInput(true);
			assertTrue(MiningCooldownState.shouldBlockMining());
			assertFalse(MiningCooldownState.shouldBlockAttack());
		});
	}

	private static TweakifyModConfig miningConfig(boolean miningCooldown, boolean miningCooldownAttacks) {
		TweakifyModConfig config = new TweakifyModConfig();
		config.functions.miningCooldown = miningCooldown;
		config.functions.miningCooldownAttacks = miningCooldownAttacks;
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
