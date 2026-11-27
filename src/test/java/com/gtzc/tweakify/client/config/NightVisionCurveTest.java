package com.gtzc.tweakify.client.config;

import com.gtzc.tweakify.config.TweakifyModConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 夜视淡出曲线：代码往返、翻译键前缀、未知代码回退平滑曲线。
 */
class NightVisionCurveTest {

	@Test
	void codesRoundTripAndUnknownFallsBackToSmooth() {
		for (NightVisionCurve curve : NightVisionCurve.values()) {
			assertEquals(curve, NightVisionCurve.fromCode(curve.code()));
			assertEquals(curve, curve.fromString(curve.code()));
			assertTrue(curve.translationKey()
					.startsWith("tweakify-mod.config.general.option.nightVisionFadeOutCurve."),
				"unexpected translation key: " + curve.translationKey());
		}
		assertEquals(TweakifyModConfig.NIGHT_VISION_CURVE_SMOOTH, NightVisionCurve.SMOOTH.code());
		assertEquals(TweakifyModConfig.NIGHT_VISION_CURVE_LINEAR, NightVisionCurve.LINEAR.code());
		assertEquals(NightVisionCurve.SMOOTH, NightVisionCurve.fromCode("not-a-curve"));
		assertEquals(NightVisionCurve.SMOOTH, NightVisionCurve.fromCode(null));
	}
}
