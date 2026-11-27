package com.gtzc.tweakify.client.config;

import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;

/**
 * 存档游玩时长的显示格式：小数小时 / 时:分 / 小时与分钟。
 * 显示名走语言文件，切换语言后无需重启。
 */
public enum SavePlayTimeFormat implements CodeOptionEntry {
	HOURS(TweakifyModConfig.SAVE_PLAY_TIME_FORMAT_HOURS,
		"tweakify-mod.config.general.option.savePlayTimeFormat.hours"),
	CLOCK(TweakifyModConfig.SAVE_PLAY_TIME_FORMAT_CLOCK,
		"tweakify-mod.config.general.option.savePlayTimeFormat.clock"),
	DURATION(TweakifyModConfig.SAVE_PLAY_TIME_FORMAT_DURATION,
		"tweakify-mod.config.general.option.savePlayTimeFormat.duration");

	private final String code;
	private final String translationKey;

	SavePlayTimeFormat(String code, String translationKey) {
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

	public static SavePlayTimeFormat fromCode(String code) {
		for (SavePlayTimeFormat format : values()) {
			if (format.code.equals(code)) {
				return format;
			}
		}
		return HOURS;
	}

	@Override
	public IConfigOptionListEntry fromString(String value) {
		return fromCode(value);
	}
}
