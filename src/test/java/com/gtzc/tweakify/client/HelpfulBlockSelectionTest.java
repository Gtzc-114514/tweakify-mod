package com.gtzc.tweakify.client;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 辅助方块选区：参考实现覆盖的方块集合、栅栏整格化与细长方块外扩，
 * 以及配置/潜行/手持匹配三重门控。
 */
class HelpfulBlockSelectionTest {
	private static final double EPSILON = 1.0e-6;
	// 细长方块的代表形状：锁链的中间柱。
	private static final VoxelShape THIN = Shapes.box(0.4375, 0.0, 0.4375, 0.5625, 1.0, 0.5625);

	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void helpfulBlocksCoverReferenceSet() {
		assertTrue(HelpfulBlockSelection.isHelpfulBlock(Blocks.OAK_FENCE));
		assertTrue(HelpfulBlockSelection.isHelpfulBlock(Blocks.IRON_BARS));
		assertTrue(HelpfulBlockSelection.isHelpfulBlock(Blocks.IRON_CHAIN));
		assertTrue(HelpfulBlockSelection.isHelpfulBlock(Blocks.END_ROD));
		assertTrue(HelpfulBlockSelection.isHelpfulBlock(Blocks.LIGHTNING_ROD.asList().getFirst()));
		assertTrue(HelpfulBlockSelection.isHelpfulBlock(Blocks.POINTED_DRIPSTONE));
		assertTrue(HelpfulBlockSelection.isHelpfulBlock(Blocks.BAMBOO));
		assertFalse(HelpfulBlockSelection.isHelpfulBlock(Blocks.STONE));
	}

	@Test
	void fencesAndIronBarsUseFullBlockSelection() {
		assertSame(Shapes.block(),
			HelpfulBlockSelection.adjust(true, Blocks.OAK_FENCE, THIN, false, true));
		assertSame(Shapes.block(),
			HelpfulBlockSelection.adjust(true, Blocks.IRON_BARS, THIN, false, true));
	}

	@Test
	void thinBlocksExpandByTwoSixteenths() {
		VoxelShape expanded = HelpfulBlockSelection.adjust(true, Blocks.IRON_CHAIN, THIN, false, true);
		AABB bounds = expanded.bounds();

		assertEquals(0.4375 - HelpfulBlockSelection.EXPAND, bounds.minX, EPSILON);
		assertEquals(0.5625 + HelpfulBlockSelection.EXPAND, bounds.maxX, EPSILON);
		assertEquals(0.0, bounds.minY, EPSILON, "外扩在方块边界处应被夹紧");
		assertEquals(1.0, bounds.maxY, EPSILON);
	}

	@Test
	void expandClampsToBlockBounds() {
		AABB bounds = HelpfulBlockSelection.expand(Shapes.block()).bounds();

		assertEquals(0.0, bounds.minX, EPSILON);
		assertEquals(0.0, bounds.minY, EPSILON);
		assertEquals(0.0, bounds.minZ, EPSILON);
		assertEquals(1.0, bounds.maxX, EPSILON);
		assertEquals(1.0, bounds.maxY, EPSILON);
		assertEquals(1.0, bounds.maxZ, EPSILON);
	}

	@Test
	void gatingKeepsVanillaShape() {
		assertSame(THIN, HelpfulBlockSelection.adjust(false, Blocks.IRON_CHAIN, THIN, false, true),
			"功能关闭时保持原版选区");
		assertSame(THIN, HelpfulBlockSelection.adjust(true, Blocks.IRON_CHAIN, THIN, true, true),
			"潜行时保持原版选区");
		assertSame(THIN, HelpfulBlockSelection.adjust(true, Blocks.IRON_CHAIN, THIN, false, false),
			"未手持同类方块时保持原版选区");
		assertSame(THIN, HelpfulBlockSelection.adjust(true, Blocks.STONE, THIN, false, true),
			"普通方块不调整选区");
	}

	@Test
	void emptyShapeIsNeverReplaced() {
		assertSame(Shapes.empty(), HelpfulBlockSelection.adjust(true, Blocks.IRON_CHAIN, Shapes.empty(), false, true));
		assertSame(Shapes.empty(), HelpfulBlockSelection.expand(Shapes.empty()));
	}
}
