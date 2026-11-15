package com.gtzc.tweakify.client;

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
}
