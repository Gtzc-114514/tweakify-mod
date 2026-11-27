package com.gtzc.tweakify.client.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 覆盖维度传送拦截的边界：门开关是该门及其黑名单的总开关；
 * 开启后黑名单为空拦全部，非空只拦名单内实体；关闭时黑名单不生效。
 */
class DimensionTeleportRulesTest {

	@Test
	void emptyBlacklistBlocksEverythingWhenSwitchOn() {
		assertTrue(DimensionTeleportRules.shouldBlock(true, true, false, true, true, false));
		assertTrue(DimensionTeleportRules.shouldBlock(true, true, true, false, true, false));
	}

	@Test
	void nonEmptyBlacklistOnlyBlocksListedEntities() {
		assertTrue(DimensionTeleportRules.shouldBlock(true, true, false, true, false, true),
			"listed entity is blocked");
		assertFalse(DimensionTeleportRules.shouldBlock(true, true, false, true, false, false),
			"unlisted entity keeps teleporting");
	}

	@Test
	void blacklistIsIgnoredWhenPortalSwitchIsOff() {
		assertFalse(DimensionTeleportRules.shouldBlock(false, false, false, true, false, true),
			"the portal switch also controls its blacklist");
	}

	@Test
	void disabledPortalTypeIsNotBlocked() {
		assertFalse(DimensionTeleportRules.shouldBlock(false, true, true, false, true, false),
			"end portal switch is off");
		assertFalse(DimensionTeleportRules.shouldBlock(true, false, false, true, true, false),
			"nether portal switch is off");
	}

	@Test
	void endGatewayUsesItsOwnSwitchAndBlacklist() {
		assertTrue(DimensionTeleportRules.shouldBlockPortalWithBlacklist(true, true, false),
			"empty gateway blacklist blocks everything");
		assertTrue(DimensionTeleportRules.shouldBlockPortalWithBlacklist(true, false, true),
			"listed entity is blocked");
		assertFalse(DimensionTeleportRules.shouldBlockPortalWithBlacklist(true, false, false),
			"unlisted entity keeps teleporting");
		assertFalse(DimensionTeleportRules.shouldBlockPortalWithBlacklist(false, true, true),
			"gateway switch off ignores its blacklist");
	}

	@Test
	void otherPortalsAreIgnored() {
		assertFalse(DimensionTeleportRules.shouldBlock(true, true, false, false, true, true),
			"this helper only handles the end portal and the nether portal");
	}
}
