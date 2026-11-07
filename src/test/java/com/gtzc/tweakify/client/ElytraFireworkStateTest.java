package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 鞘翅烟花限制的解锁条件：观察到烟花后消失代表加速结束；
 * 没看到烟花时用超时兜底，避免服务端拒绝使用后一直锁死。
 */
class ElytraFireworkStateTest {
	@Test
	void releasesOnlyAfterRocketDisappearsOrTimeout() {
		assertFalse(ElytraFireworkState.shouldRelease(false, false, 1),
			"rocket may not have reached the client yet");
		assertFalse(ElytraFireworkState.shouldRelease(false, false, 40),
			"still within the appear timeout");
		assertTrue(ElytraFireworkState.shouldRelease(false, false, 41),
			"no rocket appeared in time: release the lock");
		assertTrue(ElytraFireworkState.shouldRelease(true, false, 1),
			"rocket seen and now gone: boost ended");
		assertFalse(ElytraFireworkState.shouldRelease(true, true, 10),
			"rocket is still attached: keep blocking");
	}

	@Test
	void markUsedBlocksUntilReset() {
		ElytraFireworkState.reset();
		assertFalse(ElytraFireworkState.shouldBlockUse());
		ElytraFireworkState.markUsed();
		assertTrue(ElytraFireworkState.shouldBlockUse());
		ElytraFireworkState.reset();
		assertFalse(ElytraFireworkState.shouldBlockUse());
	}
}
