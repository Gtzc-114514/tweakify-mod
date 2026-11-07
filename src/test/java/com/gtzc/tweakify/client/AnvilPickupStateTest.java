package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnvilPickupStateTest {
	@Test
	void allowsWhenEnabledWithOutputAndCost() {
		assertTrue(AnvilPickupState.shouldAllowPickup(true, true, 1));
	}

	@Test
	void rejectsEmptyOutputSlot() {
		assertFalse(AnvilPickupState.shouldAllowPickup(true, false, 10),
			"空输出槽不能放行，否则 onTake 会拿到空物品仍扣经验");
	}

	@Test
	void rejectsZeroCost() {
		assertFalse(AnvilPickupState.shouldAllowPickup(true, true, 0));
	}

	@Test
	void rejectsWhenSwitchIsOff() {
		assertFalse(AnvilPickupState.shouldAllowPickup(false, true, 10));
	}
}
