package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 攻击范围的刷新频率需求：每秒 12 次。
 */
class AttackRangeStateTest {

	// 需求数值用字面量固定：常量被改动时必须在这里同步暴露，避免测试跟着常量漂移。
	@Test
	void refreshRateMatchesRequirement() {
		assertEquals(12, AttackRangeState.REFRESHES_PER_SECOND, "攻击范围每秒刷新 12 次");
	}
}
