package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;

/**
 * 自定义方块轮廓的调色板：单色模式下使用的颜色。
 * 显示名走语言文件，切换语言后无需重启。
 */
public enum BlockOutlinePalette implements CodeOptionEntry {
	WHITE(TweakifyModConfig.BLOCK_OUTLINE_PALETTE_WHITE,
		"tweakify-mod.config.general.option.blockOutlinePalette.white", 0xFFFFFFFF),
	RED(TweakifyModConfig.BLOCK_OUTLINE_PALETTE_RED,
		"tweakify-mod.config.general.option.blockOutlinePalette.red", 0xFFFF5555),
	GREEN(TweakifyModConfig.BLOCK_OUTLINE_PALETTE_GREEN,
		"tweakify-mod.config.general.option.blockOutlinePalette.green", 0xFF55FF55),
	BLUE(TweakifyModConfig.BLOCK_OUTLINE_PALETTE_BLUE,
		"tweakify-mod.config.general.option.blockOutlinePalette.blue", 0xFF5555FF),
	YELLOW(TweakifyModConfig.BLOCK_OUTLINE_PALETTE_YELLOW,
		"tweakify-mod.config.general.option.blockOutlinePalette.yellow", 0xFFFFFF55),
	CYAN(TweakifyModConfig.BLOCK_OUTLINE_PALETTE_CYAN,
		"tweakify-mod.config.general.option.blockOutlinePalette.cyan", 0xFF55FFFF),
	MAGENTA(TweakifyModConfig.BLOCK_OUTLINE_PALETTE_MAGENTA,
		"tweakify-mod.config.general.option.blockOutlinePalette.magenta", 0xFFFF55FF),
	ORANGE(TweakifyModConfig.BLOCK_OUTLINE_PALETTE_ORANGE,
		"tweakify-mod.config.general.option.blockOutlinePalette.orange", 0xFFFFAA00);

	private final String code;
	private final String translationKey;
	private final int argb;

	BlockOutlinePalette(String code, String translationKey, int argb) {
		this.code = code;
		this.translationKey = translationKey;
		this.argb = argb;
	}

	@Override
	public String code() {
		return this.code;
	}

	@Override
	public String translationKey() {
		return this.translationKey;
	}

	public int argb() {
		return this.argb;
	}

	public static BlockOutlinePalette fromCode(String code) {
		for (BlockOutlinePalette palette : values()) {
			if (palette.code.equals(code)) {
				return palette;
			}
		}
		return WHITE;
	}

	@Override
	public IConfigOptionListEntry fromString(String value) {
		return fromCode(value);
	}
}
