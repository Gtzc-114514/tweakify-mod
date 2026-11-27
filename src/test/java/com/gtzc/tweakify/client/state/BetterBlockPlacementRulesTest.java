package com.gtzc.tweakify.client.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 更好的放置覆盖的潜行门控：只有“开启潜行放行 + 正在潜行”同时成立才交回原版。
 */
class BetterBlockPlacementRulesTest {

	@Test
	void sneakBypassRequiresEnabledOptionAndSneaking() {
		assertTrue(BetterBlockPlacementRules.shouldBypassForSneak(true, true));
		assertFalse(BetterBlockPlacementRules.shouldBypassForSneak(true, false));
		assertFalse(BetterBlockPlacementRules.shouldBypassForSneak(false, true));
		assertFalse(BetterBlockPlacementRules.shouldBypassForSneak(false, false));
	}
}
