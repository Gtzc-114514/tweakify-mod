package com.gtzc.tweakify.client.config;

import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;

/**
 * 定位栏头像描边粗细，与参考实现 {@code Haage001/locator-heads} 的 {@code BorderThickness} 一致：
 * 细 0.3 / 中 0.5 / 标准 1.0 / 粗 2.0 像素。
 */
public enum LocatorHeadBorderThickness implements CodeOptionEntry {
	THIN(TweakifyModConfig.LOCATOR_HEAD_BORDER_THIN,
		"tweakify-mod.config.general.option.locatorBarHeadBorderThickness.thin"),
	MEDIUM(TweakifyModConfig.LOCATOR_HEAD_BORDER_MEDIUM,
		"tweakify-mod.config.general.option.locatorBarHeadBorderThickness.medium"),
	NORMAL(TweakifyModConfig.LOCATOR_HEAD_BORDER_NORMAL,
		"tweakify-mod.config.general.option.locatorBarHeadBorderThickness.normal"),
	THICK(TweakifyModConfig.LOCATOR_HEAD_BORDER_THICK,
		"tweakify-mod.config.general.option.locatorBarHeadBorderThickness.thick");

	private final String code;
	private final String translationKey;

	LocatorHeadBorderThickness(String code, String translationKey) {
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

	public static LocatorHeadBorderThickness fromCode(String code) {
		for (LocatorHeadBorderThickness thickness : values()) {
			if (thickness.code.equals(code)) {
				return thickness;
			}
		}
		return NORMAL;
	}

	@Override
	public IConfigOptionListEntry fromString(String value) {
		return fromCode(value);
	}
}
