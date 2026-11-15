package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * MiniHUD 刷新间隔：毫秒换算纳秒并夹紧到 50 ~ 1000。
 */
class MiniHudStateTest {

	@Test
	void intervalMapsToNanos() {
		assertEquals(150_000_000L, MiniHudState.intervalNanos(150));
		assertEquals(50_000_000L, MiniHudState.intervalNanos(50));
	}

	@Test
	void intervalIsClamped() {
		assertEquals(50_000_000L, MiniHudState.intervalNanos(1));
		assertEquals(1_000_000_000L, MiniHudState.intervalNanos(9999));
	}
}
