package com.gtzc.tweakify.client.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StartupTimeRulesTest {
	@Test
	void formatsSecondsWithDotAndOneDecimal() {
		assertEquals("0.0", StartupTimeRules.formatSeconds(0L));
		assertEquals("1.5", StartupTimeRules.formatSeconds(1_500L));
		assertEquals("12.3", StartupTimeRules.formatSeconds(12_345L));
	}

	@Test
	void negativeUptimeIsClampedToZero() {
		assertEquals("0.0", StartupTimeRules.formatSeconds(-500L));
	}
}
