package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * 覆盖 tick 序号的基本语义：每次 advance 递增 1。
 * 状态机据此让「跨 tick 的残留标记」失效，因此序号必须严格单调。
 */
class TickClockTest {
	@Test
	void advancesMonotonically() {
		long before = TickClock.current();
		TickClock.advance();
		assertEquals(before + 1, TickClock.current());
		TickClock.advance();
		assertEquals(before + 2, TickClock.current());
	}

	// 跨 tick 失效的底层保证：advance() 后旧序号一定不再等于当前序号。
	@Test
	void advanceInvalidatesPreviousValue() {
		long previous = TickClock.current();
		TickClock.advance();
		assertNotEquals(previous, TickClock.current());
	}
}
