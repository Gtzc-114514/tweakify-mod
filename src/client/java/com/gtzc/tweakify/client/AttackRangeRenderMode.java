package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;

/**
 * 攻击范围的渲染方式：蓝色线框 / 半透明实心。
 * 线框无 OIT，性能更好；实心会有透明通道的固定开销。
 */
public enum AttackRangeRenderMode implements CodeOptionEntry {
	WIREFRAME(TweakifyModConfig.ATTACK_RANGE_RENDER_WIREFRAME,
		"tweakify-mod.config.general.option.attackRangeRenderMode.wireframe"),
	FILLED(TweakifyModConfig.ATTACK_RANGE_RENDER_FILLED,
		"tweakify-mod.config.general.option.attackRangeRenderMode.filled");

	private final String code;
	private final String translationKey;

	AttackRangeRenderMode(String code, String translationKey) {
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

	public static AttackRangeRenderMode fromCode(String code) {
		for (AttackRangeRenderMode mode : values()) {
			if (mode.code.equals(code)) {
				return mode;
			}
		}
		return WIREFRAME;
	}

	@Override
	public IConfigOptionListEntry fromString(String value) {
		return fromCode(value);
	}
}
