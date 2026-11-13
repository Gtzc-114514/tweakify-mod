package com.gtzc.tweakify.client;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 刷怪蛋落点候选与判定注入：不依赖 Level，可在普通单测中覆盖边界顺序。
 */
class SpawnEggThrowerTest {
	@Test
	void blockHitCandidatesPreferAdjacentThenOrigin() {
		BlockPos origin = new BlockPos(10, 64, 10);
		BlockPos adjacent = new BlockPos(10, 64, 11);

		assertEquals(List.of(
			adjacent,
			adjacent.above(1),
			adjacent.above(2),
			adjacent.above(3),
			origin,
			origin.above(1),
			origin.above(2),
			origin.above(3)
		), SpawnEggThrower.spawnCandidates(origin, adjacent));
	}

	@Test
	void entityHitCandidatesOnlySearchAboveOrigin() {
		BlockPos origin = new BlockPos(10, 64, 10);

		assertEquals(List.of(
			origin,
			origin.above(1),
			origin.above(2),
			origin.above(3)
		), SpawnEggThrower.spawnCandidates(origin, null));
	}

	@Test
	void firstFreeCandidateWinsAndBlockedEndsNull() {
		BlockPos origin = new BlockPos(10, 64, 10);
		List<BlockPos> candidates = SpawnEggThrower.spawnCandidates(origin, null);
		BlockPos selected = candidates.get(2);

		assertEquals(selected, SpawnEggThrower.firstFreeSpawnPosition(candidates,
			pos -> pos.equals(selected)));
		assertNull(SpawnEggThrower.firstFreeSpawnPosition(candidates, pos -> false));
	}

	@Test
	void candidatePredicateCanRejectIndividualPositions() {
		BlockPos origin = new BlockPos(10, 64, 10);
		List<BlockPos> candidates = SpawnEggThrower.spawnCandidates(origin, null);

		assertEquals(candidates.get(3), SpawnEggThrower.firstFreeSpawnPosition(candidates,
			pos -> !pos.equals(origin) && !pos.equals(origin.above(1)) && !pos.equals(origin.above(2))));
	}
}
