package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;

/**
 * 聊天框选颜色：每个选项自带对应的半透明 ARGB 值。
 */
public enum ChatSelectionColor implements CodeOptionEntry {
	BLUE(TweakifyModConfig.CHAT_SELECTION_BLUE,
		"tweakify-mod.config.general.option.chatSelectionColor.blue", 0x5533AAFF),
	GREEN(TweakifyModConfig.CHAT_SELECTION_GREEN,
		"tweakify-mod.config.general.option.chatSelectionColor.green", 0x5533CC55),
	ORANGE(TweakifyModConfig.CHAT_SELECTION_ORANGE,
		"tweakify-mod.config.general.option.chatSelectionColor.orange", 0x55FFA033),
	PURPLE(TweakifyModConfig.CHAT_SELECTION_PURPLE,
		"tweakify-mod.config.general.option.chatSelectionColor.purple", 0x559966FF),
	GRAY(TweakifyModConfig.CHAT_SELECTION_GRAY,
		"tweakify-mod.config.general.option.chatSelectionColor.gray", 0x55AAAAAA);

	private final String code;
	private final String translationKey;
	private final int argb;

	ChatSelectionColor(String code, String translationKey, int argb) {
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

	public static ChatSelectionColor fromCode(String code) {
		for (ChatSelectionColor color : values()) {
			if (color.code.equals(code)) {
				return color;
			}
		}
		return BLUE;
	}

	@Override
	public IConfigOptionListEntry fromString(String value) {
		return fromCode(value);
	}
}
