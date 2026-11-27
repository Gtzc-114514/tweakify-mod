package com.gtzc.tweakify.client.config;

import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;

/**
 * 如履平地的跨步高度：1 格 / 1.5 格 / 2 格。
 * 显示名走语言文件，切换语言后无需重启。
 */
public enum AutoStepUpHeight implements CodeOptionEntry {
	ONE_BLOCK(TweakifyModConfig.AUTO_STEP_UP_HEIGHT_ONE_BLOCK,
		"tweakify-mod.config.general.option.autoStepUpHeight.one_block"),
	ONE_AND_HALF(TweakifyModConfig.AUTO_STEP_UP_HEIGHT_ONE_AND_HALF,
		"tweakify-mod.config.general.option.autoStepUpHeight.one_and_half"),
	TWO_BLOCKS(TweakifyModConfig.AUTO_STEP_UP_HEIGHT_TWO_BLOCKS,
		"tweakify-mod.config.general.option.autoStepUpHeight.two_blocks");

	private final String code;
	private final String translationKey;

	AutoStepUpHeight(String code, String translationKey) {
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

	public static AutoStepUpHeight fromCode(String code) {
		for (AutoStepUpHeight height : values()) {
			if (height.code.equals(code)) {
				return height;
			}
		}
		return ONE_BLOCK;
	}

	@Override
	public IConfigOptionListEntry fromString(String value) {
		return fromCode(value);
	}
}
