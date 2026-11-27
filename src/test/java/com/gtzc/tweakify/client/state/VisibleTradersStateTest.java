package com.gtzc.tweakify.client.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 可见交易：只显示锁定交易时，原始数量必须按 0 计算，客户端才会把所有行标成锁定。
 */
class VisibleTradersStateTest {

	@Test
	void originalCountKeepsRealSizeByDefault() {
		assertEquals(4, VisibleTradersState.originalCount(false, 4));
		assertEquals(0, VisibleTradersState.originalCount(false, 0));
	}

	@Test
	void onlyLockedForcesZeroOriginalCount() {
		assertEquals(0, VisibleTradersState.originalCount(true, 4));
	}

	@Test
	void negativeSizeIsClampedToZero() {
		assertEquals(0, VisibleTradersState.originalCount(false, -3));
	}

	@Test
	void futureLevelsAreClampedAndLimitedByAvailableGroups() {
		assertEquals(4, VisibleTradersState.futureLevelsToShow(4, 4));
		assertEquals(1, VisibleTradersState.futureLevelsToShow(0, 4));
		assertEquals(4, VisibleTradersState.futureLevelsToShow(99, 4));
		assertEquals(2, VisibleTradersState.futureLevelsToShow(3, 2));
		assertEquals(0, VisibleTradersState.futureLevelsToShow(3, 0));
	}

	@Test
	void lockedOfferLimitTreatsZeroAsUnlimited() {
		assertEquals(12, VisibleTradersState.limitLockedOffers(0, 12));
		assertEquals(0, VisibleTradersState.limitLockedOffers(0, 0));
		assertEquals(5, VisibleTradersState.limitLockedOffers(5, 12));
		assertEquals(12, VisibleTradersState.limitLockedOffers(64, 12));
		assertEquals(12, VisibleTradersState.limitLockedOffers(99, 12));
		assertEquals(0, VisibleTradersState.limitLockedOffers(5, -1));
	}
}
