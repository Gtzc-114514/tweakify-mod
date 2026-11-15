package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;

/**
 * 攻击范围颜色调色板：只提供 RGB，透明度由线框/填充模式各自的固定档位决定。
 * 显示名走语言文件，切换语言后无需重启。
 */
public enum AttackRangePalette implements CodeOptionEntry {
	BLUE(TweakifyModConfig.ATTACK_RANGE_PALETTE_BLUE,
		"tweakify-mod.config.general.option.attackRangePalette.blue", 0x3388FF),
	RED(TweakifyModConfig.ATTACK_RANGE_PALETTE_RED,
		"tweakify-mod.config.general.option.attackRangePalette.red", 0xFF5555),
	GREEN(TweakifyModConfig.ATTACK_RANGE_PALETTE_GREEN,
		"tweakify-mod.config.general.option.attackRangePalette.green", 0x55FF55),
	YELLOW(TweakifyModConfig.ATTACK_RANGE_PALETTE_YELLOW,
		"tweakify-mod.config.general.option.attackRangePalette.yellow", 0xFFFF55),
	CYAN(TweakifyModConfig.ATTACK_RANGE_PALETTE_CYAN,
		"tweakify-mod.config.general.option.attackRangePalette.cyan", 0x55FFFF),
	MAGENTA(TweakifyModConfig.ATTACK_RANGE_PALETTE_MAGENTA,
		"tweakify-mod.config.general.option.attackRangePalette.magenta", 0xFF55FF),
	WHITE(TweakifyModConfig.ATTACK_RANGE_PALETTE_WHITE,
		"tweakify-mod.config.general.option.attackRangePalette.white", 0xFFFFFF);

	private final String code;
	private final String translationKey;
	private final int rgb;

	AttackRangePalette(String code, String translationKey, int rgb) {
		this.code = code;
		this.translationKey = translationKey;
		this.rgb = rgb;
	}

	@Override
	public String code() {
		return this.code;
	}

	@Override
	public String translationKey() {
		return this.translationKey;
	}

	// 只要 RGB；透明度由几何缓存按线框/填充档位拼上。
	public int rgb() {
		return this.rgb;
	}

	public static AttackRangePalette fromCode(String code) {
		for (AttackRangePalette palette : values()) {
			if (palette.code.equals(code)) {
				return palette;
			}
		}
		return BLUE;
	}

	// 把配置里的 code 换成 RGB；未知值回退蓝色。
	public static int rgbOf(String code) {
		return fromCode(code).rgb();
	}

	@Override
	public IConfigOptionListEntry fromString(String value) {
		return fromCode(value);
	}
}
