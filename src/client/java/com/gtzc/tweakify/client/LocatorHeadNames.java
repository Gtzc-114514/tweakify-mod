package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;

/**
 * 定位栏玩家名字的显示时机（参考 locator-heads 的 Show Player Names）：
 * 不显示 / 总是 / 仅注视时 / 仅玩家列表（Tab）打开时。
 */
public enum LocatorHeadNames implements CodeOptionEntry {
	NEVER(TweakifyModConfig.LOCATOR_HEAD_NAMES_NEVER,
		"tweakify-mod.config.general.option.locatorBarHeadNames.never"),
	ALWAYS(TweakifyModConfig.LOCATOR_HEAD_NAMES_ALWAYS,
		"tweakify-mod.config.general.option.locatorBarHeadNames.always"),
	LOOKING_AT(TweakifyModConfig.LOCATOR_HEAD_NAMES_LOOKING_AT,
		"tweakify-mod.config.general.option.locatorBarHeadNames.looking_at"),
	PLAYER_LIST(TweakifyModConfig.LOCATOR_HEAD_NAMES_PLAYER_LIST,
		"tweakify-mod.config.general.option.locatorBarHeadNames.player_list");

	private final String code;
	private final String translationKey;

	LocatorHeadNames(String code, String translationKey) {
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

	public static LocatorHeadNames fromCode(String code) {
		for (LocatorHeadNames mode : values()) {
			if (mode.code.equals(code)) {
				return mode;
			}
		}
		return NEVER;
	}

	/**
	 * 当前是否应该画出名字。判定与渲染分离，便于单测直接覆盖四种时机。
	 *
	 * @param mode           配置里的时机 code
	 * @param lookingAt      镜头是否正对着该玩家（LOOKING_AT 用）
	 * @param playerListDown 玩家列表（Tab）是否按住（PLAYER_LIST 用）
	 */
	public static boolean shouldShow(String mode, boolean lookingAt, boolean playerListDown) {
		if (TweakifyModConfig.LOCATOR_HEAD_NAMES_ALWAYS.equals(mode)) {
			return true;
		}
		if (TweakifyModConfig.LOCATOR_HEAD_NAMES_LOOKING_AT.equals(mode)) {
			return lookingAt;
		}
		if (TweakifyModConfig.LOCATOR_HEAD_NAMES_PLAYER_LIST.equals(mode)) {
			return playerListDown;
		}
		return false;
	}

	@Override
	public IConfigOptionListEntry fromString(String value) {
		return fromCode(value);
	}
}
