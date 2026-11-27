package com.gtzc.tweakify.client.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锻造模板保留概率：100% 必定保留，0% 完全消耗，中间按 roll 判定。
 */
class SmithingTemplateStateTest {

	@Test
	void disabledNeverKeeps() {
		assertFalse(SmithingTemplateState.shouldKeepTemplate(false, 100, 0));
	}

	@Test
	void hundredPercentAlwaysKeeps() {
		assertTrue(SmithingTemplateState.shouldKeepTemplate(true, 100, 0));
		assertTrue(SmithingTemplateState.shouldKeepTemplate(true, 100, 99));
	}

	@Test
	void zeroPercentAlwaysConsumes() {
		assertFalse(SmithingTemplateState.shouldKeepTemplate(true, 0, 0));
		assertFalse(SmithingTemplateState.shouldKeepTemplate(true, 0, 99));
	}

	@Test
	void chanceUsesRollBelowThreshold() {
		assertTrue(SmithingTemplateState.shouldKeepTemplate(true, 30, 0));
		assertTrue(SmithingTemplateState.shouldKeepTemplate(true, 30, 29));
		assertFalse(SmithingTemplateState.shouldKeepTemplate(true, 30, 30));
		assertFalse(SmithingTemplateState.shouldKeepTemplate(true, 30, 99));
	}

	@Test
	void chanceIsClamped() {
		assertTrue(SmithingTemplateState.shouldKeepTemplate(true, 999, 99));
		assertFalse(SmithingTemplateState.shouldKeepTemplate(true, -10, 0));
	}
}
