package com.gtzc.tweakify.client.render;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.BarrierBlock;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 屏障判定：只有屏障方块本身需要标记，水logged 不影响判定。
 */
class BarrierVisibilityStateTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void barrierStateMatches() {
		assertTrue(BarrierVisibilityState.isBarrier(Blocks.BARRIER.defaultBlockState()));
		assertTrue(BarrierVisibilityState.isBarrier(
			Blocks.BARRIER.defaultBlockState().setValue(BarrierBlock.WATERLOGGED, true)));
	}

	@Test
	void otherBlocksDoNotMatch() {
		assertFalse(BarrierVisibilityState.isBarrier(Blocks.STONE.defaultBlockState()));
		assertFalse(BarrierVisibilityState.isBarrier(Blocks.AIR.defaultBlockState()));
	}

	// 标记上限：超出时保留离中心最近的方块，且不修改调用方传入的列表顺序。
	@Test
	void markerLimitKeepsClosestBlocks() {
		BlockPos center = new BlockPos(0, 64, 0);
		List<BlockPos> found = new ArrayList<>(List.of(
			center.offset(30, 0, 0), center.offset(2, 0, 0), center.offset(10, 0, 0)));

		BarrierVisibilityState.setMarkers(found, center, 2);

		assertEquals(2, BarrierVisibilityState.markers().size());
		assertEquals(center.offset(2, 0, 0), BarrierVisibilityState.markers().get(0),
			"最近的方块排在第一位");
		assertEquals(found.size(), 3, "调用方列表不应被修改");
		BarrierVisibilityState.clear();
	}

	@Test
	void nonPositiveMarkerLimitFallsBackToDefault() {
		BlockPos center = new BlockPos(0, 64, 0);
		List<BlockPos> found = List.of(center.offset(1, 0, 0));

		BarrierVisibilityState.setMarkers(found, center, 0);

		assertEquals(1, BarrierVisibilityState.markers().size(), "非正数上限回退到默认值，不会清空标记");
		BarrierVisibilityState.clear();
	}

	// 扫描半径：扫描盒大于配置半径时，角落方块必须被丢弃，只保留圆内标记。
	@Test
	void scanRadiusFiltersOutCornerMarkers() {
		BlockPos center = new BlockPos(0, 64, 0);
		List<BlockPos> found = List.of(
			center.offset(3, 0, 0), center.offset(9, 0, 9), center.offset(20, 0, 0));

		BarrierVisibilityState.setMarkers(found, center, 256, 8);

		assertEquals(List.of(center.offset(3, 0, 0)), BarrierVisibilityState.markers(),
			"半径 8 时只保留距离不超出 8 格的方块");
		BarrierVisibilityState.clear();
	}
}
