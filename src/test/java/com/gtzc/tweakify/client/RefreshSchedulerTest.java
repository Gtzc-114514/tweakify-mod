package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 固定频率刷新调度：每秒 6 次在 20 tick 内正好触发 6 次，间隔在 3/4 tick 之间交替，长期不漂移。
 */
class RefreshSchedulerTest {

	@Test
	void sixTimesPerSecondTriggersExactlySixInTwentyTicks() {
		RefreshScheduler scheduler = new RefreshScheduler(6);
		int triggers = 0;
		for (int i = 0; i < 20; i++) {
			if (scheduler.tick()) {
				triggers++;
			}
		}
		assertEquals(6, triggers, "20 tick 内正好 6 次");
	}

	@Test
	void intervalsAlternateBetweenThreeAndFourTicks() {
		RefreshScheduler scheduler = new RefreshScheduler(6);
		int last = -1;
		StringBuilder pattern = new StringBuilder();
		for (int tick = 0; tick < 20; tick++) {
			if (!scheduler.tick()) {
				continue;
			}
			if (last >= 0) {
				pattern.append(tick - last);
			}
			last = tick;
		}
		// 取模实现只能得到固定的 3 或 4，累计额度应该两者交替出现。
		assertTrue(pattern.toString().matches("[34]+"), "间隔应只由 3/4 组成，实际 " + pattern);
		assertTrue(pattern.indexOf("3") >= 0 && pattern.indexOf("4") >= 0, "3 和 4 都应出现，实际 " + pattern);
	}

	@Test
	void noDriftOverLongRun() {
		RefreshScheduler scheduler = new RefreshScheduler(6);
		int triggers = 0;
		for (int i = 0; i < 200; i++) {
			if (scheduler.tick()) {
				triggers++;
			}
		}
		assertEquals(60, triggers, "200 tick（10 秒）内正好 60 次");
	}

	@Test
	void resetClearsAccumulatedBudget() {
		RefreshScheduler scheduler = new RefreshScheduler(6);
		for (int i = 0; i < 3; i++) {
			assertFalse(scheduler.tick(), "前 3 tick 未到窗口");
		}
		scheduler.reset();
		assertFalse(scheduler.tick(), "重置后从 0 重新累计，不应立即触发");
	}

	@Test
	void rejectsOutOfRangeRates() {
		assertThrows(IllegalArgumentException.class, () -> new RefreshScheduler(0));
		assertThrows(IllegalArgumentException.class, () -> new RefreshScheduler(21));
	}

	@Test
	void oncePerSecondTriggersOnEveryTwentiethTick() {
		RefreshScheduler scheduler = new RefreshScheduler(1);
		int triggers = 0;
		for (int i = 0; i < 40; i++) {
			if (scheduler.tick()) {
				triggers++;
			}
		}
		assertEquals(2, triggers, "每秒 1 次时 40 tick 触发 2 次");
	}
}
