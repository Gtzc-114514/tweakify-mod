package com.gtzc.tweakify.client;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.BarrierBlock;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
}
