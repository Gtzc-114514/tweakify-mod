package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 覆盖维度传送拦截的边界：门开关是该门及其黑名单的总开关；
 * 开启后黑名单为空拦全部，非空只拦名单内实体；关闭时黑名单不生效。
 */
class DimensionTeleportStateTest {

	@Test
	void emptyBlacklistBlocksEverythingWhenSwitchOn() {
		assertTrue(DimensionTeleportState.shouldBlock(true, true, false, true, true, false));
		assertTrue(DimensionTeleportState.shouldBlock(true, true, true, false, true, false));
	}

	@Test
	void nonEmptyBlacklistOnlyBlocksListedEntities() {
		assertTrue(DimensionTeleportState.shouldBlock(true, true, false, true, false, true),
			"listed entity is blocked");
		assertFalse(DimensionTeleportState.shouldBlock(true, true, false, true, false, false),
			"unlisted entity keeps teleporting");
	}

	@Test
	void blacklistIsIgnoredWhenPortalSwitchIsOff() {
		assertFalse(DimensionTeleportState.shouldBlock(false, false, false, true, false, true),
			"the portal switch also controls its blacklist");
	}

	@Test
	void disabledPortalTypeIsNotBlocked() {
		assertFalse(DimensionTeleportState.shouldBlock(false, true, true, false, true, false),
			"end portal switch is off");
		assertFalse(DimensionTeleportState.shouldBlock(true, false, false, true, true, false),
			"nether portal switch is off");
	}

	@Test
	void endGatewayUsesItsOwnSwitchAndBlacklist() {
		assertTrue(DimensionTeleportState.shouldBlockPortalWithBlacklist(true, true, false),
			"empty gateway blacklist blocks everything");
		assertTrue(DimensionTeleportState.shouldBlockPortalWithBlacklist(true, false, true),
			"listed entity is blocked");
		assertFalse(DimensionTeleportState.shouldBlockPortalWithBlacklist(true, false, false),
			"unlisted entity keeps teleporting");
		assertFalse(DimensionTeleportState.shouldBlockPortalWithBlacklist(false, true, true),
			"gateway switch off ignores its blacklist");
	}

	@Test
	void otherPortalsAreIgnored() {
		assertFalse(DimensionTeleportState.shouldBlock(true, true, false, false, true, true),
			"this helper only handles the end portal and the nether portal");
	}
}
