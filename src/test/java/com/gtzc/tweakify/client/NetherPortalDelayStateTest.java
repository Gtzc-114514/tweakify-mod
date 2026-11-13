package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 地狱门传送延迟判定：总开关与子开关只覆盖对应模式，其余情况保持原版。
 */
class NetherPortalDelayStateTest {
	private static final int SURVIVAL_DELAY = 80;

	@Test
	void masterSwitchOffKeepsVanilla() {
		assertEquals(NetherPortalDelayState.NO_OVERRIDE,
			override(false, true, true, true, false, SURVIVAL_DELAY));
	}

	@Test
	void nonPlayerKeepsVanilla() {
		assertEquals(NetherPortalDelayState.NO_OVERRIDE,
			override(true, true, true, false, false, SURVIVAL_DELAY));
	}

	@Test
	void survivalInstantTeleportsImmediately() {
		assertEquals(0, override(true, true, false, true, false, SURVIVAL_DELAY));
	}

	@Test
	void survivalWithoutInstantKeepsVanilla() {
		assertEquals(NetherPortalDelayState.NO_OVERRIDE,
			override(true, false, false, true, false, SURVIVAL_DELAY));
	}

	@Test
	void creativeDelayedUsesSurvivalDelay() {
		assertEquals(SURVIVAL_DELAY, override(true, false, true, true, true, SURVIVAL_DELAY));
	}

	@Test
	void creativeWithoutDelayedKeepsVanilla() {
		assertEquals(NetherPortalDelayState.NO_OVERRIDE,
			override(true, false, false, true, true, SURVIVAL_DELAY));
	}

	@Test
	void negativeSurvivalDelayIsClamped() {
		assertEquals(0, override(true, false, true, true, true, -20));
	}

	// 参数顺序：总开关、生存秒传、创造延迟、是否玩家、是否无敌、生存延迟。
	private static int override(boolean master, boolean survivalInstant, boolean creativeDelayed,
								boolean isPlayer, boolean invulnerable, int survivalDelay) {
		return NetherPortalDelayState.overrideDelay(
			master, survivalInstant, creativeDelayed, isPlayer, invulnerable, survivalDelay);
	}
}
