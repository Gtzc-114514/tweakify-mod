package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;

/**
 * 攻击范围的显示方式：脚下平面圆环 / 经纬线框球体。
 * 显示名走语言文件，切换语言后无需重启。
 */
public enum AttackRangeDisplayMode implements CodeOptionEntry {
	FLAT(TweakifyModConfig.ATTACK_RANGE_DISPLAY_FLAT,
		"tweakify-mod.config.general.option.attackRangeDisplayMode.flat"),
	SPHERE(TweakifyModConfig.ATTACK_RANGE_DISPLAY_SPHERE,
		"tweakify-mod.config.general.option.attackRangeDisplayMode.sphere");

	private final String code;
	private final String translationKey;

	AttackRangeDisplayMode(String code, String translationKey) {
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

	public static AttackRangeDisplayMode fromCode(String code) {
		for (AttackRangeDisplayMode mode : values()) {
			if (mode.code.equals(code)) {
				return mode;
			}
		}
		return FLAT;
	}

	@Override
	public IConfigOptionListEntry fromString(String value) {
		return fromCode(value);
	}
}
