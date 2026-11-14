package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 定位栏优化的纯逻辑：名字时机、玩家过滤、名单解析、描边颜色与指南针几何。
 * 这些判定与渲染分离，避免只靠游戏内目测验证。
 */
class LocatorBarHeadsTest {
	@Test
	void nameModesFollowTheirTriggers() {
		assertFalse(LocatorHeadNames.shouldShow(TweakifyModConfig.LOCATOR_HEAD_NAMES_NEVER, true, true),
			"NEVER 任何情况下都不显示");
		assertTrue(LocatorHeadNames.shouldShow(TweakifyModConfig.LOCATOR_HEAD_NAMES_ALWAYS, false, false),
			"ALWAYS 不看其它条件");
		assertTrue(LocatorHeadNames.shouldShow(TweakifyModConfig.LOCATOR_HEAD_NAMES_LOOKING_AT, true, false));
		assertFalse(LocatorHeadNames.shouldShow(TweakifyModConfig.LOCATOR_HEAD_NAMES_LOOKING_AT, false, true),
			"LOOKING_AT 只看是否注视");
		assertTrue(LocatorHeadNames.shouldShow(TweakifyModConfig.LOCATOR_HEAD_NAMES_PLAYER_LIST, false, true));
		assertFalse(LocatorHeadNames.shouldShow(TweakifyModConfig.LOCATOR_HEAD_NAMES_PLAYER_LIST, true, false),
			"PLAYER_LIST 只看玩家列表");
		assertFalse(LocatorHeadNames.shouldShow("unknown", true, true), "未知取值回退成不显示");
	}

	@Test
	void filtersFollowTheModeAndKeepEmptyListSemantics() {
		assertTrue(LocatorHeadFilter.allows(TweakifyModConfig.LOCATOR_HEAD_FILTER_ALL, false, false));
		assertTrue(LocatorHeadFilter.allows(TweakifyModConfig.LOCATOR_HEAD_FILTER_INCLUDE, true, false));
		assertFalse(LocatorHeadFilter.allows(TweakifyModConfig.LOCATOR_HEAD_FILTER_INCLUDE, false, false),
			"仅包含 + 空名单 = 全部隐藏（与参考实现一致）");
		assertFalse(LocatorHeadFilter.allows(TweakifyModConfig.LOCATOR_HEAD_FILTER_EXCLUDE, false, true));
		assertTrue(LocatorHeadFilter.allows(TweakifyModConfig.LOCATOR_HEAD_FILTER_EXCLUDE, false, false),
			"排除 + 空名单 = 全部显示");
		// 两份名单互不从属：INCLUDE 只看包含名单，EXCLUDE 只看排除名单。
		assertTrue(LocatorHeadFilter.allows(TweakifyModConfig.LOCATOR_HEAD_FILTER_INCLUDE, true, true));
		assertFalse(LocatorHeadFilter.allows(TweakifyModConfig.LOCATOR_HEAD_FILTER_EXCLUDE, true, true));
		assertTrue(LocatorHeadFilter.allows("unknown", false, true), "未知取值回退成全部显示");
	}

	// 描边粗细必须与参考实现的四档表一致（0.3 / 0.5 / 1.0 / 2.0）。
	@Test
	void borderThicknessFollowsReferenceTable() {
		assertEquals(0.3, TweakifyModConfig.locatorHeadBorderThicknessPixels(
			TweakifyModConfig.LOCATOR_HEAD_BORDER_THIN));
		assertEquals(0.5, TweakifyModConfig.locatorHeadBorderThicknessPixels(
			TweakifyModConfig.LOCATOR_HEAD_BORDER_MEDIUM));
		assertEquals(1.0, TweakifyModConfig.locatorHeadBorderThicknessPixels(
			TweakifyModConfig.LOCATOR_HEAD_BORDER_NORMAL));
		assertEquals(2.0, TweakifyModConfig.locatorHeadBorderThicknessPixels(
			TweakifyModConfig.LOCATOR_HEAD_BORDER_THICK));
		assertEquals(1.0, TweakifyModConfig.locatorHeadBorderThicknessPixels("unknown"), "未知取值回退 1.0");
		assertEquals(TweakifyModConfig.LOCATOR_HEAD_BORDER_NORMAL,
			TweakifyModConfig.DEFAULT_LOCATOR_HEAD_BORDER_THICKNESS, "默认档位为标准 1.0");
	}

