package com.gtzc.tweakify.client.render;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

/**
 * T1 的真实 LevelChunk 集成测试。
 *
 * <p>每个场景在服务器世界里放置一个屏障与一个基岩（分属不同 section），
 * 用 {@link BlockMarkerScanner.ScanJob} 实际扫描 49³（负 y 场景为 17³）体积：
 * 只开屏障时基岩 section 必须被跳过，只开基岩时屏障 section 必须被跳过，两者全开时只扫描两个命中 section；
 * 另有两个负 y 场景，分别覆盖世界最底层 section 0 与扫描盒越过 minY 时的越界跳过。
 * 断言实际逐方块读取数与“读取过的 section 集合”都精确匹配目标组合；
 * 若 {@code chunkMayContain} 被改成 {@code true}，读取数会变成完整体积，测试立即失败。
 *
 * <p>测试区域远离 GameTest 结构网格，避免默认结构边界与方块干扰。
 */
public class BlockMarkerScannerGameTest {
	private static final int RADIUS = 24;
	// 目标区块按 16×16 计算：中心固定在区块中心，扫描盒完整覆盖该区块。
	private static final int CHUNK_AREA = 16 * 16;
	// 场景固定在世界坐标上，彼此间隔 256 格，扫描盒不会互相覆盖。
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

	// 世界最底层：中心 -56、半径 8，屏障放在 -64（section 0 的下边界）。
	@GameTest(maxTicks = 100)
	public void bottomSectionStartsAtWorldMin(GameTestHelper helper) {
		runScenario(helper, true, false, 3, -56, 8, -8, 8);
	}

	// 扫描盒越过 minY：中心 -60、半径 8 → -68..-52，屏障在 -60，越界层必须被跳过。
	@GameTest(maxTicks = 100)
	public void belowWorldMinSkipsInvalidSections(GameTestHelper helper) {
		runScenario(helper, true, false, 4, -60, 8, 0, 8);
	}

	private static void runScenario(GameTestHelper helper, boolean barrier, boolean bedrock, int scenarioIndex) {
		runScenario(helper, barrier, bedrock, scenarioIndex, 100, RADIUS, -RADIUS, RADIUS);
	}

	private static void runScenario(GameTestHelper helper, boolean barrier, boolean bedrock, int scenarioIndex,
			int centerY, int radius) {
		runScenario(helper, barrier, bedrock, scenarioIndex, centerY, radius, -radius, radius);
	}

	private static void runScenario(GameTestHelper helper, boolean barrier, boolean bedrock, int scenarioIndex,
			int centerY, int radius, int barrierOffset, int bedrockOffset) {
		helper.assertTrue(radius >= 8, "radius must cover the full target chunk");
		ServerLevel level = helper.getLevel();
		// 让 x/z 落在目标区块中心：扫描盒完整覆盖目标区块的 16×16 全部列。
		BlockPos center = new BlockPos(
			BASE_X + scenarioIndex * SCENARIO_SPACING + 8,
			centerY,
			BASE_Z + 8);
		BlockPos barrierPos = center.offset(0, barrierOffset, 0);
		BlockPos bedrockPos = center.offset(0, bedrockOffset, 0);

		loadAndPlace(level, barrierPos, Blocks.BARRIER.defaultBlockState());
		loadAndPlace(level, bedrockPos, Blocks.BEDROCK.defaultBlockState());

		// 最后一个 true 开启仅测试使用的 section 诊断集合；生产 createJob 不开启。
		BlockMarkerScanner.ScanJob job = new BlockMarkerScanner.ScanJob(
			level, center, barrier, bedrock, radius, radius, 64, true);
		int guard = 0;
		while (!job.step()) {
			if (++guard > 10) {
				helper.assertTrue(false, "scan job did not finish");
			}
		}

		// 目标 section 的命中层数：扫描盒与 section 的 y 重叠部分。
		int scanMinY = center.getY() - radius;
		int scanMaxY = center.getY() + radius;
		int barrierLayers = overlapLayers(level, barrierPos.getY(), scanMinY, scanMaxY);
		int bedrockLayers = overlapLayers(level, bedrockPos.getY(), scanMinY, scanMaxY);
		int expectedReads = (barrier ? barrierLayers : 0) * CHUNK_AREA
			+ (bedrock ? bedrockLayers : 0) * CHUNK_AREA;
		int fullVolume = (radius * 2 + 1) * (radius * 2 + 1) * (radius * 2 + 1);

		// 关键判别一：读取数只应包含命中 section 的层；改成 true 时会读满完整体积。
		helper.assertTrue(job.blockReadsForTest() == expectedReads,
			"section prefilter read count mismatch: expected=" + expectedReads
				+ " actual=" + job.blockReadsForTest() + " full=" + fullVolume);

		// 关键判别二：读取过的 section 集合必须精确等于目标组合，防止 target 写反或漏扫。
		Set<BlockMarkerScanner.ScanJob.SectionKey> expectedSections = new HashSet<>();
		if (barrier) {
			expectedSections.add(sectionKey(level, barrierPos));
		}
		if (bedrock) {
			expectedSections.add(sectionKey(level, bedrockPos));
		}
		helper.assertTrue(job.sectionsReadForTest().equals(expectedSections),
			"section set mismatch: expected=" + expectedSections
				+ " actual=" + job.sectionsReadForTest());

		if (barrier) {
			helper.assertTrue(job.barriersForTest().contains(barrierPos), "barrier was not discovered");
		}
		if (bedrock) {
			helper.assertTrue(job.markersForTest().stream()
				.anyMatch(marker -> marker.pos().equals(bedrockPos)), "bedrock marker was not discovered");
		}
		helper.succeed();
	}

	private static void loadAndPlace(ServerLevel level, BlockPos pos, BlockState state) {
		// 先确保区块已加载，再清空自身与上下邻居并回读校验；
		// 负 y 场景需要上下为空气，BedrockHighlightState.markerAt 才会把基岩视为单层暴露。
		level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
		clearIfInWorld(level, pos);
		clearIfInWorld(level, pos.offset(0, 1, 0));
		clearIfInWorld(level, pos.offset(0, -1, 0));
		if (!level.setBlockAndUpdate(pos, state)) {
			throw new IllegalStateException("Could not place " + state + " at " + pos);
		}
		if (!level.getBlockState(pos).equals(state)) {
			throw new IllegalStateException("Block state mismatch after placing at " + pos);
		}
		if (state.is(Blocks.BEDROCK)
			&& (!level.getBlockState(pos.offset(0, 1, 0)).isAir()
				|| !level.getBlockState(pos.offset(0, -1, 0)).isAir())) {
			throw new IllegalStateException("Bedrock neighbors are not air at " + pos);
		}
	}

	// 世界底部的 -65 等位置不可写；清理邻居时先按 minY..maxY 过滤。
	private static void clearIfInWorld(ServerLevel level, BlockPos pos) {
		int minY = level.getMinY();
		int maxY = minY + level.getHeight() - 1;
		if (pos.getY() >= minY && pos.getY() <= maxY) {
			level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
		}
	}

	private static BlockMarkerScanner.ScanJob.SectionKey sectionKey(ServerLevel level, BlockPos pos) {
		return new BlockMarkerScanner.ScanJob.SectionKey(
			pos.getX() >> 4, pos.getZ() >> 4, level.getSectionIndex(pos.getY()));
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
