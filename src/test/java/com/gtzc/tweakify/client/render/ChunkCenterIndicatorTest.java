package com.gtzc.tweakify.client.render;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 区块中心指示的几何：竖线 x/z 固定在区块中心，y 覆盖整个世界高度。
 */
class ChunkCenterIndicatorTest {
	@Test
	void lineStartsAtWorldBottomAndEndsAtTop() {
		Vec3[] line = ChunkCenterIndicator.centerLine(
			SectionPos.of(new BlockPos(31, 64, -2)), -64, 319);
		assertEquals(24.0, line[0].x, "31 所在区块的最小 x 是 16，中心加 8");
		assertEquals(-8.0, line[0].z, "-2 所在区块的最小 z 是 -16，中心加 8");
		assertEquals(-64.0, line[0].y);
		assertEquals(24.0, line[1].x);
		assertEquals(-8.0, line[1].z);
		assertEquals(320.0, line[1].y, "最高点向上闭区间到 maxY + 1");
	}

	@Test
	void negativeCoordinatesUseSectionMinimum() {
		Vec3[] line = ChunkCenterIndicator.centerLine(
			SectionPos.of(new BlockPos(-1, 0, -17)), 0, 10);
		assertEquals(-8.0, line[0].x, "-1 所在区块的最小 x 是 -16");
		assertEquals(-24.0, line[0].z, "-17 所在区块的最小 z 是 -32");
	}
}
