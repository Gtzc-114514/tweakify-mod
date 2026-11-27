package com.gtzc.tweakify.client.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 主动爬行的纯布尔门控：按住状态（总开关/玩家/按键）与姿态生效条件
 * （总开关/按住/受控玩家/落地要求）。实体与环境判定仍由实机回归覆盖。
 */
class ForceCrawlStateTest {

	@Test
	void holdStateRequiresMasterSwitchPlayerAndKey() {
		assertTrue(ForceCrawlState.shouldHoldCrawlKey(true, true, true));
		assertFalse(ForceCrawlState.shouldHoldCrawlKey(false, true, true), "总开关关闭");
		assertFalse(ForceCrawlState.shouldHoldCrawlKey(true, false, true), "没有玩家");
		assertFalse(ForceCrawlState.shouldHoldCrawlKey(true, true, false), "按键未按住");
	}

	@Test
	void forceCrawlRequiresMasterSwitchHeldKeyAndControlledPlayer() {
		assertTrue(ForceCrawlState.shouldForceCrawl(true, true, true, false, false));
		assertFalse(ForceCrawlState.shouldForceCrawl(false, true, true, false, true), "总开关关闭");
		assertFalse(ForceCrawlState.shouldForceCrawl(true, false, true, false, true), "按键未按住");
		assertFalse(ForceCrawlState.shouldForceCrawl(true, true, false, false, true), "非受控玩家");
	}

	@Test
	void groundRequirementOnlyAppliesWhenEnabled() {
		assertFalse(ForceCrawlState.shouldForceCrawl(true, true, true, true, false),
			"仅落地时生效但玩家在空中");
		assertTrue(ForceCrawlState.shouldForceCrawl(true, true, true, true, true));
		assertTrue(ForceCrawlState.shouldForceCrawl(true, true, true, false, false),
			"不要求落地时空中也允许");
	}
}
