package com.gtzc.tweakify.client.state;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 穿牌打开容器的纯逻辑：潜行保留告示牌原版行为，候选位置优先取命中面的反方向。
 */
class SignContainerTargetsTest {
	@Test
	void opensOnlyWhenEnabledAndLookingAtSign() {
		assertTrue(SignContainerTargets.shouldOpenContainer(true, true, false));
	}

	@Test
	void sneakKeepsVanillaSignBehaviour() {
		assertFalse(SignContainerTargets.shouldOpenContainer(true, true, true),
			"潜行右键必须保留告示牌的编辑/放置行为");
	}

	// 关闭潜行放行后，潜行也会把交互转给告示牌背后的容器。
	@Test
	void disablingSneakBypassAllowsSneaking() {
		assertTrue(SignContainerTargets.shouldOpenContainer(true, false, true, true));
		assertFalse(SignContainerTargets.shouldOpenContainer(true, true, true, true));
	}

	@Test
	void disabledSwitchDoesNothing() {
		assertFalse(SignContainerTargets.shouldOpenContainer(false, true, false));
	}

	@Test
	void nonSignDoesNothing() {
		assertFalse(SignContainerTargets.shouldOpenContainer(true, false, false));
	}

	// 告示牌贴在箱子正面时，玩家命中的是正面，容器在命中面的反方向。
	@Test
	void oppositeOfHitFaceIsTriedFirst() {
		BlockPos sign = new BlockPos(10, 64, 10);
		List<BlockPos> candidates = SignContainerTargets.candidatePositions(sign, Direction.NORTH);

		assertEquals(sign.relative(Direction.SOUTH), candidates.getFirst());
		assertEquals(6, candidates.size(), "六个方向各一个候选，不重复");
		assertTrue(candidates.contains(sign.relative(Direction.UP)));
		assertTrue(candidates.contains(sign.relative(Direction.DOWN)));
	}

	@Test
	void candidateOrderIsStableWithoutHitFace() {
		BlockPos sign = BlockPos.ZERO;
		List<BlockPos> candidates = SignContainerTargets.candidatePositions(sign, null);

		assertEquals(6, candidates.size());
		assertEquals(sign.relative(Direction.values()[0]), candidates.getFirst());
	}

	// 没有世界时直接返回 null：Mixin 会保持原版流程。
	@Test
	void missingLevelHasNoTarget() {
		assertNull(SignContainerTargets.findContainer(null, BlockPos.ZERO, Direction.NORTH));
	}
}
