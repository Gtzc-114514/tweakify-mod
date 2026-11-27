package com.gtzc.tweakify.client.config;

import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;

/**
 * 实体高亮的描边样式：常亮 / 按周期闪烁。
 * 显示名走语言文件，切换语言后无需重启。
 */
public enum EntityHighlightStyle implements CodeOptionEntry {
	OUTLINE(TweakifyModConfig.ENTITY_HIGHLIGHT_STYLE_OUTLINE,
		"tweakify-mod.config.highlight.option.entityHighlightStyle.outline"),
	BLINK(TweakifyModConfig.ENTITY_HIGHLIGHT_STYLE_BLINK,
		"tweakify-mod.config.highlight.option.entityHighlightStyle.blink");

	private final String code;
	private final String translationKey;

	EntityHighlightStyle(String code, String translationKey) {
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

	public static EntityHighlightStyle fromCode(String code) {
		for (EntityHighlightStyle style : values()) {
			if (style.code.equals(code)) {
				return style;
			}
		}
		return OUTLINE;
	}

	@Override
	public IConfigOptionListEntry fromString(String value) {
		return fromCode(value);
	}
}
