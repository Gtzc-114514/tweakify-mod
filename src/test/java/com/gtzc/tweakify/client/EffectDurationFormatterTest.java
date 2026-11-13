package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 药水效果时间格式：秒 / 分:秒 / 2h05 / 2d05，且剩余时间向上取整。
 */
class EffectDurationFormatterTest {
	@Test
	void formatsSecondsUnderOneMinute() {
		assertEquals("0", EffectDurationFormatter.format(0));
		assertEquals("45", EffectDurationFormatter.format(900));
	}

	@Test
	void roundsPartialSecondsUp() {
		assertEquals("1", EffectDurationFormatter.format(1));
		assertEquals("1", EffectDurationFormatter.format(19));
		assertEquals("1", EffectDurationFormatter.format(20));
		assertEquals("2", EffectDurationFormatter.format(21));
	}

	@Test
	void formatsMinutesAndHours() {
		assertEquals("1:00", EffectDurationFormatter.format(1200));
		assertEquals("40:00", EffectDurationFormatter.format(20 * (40 * 60)));
		assertEquals("6h40", EffectDurationFormatter.format(20 * (400 * 60)));
		assertEquals("1h02", EffectDurationFormatter.format(20 * (3600 + 123)));
		assertEquals("1h00", EffectDurationFormatter.format(20 * 3600));
		assertEquals("1d00", EffectDurationFormatter.format(20 * 86400));
		assertEquals("13h53", EffectDurationFormatter.format(999999));
		assertEquals("11d13", EffectDurationFormatter.format(20 * 999999));
		assertEquals("100d", EffectDurationFormatter.format(20 * (100 * 86400)));
	}

	@Test
	void longestOutputStaysWithinFiveCharacters() {
		for (int seconds : new int[] {59, 3599, 86399, 99 * 86400 + 23 * 3600, 100 * 86400}) {
			String text = EffectDurationFormatter.format(20 * seconds);
			assertTrue(text.length() <= 5, "duration text too long: " + text);
		}
		String maxTicks = EffectDurationFormatter.format(Integer.MAX_VALUE);
		assertEquals("1242d", maxTicks);
	}

	@Test
	void clampsNegativeTicks() {
		assertEquals("0", EffectDurationFormatter.format(-20));
	}
}
