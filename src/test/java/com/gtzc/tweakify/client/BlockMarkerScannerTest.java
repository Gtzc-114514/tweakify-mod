package com.gtzc.tweakify.client;

import net.minecraft.world.level.chunk.LevelChunkSection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 方块扫描的刷新频率、目标推导与 section 预筛。
 *
 * <p>section 预筛覆盖：屏障/基岩四种开关组合下，只跳过不含任何目标方块的 section；
 * 越界索引与缺失 section 同样跳过。
 */
class BlockMarkerScannerTest {

	// 需求数值用字面量固定：常量被改动时必须在这里同步暴露，避免测试跟着常量漂移。
	@Test
	void refreshRateMatchesRequirement() {
		assertEquals(6, BlockMarkerScanner.REFRESHES_PER_SECOND, "屏障/基岩每秒刷新 6 次");
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

	@Test
	void sectionIndexBoundsAreRejected() {
		assertFalse(BlockMarkerScanner.isValidSectionIndex(-1, 24), "世界底部以下的越界索引");
		assertFalse(BlockMarkerScanner.isValidSectionIndex(24, 24), "世界顶部以上的越界索引");
		assertFalse(BlockMarkerScanner.isValidSectionIndex(0, 0), "没有 section 的空区块");
		assertTrue(BlockMarkerScanner.isValidSectionIndex(0, 24));
		assertTrue(BlockMarkerScanner.isValidSectionIndex(23, 24));
	}

	@Test
	void nullSectionIsSkipped() {
		for (BlockMarkerScanner.ScanTarget target : BlockMarkerScanner.ScanTarget.values()) {
			assertFalse(BlockMarkerScanner.sectionNeedsScan((LevelChunkSection) null, target),
				"缺失 section 在 " + target + " 下都应跳过");
		}
		assertFalse(BlockMarkerScanner.sectionNeedsScan((LevelChunkSection) null, null));
	}
}
