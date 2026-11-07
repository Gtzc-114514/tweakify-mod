package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;

/**
 * 自定义方块轮廓的彩色模式：
 * palette（固定调色板颜色）/ rainbow（随时间循环色相）/ pulse（周期性改变亮度）。
 */
public enum BlockOutlineColorMode implements CodeOptionEntry {
	PALETTE(TweakifyModConfig.BLOCK_OUTLINE_MODE_PALETTE,
		"tweakify-mod.config.general.option.blockOutlineColorMode.palette"),
	RAINBOW(TweakifyModConfig.BLOCK_OUTLINE_MODE_RAINBOW,
		"tweakify-mod.config.general.option.blockOutlineColorMode.rainbow"),
	PULSE(TweakifyModConfig.BLOCK_OUTLINE_MODE_PULSE,
		"tweakify-mod.config.general.option.blockOutlineColorMode.pulse");

	private final String code;
	private final String translationKey;

	BlockOutlineColorMode(String code, String translationKey) {
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

	public static BlockOutlineColorMode fromCode(String code) {
		for (BlockOutlineColorMode mode : values()) {
			if (mode.code.equals(code)) {
				return mode;
			}
		}
		return PALETTE;
	}

	@Override
	public IConfigOptionListEntry fromString(String value) {
		return fromCode(value);
	}
}