	// Flashback 不在测试类路径上：检测必须安全返回 false。
	@Test
	void flashbackReplayCheckIsSafeWithoutTheMod() {
		assertFalse(LocatorCompass.isInFlashbackReplay(), "未安装 Flashback 时应返回 false");
	}

	@Test
	void filterNamesSplitOnAllReferenceSeparators() {
		List<String> names = LocatorBarHeads.splitNames(List.of(
			"Jeb_, Dinnerbone; LadyAgnes", " notch.hero:alex"));

		assertEquals(List.of("jeb_", "dinnerbone", "ladyagnes", "notch", "hero", "alex"), names);
		assertTrue(LocatorBarHeads.containsName(names, "Dinnerbone"), "比较时忽略大小写");
		assertFalse(LocatorBarHeads.containsName(names, "Steve"));
		assertFalse(LocatorBarHeads.containsName(List.of(), "Steve"));
		assertFalse(LocatorBarHeads.containsName(names, null));
		assertTrue(LocatorBarHeads.splitNames(List.of()).isEmpty());
		assertTrue(LocatorBarHeads.splitNames(null).isEmpty());
	}

	@Test
	void borderColourResolvesByStyle() {
		TweakifyModConfig config = new TweakifyModConfig();
		config.functions.locatorBarHeadBorder = TweakifyModConfig.LOCATOR_HEAD_BORDER_OFF;
		assertEquals(-1, LocatorBarHeads.resolveBorderRgb(config, 0x123456), "关闭时不画描边");

		config.functions.locatorBarHeadBorder = TweakifyModConfig.LOCATOR_HEAD_BORDER_TEAM;
		assertEquals(0x123456, LocatorBarHeads.resolveBorderRgb(config, 0x123456));
		assertEquals(0xFFFFFF, LocatorBarHeads.resolveBorderRgb(config, null), "没有队伍时回退白色");

		config.functions.locatorBarHeadBorder = TweakifyModConfig.LOCATOR_HEAD_BORDER_STATIC;
		config.functions.locatorBarHeadBorderColor = TweakifyModConfig.LOCATOR_COLOR_RED;
		assertEquals(LocatorHeadColor.RED.rgb(), LocatorBarHeads.resolveBorderRgb(config, 0x123456),
			"固定颜色样式忽略队伍颜色");
	}

	@Test
	void borderStyleHelpersMatchTheirCodes() {
		assertFalse(LocatorHeadBorder.isVisible(TweakifyModConfig.LOCATOR_HEAD_BORDER_OFF));
		assertTrue(LocatorHeadBorder.isVisible(TweakifyModConfig.LOCATOR_HEAD_BORDER_STATIC));
		assertTrue(LocatorHeadBorder.followsTeam(TweakifyModConfig.LOCATOR_HEAD_BORDER_TEAM));
		assertFalse(LocatorHeadBorder.followsTeam(TweakifyModConfig.LOCATOR_HEAD_BORDER_STATIC));
		assertTrue(LocatorHeadBorder.usesStaticColor(TweakifyModConfig.LOCATOR_HEAD_BORDER_STATIC));
		assertEquals(LocatorHeadBorder.STATIC, LocatorHeadBorder.fromCode("unknown"));
	}

	@Test
	void paletteColorsExposeStableArgb() {
		assertEquals(0xFFFFFFFF, LocatorHeadColor.WHITE.argb());
		assertEquals(0xFFFF5555, LocatorHeadColor.RED.argb());
		assertEquals(0xFF5555FF, LocatorHeadColor.BLUE.argb());
		assertEquals(0xFFFFFF, LocatorHeadColor.WHITE.rgb(), "rgb() 去掉透明度");
		assertEquals(LocatorHeadColor.WHITE, LocatorHeadColor.fromCode("unknown"), "未知颜色回退白色");
		assertEquals(LocatorHeadColor.GREEN.rgb(), LocatorHeadColor.rgbOf(TweakifyModConfig.LOCATOR_COLOR_GREEN));
	}

