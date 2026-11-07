package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;

/**
 * 剑穿草打生物：允许穿过的植物范围。
 */
public enum CutThroughPlantMode implements CodeOptionEntry {
	FLOWERS(TweakifyModConfig.CUT_THROUGH_PLANTS_FLOWERS,
		"tweakify-mod.config.general.option.cutThroughPlants.flowers"),
	CROPS(TweakifyModConfig.CUT_THROUGH_PLANTS_CROPS,
		"tweakify-mod.config.general.option.cutThroughPlants.crops"),
	ALL(TweakifyModConfig.CUT_THROUGH_PLANTS_ALL,
		"tweakify-mod.config.general.option.cutThroughPlants.all");

	private final String code;
	private final String translationKey;

	CutThroughPlantMode(String code, String translationKey) {
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

	public static CutThroughPlantMode fromCode(String code) {
		for (CutThroughPlantMode mode : values()) {
			if (mode.code.equals(code)) {
				return mode;
			}
		}
		return ALL;
	}

	@Override
	public IConfigOptionListEntry fromString(String value) {
		return fromCode(value);
	}
}
