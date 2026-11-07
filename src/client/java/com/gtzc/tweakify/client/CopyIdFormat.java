package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;

/**
 * 复制 ID 的输出格式：纯 ID / 带类型前缀 / 带数量。
 */
public enum CopyIdFormat implements CodeOptionEntry {
	PLAIN(TweakifyModConfig.COPY_ID_FORMAT_PLAIN,
		"tweakify-mod.config.general.option.copyItemIdFormat.plain"),
	TYPED(TweakifyModConfig.COPY_ID_FORMAT_TYPED,
		"tweakify-mod.config.general.option.copyItemIdFormat.typed"),
	COUNTED(TweakifyModConfig.COPY_ID_FORMAT_COUNTED,
		"tweakify-mod.config.general.option.copyItemIdFormat.counted");

	private final String code;
	private final String translationKey;

	CopyIdFormat(String code, String translationKey) {
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

	public static CopyIdFormat fromCode(String code) {
		for (CopyIdFormat format : values()) {
			if (format.code.equals(code)) {
				return format;
			}
		}
		return PLAIN;
	}

	@Override
	public IConfigOptionListEntry fromString(String value) {
		return fromCode(value);
	}
}
