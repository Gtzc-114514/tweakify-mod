package com.gtzc.tweakify.client.config;

import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;

/**
 * 定位栏可用的命名颜色：描边（固定色）与指南针共用。
 *
 * <p>MaLiLib 没有取色器，这里沿用项目里其它“调色板”选项的做法：给一组固定颜色，
 * 需要精确取色时仍然可以用队伍颜色描边。
 */
public enum LocatorHeadColor implements CodeOptionEntry {
	WHITE(TweakifyModConfig.LOCATOR_COLOR_WHITE, 0xFFFFFFFF),
	BLACK(TweakifyModConfig.LOCATOR_COLOR_BLACK, 0xFF000000),
	GRAY(TweakifyModConfig.LOCATOR_COLOR_GRAY, 0xFF9E9E9E),
	RED(TweakifyModConfig.LOCATOR_COLOR_RED, 0xFFFF5555),
	ORANGE(TweakifyModConfig.LOCATOR_COLOR_ORANGE, 0xFFFFAA00),
	YELLOW(TweakifyModConfig.LOCATOR_COLOR_YELLOW, 0xFFFFFF55),
	GREEN(TweakifyModConfig.LOCATOR_COLOR_GREEN, 0xFF55FF55),
	BLUE(TweakifyModConfig.LOCATOR_COLOR_BLUE, 0xFF5555FF),
	AQUA(TweakifyModConfig.LOCATOR_COLOR_AQUA, 0xFF55FFFF);

	private final String code;
	private final int argb;

	LocatorHeadColor(String code, int argb) {
		this.code = code;
		this.argb = argb;
	}

	@Override
	public String code() {
		return this.code;
	}

	@Override
	public String translationKey() {
		return "tweakify-mod.config.general.option.locatorHeadColor." + this.code;
	}

	public int argb() {
		return this.argb;
	}

	// 只要 RGB，透明度由调用方按淡出/描边需求决定。
	public int rgb() {
		return argb() & 0xFFFFFF;
	}

	public static LocatorHeadColor fromCode(String code) {
		for (LocatorHeadColor color : values()) {
			if (color.code.equals(code)) {
				return color;
			}
		}
		return WHITE;
	}

	// 把配置里的 code 换成 RGB；未知值回退白色。
	public static int rgbOf(String code) {
		return fromCode(code).rgb();
	}

	@Override
	public IConfigOptionListEntry fromString(String value) {
		return fromCode(value);
	}
}
