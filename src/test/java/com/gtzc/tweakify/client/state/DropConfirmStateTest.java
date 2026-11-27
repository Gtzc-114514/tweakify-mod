package com.gtzc.tweakify.client.state;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DropConfirmStateTest {
	@BeforeEach
	void resetState() {
		DropConfirmState.reset();
	}

	@Test
	void emptyListMatchesEverything() {
		assertTrue(DropConfirmState.matchesList("minecraft:stone", List.of()));
		assertTrue(DropConfirmState.matchesList("minecraft:stone", null));
	}

	@Test
	void configuredListLimitsConfirmation() {
		List<String> items = List.of("minecraft:diamond");
		assertTrue(DropConfirmState.matchesList("minecraft:diamond", items));
		assertFalse(DropConfirmState.matchesList("minecraft:stone", items));
	}

	@Test
	void secondPressWithinWindowAllowsDrop() {
		long now = 10_000L;
		assertEquals(DropConfirmState.Decision.BLOCK,
			DropConfirmState.evaluate(true, List.of(), "minecraft:stone", now));
		assertEquals(DropConfirmState.Decision.ALLOW,
			DropConfirmState.evaluate(true, List.of(), "minecraft:stone", now + 2_999L));
		assertFalse(DropConfirmState.isPendingForTest(), "放行后必须清掉待确认状态");
	}

	@Test
	void secondPressAfterWindowBlocksAgain() {
		long now = 10_000L;
		DropConfirmState.evaluate(true, List.of(), "minecraft:stone", now);
		assertEquals(DropConfirmState.Decision.BLOCK,
			DropConfirmState.evaluate(true, List.of(), "minecraft:stone", now + 3_001L));
		assertTrue(DropConfirmState.isPendingForTest());
	}

	@Test
	void windowBoundaryIsInclusive() {
		long now = 10_000L;
		DropConfirmState.evaluate(true, List.of(), "minecraft:stone", now);
		assertEquals(DropConfirmState.Decision.ALLOW,
			DropConfirmState.evaluate(true, List.of(), "minecraft:stone", now + 3_000L));
	}

	@Test
	void switchingItemRestartsConfirmation() {
		long now = 10_000L;
		DropConfirmState.evaluate(true, List.of(), "minecraft:stone", now);
		assertEquals(DropConfirmState.Decision.BLOCK,
			DropConfirmState.evaluate(true, List.of(), "minecraft:dirt", now + 1_000L));
		assertEquals(DropConfirmState.Decision.ALLOW,
			DropConfirmState.evaluate(true, List.of(), "minecraft:dirt", now + 2_000L));
	}

	@Test
	void disabledFeatureNeverBlocksAndClearsState() {
		long now = 10_000L;
		DropConfirmState.evaluate(true, List.of(), "minecraft:stone", now);
		assertEquals(DropConfirmState.Decision.IGNORE,
			DropConfirmState.evaluate(false, List.of(), "minecraft:stone", now + 1_000L));
		assertFalse(DropConfirmState.isPendingForTest());
	}

	@Test
	void unlistedItemKeepsVanillaBehavior() {
		assertEquals(DropConfirmState.Decision.IGNORE, DropConfirmState.evaluate(
			true, List.of("minecraft:diamond"), "minecraft:stone", 10_000L));
	}

	@Test
	void emptyItemIdIsIgnored() {
		assertEquals(DropConfirmState.Decision.IGNORE,
			DropConfirmState.evaluate(true, List.of(), null, 10_000L));
		assertEquals(DropConfirmState.Decision.IGNORE,
			DropConfirmState.evaluate(true, List.of(), "", 10_000L));
	}
}
