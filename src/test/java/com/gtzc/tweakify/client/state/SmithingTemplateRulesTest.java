package com.gtzc.tweakify.client.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锻造模板保留概率：100% 必定保留，0% 完全消耗，中间按 roll 判定。
 */
class SmithingTemplateRulesTest {

	@Test
	void disabledNeverKeeps() {
		assertFalse(SmithingTemplateRules.shouldKeepTemplate(false, 100, 0));
	}

	@Test
	void hundredPercentAlwaysKeeps() {
		assertTrue(SmithingTemplateRules.shouldKeepTemplate(true, 100, 0));
		assertTrue(SmithingTemplateRules.shouldKeepTemplate(true, 100, 99));
	}

	@Test
	void zeroPercentAlwaysConsumes() {
		assertFalse(SmithingTemplateRules.shouldKeepTemplate(true, 0, 0));
		assertFalse(SmithingTemplateRules.shouldKeepTemplate(true, 0, 99));
	}

	@Test
	void chanceUsesRollBelowThreshold() {
		assertTrue(SmithingTemplateRules.shouldKeepTemplate(true, 30, 0));
		assertTrue(SmithingTemplateRules.shouldKeepTemplate(true, 30, 29));
		assertFalse(SmithingTemplateRules.shouldKeepTemplate(true, 30, 30));
		assertFalse(SmithingTemplateRules.shouldKeepTemplate(true, 30, 99));
	}

	@Test
	void chanceIsClamped() {
		assertTrue(SmithingTemplateRules.shouldKeepTemplate(true, 999, 99));
		assertFalse(SmithingTemplateRules.shouldKeepTemplate(true, -10, 0));
	}
}
