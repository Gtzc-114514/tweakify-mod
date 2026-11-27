package com.gtzc.tweakify.client.config;

import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;

/**
 * 砂轮移除诅咒的范围：全部诅咒 / 仅绑定诅咒 / 仅消失诅咒。
 * 显示名走语言文件，切换语言后无需重启。
 */
public enum CurseRemovalScope implements CodeOptionEntry {
	ALL(TweakifyModConfig.CURSE_SCOPE_ALL,
		"tweakify-mod.config.general.option.curseRemovalScope.all"),
	BINDING_ONLY(TweakifyModConfig.CURSE_SCOPE_BINDING_ONLY,
		"tweakify-mod.config.general.option.curseRemovalScope.binding_only"),
	VANISHING_ONLY(TweakifyModConfig.CURSE_SCOPE_VANISHING_ONLY,
		"tweakify-mod.config.general.option.curseRemovalScope.vanishing_only");

	private final String code;
	private final String translationKey;

	CurseRemovalScope(String code, String translationKey) {
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

	public static CurseRemovalScope fromCode(String code) {
		for (CurseRemovalScope scope : values()) {
			if (scope.code.equals(code)) {
				return scope;
			}
		}
		return ALL;
	}

	// 是否移除指定路径（binding_curse / vanishing_curse）的诅咒；未知路径只在“全部”范围移除。
	public static boolean shouldRemove(String code, String enchantmentPath) {
		return switch (fromCode(code)) {
			case ALL -> true;
			case BINDING_ONLY -> "binding_curse".equals(enchantmentPath);
			case VANISHING_ONLY -> "vanishing_curse".equals(enchantmentPath);
		};
	}

	@Override
	public IConfigOptionListEntry fromString(String value) {
		return fromCode(value);
	}
}
