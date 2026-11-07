package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 覆盖使用冷却的纯判定函数：只有“对应手开关开启 && 该手已使用 && 物品在列表”才拦截。
 */
class UseCooldownStateTest {

	@Test
	void shouldBlockHandRequiresAllConditions() {
		assertTrue(UseCooldownState.shouldBlockHand(true, true, true));
		assertFalse(UseCooldownState.shouldBlockHand(false, true, true));
		assertFalse(UseCooldownState.shouldBlockHand(true, false, true));
		assertFalse(UseCooldownState.shouldBlockHand(true, true, false));
	}
}
