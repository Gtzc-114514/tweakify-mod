package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;

/**
 * 夜视淡出曲线：平滑（smoothstep）/ 线性。
 */
public enum NightVisionCurve implements CodeOptionEntry {
	SMOOTH(TweakifyModConfig.NIGHT_VISION_CURVE_SMOOTH,
		"tweakify-mod.config.general.option.nightVisionFadeOutCurve.smooth"),
	LINEAR(TweakifyModConfig.NIGHT_VISION_CURVE_LINEAR,
		"tweakify-mod.config.general.option.nightVisionFadeOutCurve.linear");

	private final String code;
	private final String translationKey;

	NightVisionCurve(String code, String translationKey) {
		this.code = code;
		this.translationKey = translationKey;
	}

	@Override
	public String code() {
		return this.code;
	}

	@Override
	public String translationKey() {
		return this.translationKey;
	}

	public static NightVisionCurve fromCode(String code) {
		for (NightVisionCurve curve : values()) {
			if (curve.code.equals(code)) {
				return curve;
			}
		}
		return SMOOTH;
	}

	@Override
	public IConfigOptionListEntry fromString(String value) {
		return fromCode(value);
	}
}
