package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 方块扫描的两项降耗判定：静止阈值边界与 section 预筛的开关组合。
 *
 * <p>静止判定覆盖：位移恰好 0.05 格不触发移动、40 tick 临界值、静止与移动互转、
 * 对角线位移按平方和判断、重置与失活后的基线重建。
 * <p>section 预筛覆盖：屏障/基岩四种开关组合下，只跳过不含任何目标方块的 section。
 */
class BlockMarkerScannerTest {

	// ===== 静止判定 =====

	@Test
	void firstUpdateInitializesWithoutReportingMovement() {
		BlockMarkerScanner.IdleTracker tracker = new BlockMarkerScanner.IdleTracker();
		assertFalse(tracker.update(10.0, 64.0, -3.0), "首次观测只建立基线");
		assertFalse(tracker.isIdle());
	}

	@Test
	void displacementExactlyAtThresholdIsNotMovement() {
		BlockMarkerScanner.IdleTracker tracker = new BlockMarkerScanner.IdleTracker();
		tracker.reset(0.0, 0.0, 0.0);
		// 位移恰好 0.05 格：平方值与阈值相等，判定使用严格大于，因此不算移动。
		double delta = 0.05;
		assertEquals(BlockMarkerScanner.IdleTracker.MOVE_EPSILON_SQ, delta * delta, 0.0);
		for (int i = 0; i < BlockMarkerScanner.IdleTracker.IDLE_TRIGGER_TICKS; i++) {
			assertFalse(tracker.update(delta, 0.0, 0.0), "阈值边界不应重置静止计数");
		}
		assertTrue(tracker.isIdle(), "持续处于阈值边界也应正常进入静止");
	}

	@Test
	void displacementJustAboveThresholdResetsIdleCount() {
		BlockMarkerScanner.IdleTracker tracker = new BlockMarkerScanner.IdleTracker();
		tracker.reset(0.0, 0.0, 0.0);
		for (int i = 0; i < BlockMarkerScanner.IdleTracker.IDLE_TRIGGER_TICKS; i++) {
			tracker.update(0.0, 0.0, 0.0);
		}
		assertTrue(tracker.isIdle());
		assertTrue(tracker.update(0.050_001, 0.0, 0.0), "从静止转为移动应触发补扫");
		assertFalse(tracker.isIdle());
	}

	@Test
	void idleTriggersExactlyAtTriggerTick() {
		BlockMarkerScanner.IdleTracker tracker = new BlockMarkerScanner.IdleTracker();
		tracker.reset(0.0, 0.0, 0.0);
		for (int i = 0; i < BlockMarkerScanner.IdleTracker.IDLE_TRIGGER_TICKS - 1; i++) {
			tracker.update(0.0, 0.0, 0.0);
			assertFalse(tracker.isIdle(), "第 " + (i + 1) + " tick 还不应进入静止");
		}
		tracker.update(0.0, 0.0, 0.0);
		assertTrue(tracker.isIdle(), "第 40 tick 应进入静止");
	}

	@Test
	void continuousMovementNeverReportsStartedMoving() {
		BlockMarkerScanner.IdleTracker tracker = new BlockMarkerScanner.IdleTracker();
		tracker.reset(0.0, 0.0, 0.0);
		assertFalse(tracker.update(0.1, 0.0, 0.0), "移动中不算从静止转为移动");
		assertFalse(tracker.isIdle());
		assertFalse(tracker.update(0.2, 0.0, 0.0));
		// 同一位置的重复观测位移为 0，只会累计静止计数，不会误报移动。
		assertFalse(tracker.update(0.2, 0.0, 0.0));
		assertFalse(tracker.isIdle());
	}

	@Test
	void diagonalDisplacementUsesSquaredDistance() {
		BlockMarkerScanner.IdleTracker tracker = new BlockMarkerScanner.IdleTracker();
		tracker.reset(0.0, 0.0, 0.0);
		// 单轴 0.04 都低于 0.05，但平方和 0.0032 大于阈值 0.0025，仍判定为移动。
		assertFalse(tracker.update(0.04, 0.0, 0.04));
		assertFalse(tracker.isIdle());
	}

