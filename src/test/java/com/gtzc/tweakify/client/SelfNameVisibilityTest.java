package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 自身名字显示的组合表：只有配置开启、对象是本地玩家且四个边界全部放行时才显示。
 */
class SelfNameVisibilityTest {
	@Test
	void allBoundaryCombinationsMatchTheTruthTable() {
		for (int mask = 0; mask < 64; mask++) {
			boolean enabled = (mask & 1) != 0;
			boolean localPlayer = (mask & 2) != 0;
			boolean firstPerson = (mask & 4) != 0;
			boolean hudHidden = (mask & 8) != 0;
			boolean invisible = (mask & 16) != 0;
			boolean spectator = (mask & 32) != 0;
			boolean expected = enabled && localPlayer && !firstPerson && !hudHidden
				&& !invisible && !spectator;

			assertTrue(SelfNameVisibility.shouldShowSelfName(enabled, localPlayer, firstPerson,
				hudHidden, invisible, spectator) == expected,
				"unexpected result for mask " + mask);
		}
	}

	@Test
	void defaultVisibleCaseShowsName() {
		assertTrue(SelfNameVisibility.shouldShowSelfName(true, true,
			false, false, false, false));
	}

	@Test
	void eachBoundaryHidesName() {
		assertFalse(SelfNameVisibility.shouldShowSelfName(false, true, false, false, false, false));
		assertFalse(SelfNameVisibility.shouldShowSelfName(true, false, false, false, false, false));
		assertFalse(SelfNameVisibility.shouldShowSelfName(true, true, true, false, false, false));
		assertFalse(SelfNameVisibility.shouldShowSelfName(true, true, false, true, false, false));
		assertFalse(SelfNameVisibility.shouldShowSelfName(true, true, false, false, true, false));
		assertFalse(SelfNameVisibility.shouldShowSelfName(true, true, false, false, false, true));
	}
}
