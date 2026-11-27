package com.gtzc.tweakify.client.config;

import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;

/**
 * 中键获得液体桶的范围：水与岩浆 / 仅水 / 仅岩浆。
 * 显示名走语言文件，切换语言后无需重启。
 */
public enum FluidPickScope implements CodeOptionEntry {
	WATER_AND_LAVA(TweakifyModConfig.FLUID_PICK_WATER_AND_LAVA,
		"tweakify-mod.config.general.option.pickFluidBucketFluids.water_and_lava"),
	WATER_ONLY(TweakifyModConfig.FLUID_PICK_WATER_ONLY,
		"tweakify-mod.config.general.option.pickFluidBucketFluids.water_only"),
	LAVA_ONLY(TweakifyModConfig.FLUID_PICK_LAVA_ONLY,
		"tweakify-mod.config.general.option.pickFluidBucketFluids.lava_only");

	private final String code;
	private final String translationKey;

	FluidPickScope(String code, String translationKey) {
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

	public static FluidPickScope fromCode(String code) {
		for (FluidPickScope scope : values()) {
			if (scope.code.equals(code)) {
				return scope;
			}
		}
		return WATER_AND_LAVA;
	}

	@Override
	public IConfigOptionListEntry fromString(String value) {
		return fromCode(value);
	}
}
