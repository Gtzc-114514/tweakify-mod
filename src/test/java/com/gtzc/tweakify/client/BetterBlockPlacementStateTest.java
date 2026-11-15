package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 更好的放置覆盖的潜行门控：只有“开启潜行放行 + 正在潜行”同时成立才交回原版。
 */
class BetterBlockPlacementStateTest {

	@Test
	void sneakBypassRequiresEnabledOptionAndSneaking() {
		assertTrue(BetterBlockPlacementState.shouldBypassForSneak(true, true));
		assertFalse(BetterBlockPlacementState.shouldBypassForSneak(true, false));
		assertFalse(BetterBlockPlacementState.shouldBypassForSneak(false, true));
		assertFalse(BetterBlockPlacementState.shouldBypassForSneak(false, false));
	}
}
