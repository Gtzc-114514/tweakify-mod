package com.gtzc.tweakify.client.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锹单层铲雪判定：开关、手持锹、目标为多层雪层，三者缺一不可。
 */
class ShovelSnowLayerRulesTest {

	@Test
	void reducesWhenAllConditionsMatch() {
		assertTrue(ShovelSnowLayerRules.shouldReduceOneLayer(true, true, true, 2));
		assertTrue(ShovelSnowLayerRules.shouldReduceOneLayer(true, true, true, 8));
	}

	@Test
	void requiresEnabledSwitch() {
		assertFalse(ShovelSnowLayerRules.shouldReduceOneLayer(false, true, true, 8));
	}

	@Test
	void requiresShovelInMainHand() {
		assertFalse(ShovelSnowLayerRules.shouldReduceOneLayer(true, false, true, 8));
	}

	@Test
	void requiresSnowLayerBlock() {
		assertFalse(ShovelSnowLayerRules.shouldReduceOneLayer(true, true, false, 8));
	}

	@Test
	void singleLayerBreaksNormally() {
		assertFalse(ShovelSnowLayerRules.shouldReduceOneLayer(true, true, true, 1));
	}
}
