package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;

/**
 * 定位栏头像描边样式（参考 locator-heads 的 Border Style）：关闭 / 固定颜色 / 跟随队伍颜色。
 */
public enum LocatorHeadBorder implements CodeOptionEntry {
	OFF(TweakifyModConfig.LOCATOR_HEAD_BORDER_OFF,
		"tweakify-mod.config.general.option.locatorBarHeadBorder.off"),
	STATIC(TweakifyModConfig.LOCATOR_HEAD_BORDER_STATIC,
		"tweakify-mod.config.general.option.locatorBarHeadBorder.static"),
	TEAM(TweakifyModConfig.LOCATOR_HEAD_BORDER_TEAM,
		"tweakify-mod.config.general.option.locatorBarHeadBorder.team");

	private final String code;
	private final String translationKey;

	LocatorHeadBorder(String code, String translationKey) {
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

	public static LocatorHeadBorder fromCode(String code) {
		for (LocatorHeadBorder border : values()) {
			if (border.code.equals(code)) {
				return border;
			}
		}
		return STATIC;
	}

	// 描边是否参与绘制；TEAM 在玩家没有队伍时回退成白色描边。
	public static boolean isVisible(String code) {
		return !TweakifyModConfig.LOCATOR_HEAD_BORDER_OFF.equals(code);
	}

	public static boolean followsTeam(String code) {
		return TweakifyModConfig.LOCATOR_HEAD_BORDER_TEAM.equals(code);
	}

	public static boolean usesStaticColor(String code) {
		return TweakifyModConfig.LOCATOR_HEAD_BORDER_STATIC.equals(code);
	}

	@Override
	public IConfigOptionListEntry fromString(String value) {
		return fromCode(value);
	}
}
