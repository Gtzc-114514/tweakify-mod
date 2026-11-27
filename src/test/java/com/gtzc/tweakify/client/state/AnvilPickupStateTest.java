package com.gtzc.tweakify.client.state;

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

	// 费用上限：不超过上限时放行，超过且非“仅提示”时阻止。
	@Test
	void thresholdAllowsUpToMaxCost() {
		assertTrue(AnvilPickupState.shouldAllowPickup(true, true, 30, 30, false));
		assertFalse(AnvilPickupState.shouldAllowPickup(true, true, 31, 30, false));
		assertTrue(AnvilPickupState.shouldAllowResult(true, 30, 30, false));
		assertFalse(AnvilPickupState.shouldAllowResult(true, 31, 30, false));
	}

	// 仅提示模式：超过上限仍可取，但仍显示提示。
	@Test
	void warnOnlyAllowsOverThreshold() {
		assertTrue(AnvilPickupState.shouldAllowResult(true, 100, 30, true));
		assertTrue(AnvilPickupState.shouldAllowPickup(true, true, 100, 30, true));
		assertTrue(AnvilPickupState.shouldShowTooExpensiveLabel(true, 100, 30));
	}

	@Test
	void blockByCostOnlyWhenOverLimitAndNotWarnOnly() {
		assertTrue(AnvilPickupState.shouldBlockByCost(true, 31, 30, false));
		assertFalse(AnvilPickupState.shouldBlockByCost(true, 30, 30, false));
		assertFalse(AnvilPickupState.shouldBlockByCost(true, 31, 30, true));
		assertFalse(AnvilPickupState.shouldBlockByCost(true, 31, 0, false), "0 = 不限制");
	}

	@Test
	void labelOnlyWhenOverLimit() {
		assertTrue(AnvilPickupState.shouldShowTooExpensiveLabel(true, 31, 30));
		assertFalse(AnvilPickupState.shouldShowTooExpensiveLabel(true, 30, 30));
		assertFalse(AnvilPickupState.shouldShowTooExpensiveLabel(true, 100, 0));
		assertFalse(AnvilPickupState.shouldShowTooExpensiveLabel(false, 100, 30));
	}

	@Test
	void unlimitedKeepsOldBehaviour() {
		assertTrue(AnvilPickupState.shouldAllowResult(true, 1000, 0, false));
		assertFalse(AnvilPickupState.shouldBlockByCost(true, 1000, 0, false));
	}
}
