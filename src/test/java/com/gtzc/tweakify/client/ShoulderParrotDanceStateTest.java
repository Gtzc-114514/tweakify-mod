package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 肩膀鹦鹉跳舞的玩家资格：可限定只对本地玩家生效。
 */
class ShoulderParrotDanceStateTest {

	@Test
	void allPlayersWhenNotRestricted() {
		assertTrue(ShoulderParrotDanceState.isEligiblePlayer(false, true));
		assertTrue(ShoulderParrotDanceState.isEligiblePlayer(false, false));
	}

	@Test
	void localOnlyRequiresLocalPlayer() {
		assertTrue(ShoulderParrotDanceState.isEligiblePlayer(true, true));
		assertFalse(ShoulderParrotDanceState.isEligiblePlayer(true, false));
	}
}