	@Test
	void compassGeometryMatchesReferenceMath() {
		assertEquals(0.0, LocatorCompass.angleDifference(0.0, 0.0));
		assertEquals(-10.0, LocatorCompass.angleDifference(10.0, 0.0));
		assertEquals(10.0, LocatorCompass.angleDifference(350.0, 0.0), "跨 0 度时归一化");
		assertEquals(-180.0, LocatorCompass.angleDifference(180.0, 0.0));
		assertEquals(90.0, LocatorCompass.angleDifference(270.0, 0.0));

		assertEquals(100, LocatorCompass.labelX(100, 0.0));
		assertEquals(191, LocatorCompass.labelX(100, LocatorCompass.HALF_FOV_DEGREES));
		assertEquals(9, LocatorCompass.labelX(100, -LocatorCompass.HALF_FOV_DEGREES));
		assertNull(LocatorCompass.labelX(100, LocatorCompass.HALF_FOV_DEGREES + 1.0), "超出可视范围不画");

		assertEquals(255, LocatorCompass.labelAlpha(0.0));
		assertEquals(127, LocatorCompass.labelAlpha(LocatorCompass.HALF_FOV_DEGREES), "边缘淡到一半");
		assertFalse(LocatorCompass.hidesCompass(null), "没有界面时正常显示");

		// 纵向与参考实现一致：写死 screenHeight - 31；26.3 的条带顶端是 高度 - 24 - 5，
		// 因此正好与头像/圆点同一行。
		assertEquals(31, LocatorCompass.COMPASS_Y_FROM_BOTTOM);
		int guiHeight = 240;
		int barTop = guiHeight - 24 - 5;
		assertEquals(barTop - 2, guiHeight - LocatorCompass.COMPASS_Y_FROM_BOTTOM, "与头像/圆点同一行");
	}

	// 头像倍率表必须与参考实现一致（5 = 1.0x）。
	@Test
	void headSizeMultiplierMatchesReferenceTable() {
		assertEquals(0.5, LocatorBarHeads.headSizeMultiplier(1));
		assertEquals(0.6, LocatorBarHeads.headSizeMultiplier(2));
		assertEquals(0.7, LocatorBarHeads.headSizeMultiplier(3));
		assertEquals(0.8, LocatorBarHeads.headSizeMultiplier(4));
		assertEquals(1.0, LocatorBarHeads.headSizeMultiplier(5));
		assertEquals(1.1, LocatorBarHeads.headSizeMultiplier(6));
		assertEquals(1.2, LocatorBarHeads.headSizeMultiplier(7));
		assertEquals(1.3, LocatorBarHeads.headSizeMultiplier(8));
		assertEquals(1.5, LocatorBarHeads.headSizeMultiplier(9));
		assertEquals(1.0, LocatorBarHeads.headSizeMultiplier(0), "越界回退 1.0x");
	}

	// 回归：指南针开启后经验等级数字整体上移，与字母像素带不相交。
	@Test
	void experienceLevelTextClearsTheCompassRow() {
		int guiHeight = 240;
		int lineHeight = 9;
		int letterInkTop = guiHeight - LocatorCompass.COMPASS_Y_FROM_BOTTOM + 1;
		int levelPaintedBottom = LocatorCompass.levelTextTop(guiHeight, lineHeight) + lineHeight + 1;
		assertEquals(letterInkTop - 1, levelPaintedBottom, "描边底边停在字母之上 1 像素");
		assertTrue(levelPaintedBottom < letterInkTop, "两者像素带不相交");

		// 反证：原版位置会压住字母，说明上移是必需的。
		int vanillaBottom = (guiHeight - 24 - lineHeight - 2) + lineHeight + 1;
		assertTrue(vanillaBottom > letterInkTop, "原版位置会与字母相交");
	}

}
