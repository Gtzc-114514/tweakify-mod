package com.gtzc.tweakify.client;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * T1 的真实 LevelChunk 集成测试。
 *
 * <p>每个场景在服务器世界里放置一个屏障与一个基岩（分属不同 section），
 * 用 {@link BlockMarkerScanner.ScanJob} 实际扫描 49³ 体积：
 * 只开屏障时基岩 section 必须被跳过，只开基岩时屏障 section 必须被跳过，两者全开时只扫描两个命中 section。
 * 断言实际逐方块读取数等于命中 section 的层数 × 目标区块的 16×16，
 * 若 {@code chunkMayContain} 被改成 {@code true}，读取数会变成完整体积（117649），测试立即失败。
 *
 * <p>GameTest 结构本身不参与扫描：测试区域在结构外 100 个区块处，避免默认结构边界与方块干扰。
 */
public class BlockMarkerScannerGameTest {
	private static final int RADIUS = 24;
	// 目标区块按 16×16 计算：中心固定在区块中心，扫描盒完整覆盖该区块。
	private static final int CHUNK_AREA = 16 * 16;
	// 三个场景固定在世界坐标上，彼此间隔 256 格，扫描盒不会互相覆盖。
	private static final int BASE_X = 100_000;
	private static final int BASE_Z = 100_000;
	private static final int SCENARIO_SPACING = 256;

	@GameTest(maxTicks = 100)
	public void barrierOnlySkipsBedrockSections(GameTestHelper helper) {
		runScenario(helper, true, false, 0);
	}

	@GameTest(maxTicks = 100)
	public void bedrockOnlySkipsBarrierSections(GameTestHelper helper) {
		runScenario(helper, false, true, 1);
	}

	@GameTest(maxTicks = 100)
	public void bothTargetsScanOnlyMatchingSections(GameTestHelper helper) {
		runScenario(helper, true, true, 2);
	}

	private static void runScenario(GameTestHelper helper, boolean barrier, boolean bedrock, int scenarioIndex) {
		ServerLevel level = helper.getLevel();
		// 固定 y=100，并让 x/z 落在目标区块中心：49 宽扫描盒覆盖目标区块的 16×16 全部列。
		BlockPos center = new BlockPos(
			BASE_X + scenarioIndex * SCENARIO_SPACING + 8,
			100,
			BASE_Z + 8);
		BlockPos barrierPos = center.offset(0, -RADIUS, 0);
		BlockPos bedrockPos = center.offset(0, RADIUS, 0);

		loadAndPlace(level, barrierPos, Blocks.BARRIER.defaultBlockState());
		loadAndPlace(level, bedrockPos, Blocks.BEDROCK.defaultBlockState());

		BlockMarkerScanner.ScanJob job = new BlockMarkerScanner.ScanJob(
			level, center, barrier, bedrock, RADIUS, RADIUS, 64);
		int guard = 0;
		while (!job.step()) {
			if (++guard > 10) {
				helper.assertTrue(false, "scan job did not finish");
				return;
			}
		}

		// 目标 section 的命中层数：扫描盒与 section 的 y 重叠部分。
		int scanMinY = center.getY() - RADIUS;
		int scanMaxY = center.getY() + RADIUS;
		int barrierLayers = overlapLayers(level, barrierPos.getY(), scanMinY, scanMaxY);
		int bedrockLayers = overlapLayers(level, bedrockPos.getY(), scanMinY, scanMaxY);
		int expectedReads = (barrier ? barrierLayers : 0) * CHUNK_AREA
			+ (bedrock ? bedrockLayers : 0) * CHUNK_AREA;
		int fullVolume = (RADIUS * 2 + 1) * (RADIUS * 2 + 1) * (RADIUS * 2 + 1);

		// 关键判别：预筛正确时只读命中 section；被改成 true 时会读满 117649 格。
		helper.assertTrue(job.blockReadsForTest() == expectedReads,
			"section prefilter read count mismatch: expected=" + expectedReads
				+ " actual=" + job.blockReadsForTest() + " full=" + fullVolume);

		if (barrier) {
			helper.assertTrue(job.barriersForTest().contains(barrierPos), "barrier was not discovered");
		} else {
			helper.assertTrue(job.barriersForTest().isEmpty(), "barrier must be ignored when only bedrock is enabled");
		}
		if (bedrock) {
			helper.assertTrue(job.markersForTest().stream()
				.anyMatch(marker -> marker.pos().equals(bedrockPos)), "bedrock marker was not discovered");
		} else {
			helper.assertTrue(job.markersForTest().isEmpty(),
				"bedrock must be ignored when only barrier is enabled");
		}
		helper.succeed();
	}

	private static void loadAndPlace(ServerLevel level, BlockPos pos, BlockState state) {
		// 先确保区块已加载；重复运行时目标方块可能已存在，此时 setBlockAndUpdate 会返回 false。
		level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
		if (level.getBlockState(pos).equals(state)) {
			return;
		}
		if (!level.setBlockAndUpdate(pos, state)) {
			throw new IllegalStateException("Could not place " + state + " at " + pos);
		}
	}

	// 扫描盒 [scanMinY, scanMaxY] 与该方块所在 section [bottom, bottom+15] 的重叠层数。
	private static int overlapLayers(ServerLevel level, int blockY, int scanMinY, int scanMaxY) {
		int minY = level.getMinY();
		int sectionBottom = minY + Math.floorDiv(blockY - minY, 16) * 16;
		int bottom = Math.max(scanMinY, sectionBottom);
		int top = Math.min(scanMaxY, sectionBottom + 15);
		return Math.max(0, top - bottom + 1);
	}
}
