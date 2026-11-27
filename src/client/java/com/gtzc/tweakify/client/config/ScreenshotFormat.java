package com.gtzc.tweakify.client.config;

import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;

/**
 * 截图输出格式：PNG 无损，JPG 体积小，TGA/BMP 供后续处理工具使用。
 */
public enum ScreenshotFormat implements CodeOptionEntry {
	PNG(TweakifyModConfig.SCREENSHOT_FORMAT_PNG,
		"tweakify-mod.config.general.option.betterScreenshotFormat.png", "png"),
	JPG(TweakifyModConfig.SCREENSHOT_FORMAT_JPG,
		"tweakify-mod.config.general.option.betterScreenshotFormat.jpg", "jpg"),
	TGA(TweakifyModConfig.SCREENSHOT_FORMAT_TGA,
		"tweakify-mod.config.general.option.betterScreenshotFormat.tga", "tga"),
	BMP(TweakifyModConfig.SCREENSHOT_FORMAT_BMP,
		"tweakify-mod.config.general.option.betterScreenshotFormat.bmp", "bmp");

	private final String code;
	private final String translationKey;
	private final String extension;

	ScreenshotFormat(String code, String translationKey, String extension) {
		this.code = code;
		this.translationKey = translationKey;
		this.extension = extension;
	}

	@Override
	public String code() {
		return this.code;
	}

	@Override
	public String translationKey() {
		return this.translationKey;
	}

	public String extension() {
		return this.extension;
	}

	public static ScreenshotFormat fromCode(String code) {
		for (ScreenshotFormat format : values()) {
			if (format.code.equals(code)) {
				return format;
			}
		}
		return PNG;
	}

	@Override
	public IConfigOptionListEntry fromString(String value) {
		return fromCode(value);
	}
}
