package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锹单层铲雪判定：开关、手持锹、目标为多层雪层，三者缺一不可。
 */
class ShovelSnowLayerStateTest {

	@Test
	void reducesWhenAllConditionsMatch() {
		assertTrue(ShovelSnowLayerState.shouldReduceOneLayer(true, true, true, 2));
		assertTrue(ShovelSnowLayerState.shouldReduceOneLayer(true, true, true, 8));
	}

	@Test
	void requiresEnabledSwitch() {
		assertFalse(ShovelSnowLayerState.shouldReduceOneLayer(false, true, true, 8));
	}

	@Test
	void requiresShovelInMainHand() {
		assertFalse(ShovelSnowLayerState.shouldReduceOneLayer(true, false, true, 8));
	}

	@Test
	void requiresSnowLayerBlock() {
		assertFalse(ShovelSnowLayerState.shouldReduceOneLayer(true, true, false, 8));
	}

	@Test
	void singleLayerBreaksNormally() {
		assertFalse(ShovelSnowLayerState.shouldReduceOneLayer(true, true, true, 1));
	}
}