	@Test
	void idleThenMoveThenIdleAgainFollowsFullCycle() {
		BlockMarkerScanner.IdleTracker tracker = new BlockMarkerScanner.IdleTracker();
		tracker.reset(0.0, 0.0, 0.0);
		for (int i = 0; i < BlockMarkerScanner.IdleTracker.IDLE_TRIGGER_TICKS; i++) {
			tracker.update(0.0, 0.0, 0.0);
		}
		assertTrue(tracker.isIdle());
		assertTrue(tracker.update(1.0, 0.0, 0.0));
		assertFalse(tracker.isIdle());
		// 重新静止后应能在下一个 40 tick 再次进入静止，而不是一直停在移动状态。
		for (int i = 0; i < BlockMarkerScanner.IdleTracker.IDLE_TRIGGER_TICKS - 1; i++) {
			tracker.update(1.0, 0.0, 0.0);
		}
		assertFalse(tracker.isIdle());
		tracker.update(1.0, 0.0, 0.0);
		assertTrue(tracker.isIdle());
	}

	@Test
	void resetStartsFreshBaseline() {
		BlockMarkerScanner.IdleTracker tracker = new BlockMarkerScanner.IdleTracker();
		tracker.reset(100.0, 70.0, 100.0);
		assertFalse(tracker.update(100.0, 70.0, 100.0));
		// 重置到新位置后，旧基线与新位置之间的跳跃不会被当成移动。
		tracker.reset(-200.0, 30.0, 500.0);
		assertFalse(tracker.update(-200.0, 30.0, 500.0));
		assertFalse(tracker.isIdle());
	}

	@Test
	void clearRequiresReinitialization() {
		BlockMarkerScanner.IdleTracker tracker = new BlockMarkerScanner.IdleTracker();
		tracker.reset(0.0, 0.0, 0.0);
		for (int i = 0; i < BlockMarkerScanner.IdleTracker.IDLE_TRIGGER_TICKS; i++) {
			tracker.update(0.0, 0.0, 0.0);
		}
		assertTrue(tracker.isIdle());
		tracker.clear();
		assertFalse(tracker.isIdle());
		assertFalse(tracker.update(1.0, 2.0, 3.0), "失活后第一次观测只重建基线");
	}

	// ===== section 预筛 =====

	@Test
	void bothSwitchesOffProduceNoTarget() {
		assertNull(BlockMarkerScanner.ScanTarget.of(false, false));
		assertFalse(BlockMarkerScanner.sectionNeedsScan(true, true, null), "没有目标时应跳过所有 section");
	}

	@Test
	void barrierOnlyTargetIgnoresBedrockSections() {
		BlockMarkerScanner.ScanTarget target = BlockMarkerScanner.ScanTarget.of(true, false);
		assertEquals(BlockMarkerScanner.ScanTarget.BARRIER, target);
		assertTrue(BlockMarkerScanner.sectionNeedsScan(true, false, target));
		assertTrue(BlockMarkerScanner.sectionNeedsScan(true, true, target));
		assertFalse(BlockMarkerScanner.sectionNeedsScan(false, true, target), "只开屏障时纯基岩 section 应跳过");
		assertFalse(BlockMarkerScanner.sectionNeedsScan(false, false, target));
	}

	@Test
	void bedrockOnlyTargetIgnoresBarrierSections() {
		BlockMarkerScanner.ScanTarget target = BlockMarkerScanner.ScanTarget.of(false, true);
		assertEquals(BlockMarkerScanner.ScanTarget.BEDROCK, target);
		assertTrue(BlockMarkerScanner.sectionNeedsScan(true, true, target));
		assertFalse(BlockMarkerScanner.sectionNeedsScan(true, false, target), "只开基岩时纯屏障 section 应跳过");
		assertFalse(BlockMarkerScanner.sectionNeedsScan(false, false, target));
	}

	@Test
	void bothTargetsScanSectionsWithEitherBlock() {
		BlockMarkerScanner.ScanTarget target = BlockMarkerScanner.ScanTarget.of(true, true);
		assertEquals(BlockMarkerScanner.ScanTarget.BOTH, target);
		assertTrue(BlockMarkerScanner.sectionNeedsScan(true, false, target));
		assertTrue(BlockMarkerScanner.sectionNeedsScan(false, true, target));
		assertTrue(BlockMarkerScanner.sectionNeedsScan(true, true, target));
		assertFalse(BlockMarkerScanner.sectionNeedsScan(false, false, target), "两种方块都没有的 section 应跳过");
	}

	@Test
	void emptySectionIsSkippedForEveryTarget() {
		for (BlockMarkerScanner.ScanTarget target : BlockMarkerScanner.ScanTarget.values()) {
			assertFalse(BlockMarkerScanner.sectionNeedsScan(false, false, target),
				"空 section 在 " + target + " 下都应跳过");
		}
	}
}
