package com.gtzc.tweakify.client.ui;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.TweakifyModTestSupport;
import com.gtzc.tweakify.client.config.ConfigGuiTab;
import com.gtzc.tweakify.client.config.TestingConfigOptions;
import com.gtzc.tweakify.client.config.TweakifyModConfigOptions;
import com.gtzc.tweakify.client.render.FavoriteStarRenderer;
import com.gtzc.tweakify.config.ConfigCatalog;
import com.gtzc.tweakify.config.FavoritesStorage;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.AsyncFileWriter;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 配置树与分类标签的完整性：新增配置项后如果忘记登记到树或“修改”标签，
 * 界面会静默丢失条目或错误归类，游戏里很难发现。
 */
class TweakifyModConfigScreenTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void treeCoversEveryConfigOption() {
		Set<String> treeNames = TweakifyModConfigScreen.treeConfigNames();
		Set<String> allNames = new HashSet<>();
		for (IConfigBase option : TweakifyModConfigOptions.ALL_OPTIONS) {
			assertTrue(allNames.add(option.getName()), "duplicate option: " + option.getName());
			assertTrue(treeNames.contains(option.getName()),
				"config tree is missing option: " + option.getName());
		}
	}

	@Test
	void everyDisableOptionIsClassifiedExplicitly() {
		for (IConfigBase option : TweakifyModConfigOptions.ALL_OPTIONS) {
			if (!option.getName().startsWith("disable")) {
				continue;
			}
			boolean functions = TweakifyModConfigScreen.matchesTab(
				option, TweakifyModConfigScreen.ConfigTab.FUNCTIONS);
			boolean modified = TweakifyModConfigScreen.matchesTab(
				option, TweakifyModConfigScreen.ConfigTab.MODIFIED);
			assertTrue(functions ^ modified,
				"disable option must be in functions or modified, not general: " + option.getName());
		}
	}

	// 滚动列表记忆只属于「通用」标签，不再出现在「功能」标签。
	@Test
	void scrollMemoryIsGeneralOnly() {
		IConfigBase option = TweakifyModConfigOptions.optionOf("scrollListMemory");
		assertTrue(option != null, "scrollListMemory option must exist");
		assertTrue(TweakifyModConfigScreen.matchesTab(option, TweakifyModConfigScreen.ConfigTab.GENERAL),
			"scrollListMemory should appear in the general tab");
		assertFalse(TweakifyModConfigScreen.matchesTab(option, TweakifyModConfigScreen.ConfigTab.FUNCTIONS),
			"scrollListMemory must not appear in the functions tab");
	}

	// 配置界面的搜索高亮与平滑滚动只属于「通用」标签，不占用功能/修改标签。
	@Test
	void configViewInteractionOptionsAreGeneralOnly() {
		assertTab("searchHighlight", TweakifyModConfigScreen.ConfigTab.GENERAL);
		assertTab("smoothScroll", TweakifyModConfigScreen.ConfigTab.GENERAL);
		// 平滑滚动的子设置（按界面开关 / 时长）同样归入通用标签。
		assertTab("smoothScrollConfigList", TweakifyModConfigScreen.ConfigTab.GENERAL);
		assertTab("smoothScrollCreative", TweakifyModConfigScreen.ConfigTab.GENERAL);
		assertTab("smoothScrollChat", TweakifyModConfigScreen.ConfigTab.GENERAL);
		assertTab("smoothScrollModMenu", TweakifyModConfigScreen.ConfigTab.GENERAL);
		assertTab("smoothScrollDuration", TweakifyModConfigScreen.ConfigTab.GENERAL);
		assertFalse(TweakifyModConfigScreen.matchesTab(
			TweakifyModConfigOptions.optionOf("searchHighlight"), TweakifyModConfigScreen.ConfigTab.FUNCTIONS),
			"searchHighlight must not appear in the functions tab");
		assertFalse(TweakifyModConfigScreen.matchesTab(
			TweakifyModConfigOptions.optionOf("smoothScroll"), TweakifyModConfigScreen.ConfigTab.FUNCTIONS),
			"smoothScroll must not appear in the functions tab");
	}

	// 锹单层铲雪归入“功能”标签（子项随父项一起显示）。
	@Test
	void shovelSwitchIsInFunctionsTab() {
		IConfigBase option = TweakifyModConfigOptions.optionOf("disableShovelSnowFullBreak");
		assertTrue(option != null, "disableShovelSnowFullBreak option must exist");
		assertTrue(TweakifyModConfigScreen.matchesTab(option, TweakifyModConfigScreen.ConfigTab.FUNCTIONS),
			"disableShovelSnowFullBreak should appear in the functions tab");
		assertFalse(TweakifyModConfigScreen.matchesTab(option, TweakifyModConfigScreen.ConfigTab.MODIFIED),
			"disableShovelSnowFullBreak should no longer appear in the modified tab");
	}

	// “功能”标签只给根功能提供快捷键；子设置全部不带快捷键。
	@Test
	void functionTabHotkeysOnlyOnRootFeatures() {
		for (ConfigCatalog.Entry entry : ConfigCatalog.entries()) {
			IConfigBase option = TweakifyModConfigOptions.optionOf(entry.name());
			if (!(option instanceof IConfigBoolean)
				|| !TweakifyModConfigScreen.matchesTab(option, TweakifyModConfigScreen.ConfigTab.FUNCTIONS)) {
				continue;
			}
			if (entry.isRoot()) {
				assertTrue(option instanceof IHotkey, "function root needs a hotkey: " + entry.name());
			} else {
				assertFalse(option instanceof IHotkey, "function child must not have a hotkey: " + entry.name());
			}
		}
	}

	// “优化”标签同样只放开关类优化，不提供快捷键设置。
	@Test
	void optimizationTabHasNoHotkeyOptions() {
		for (IConfigBase option : TweakifyModConfigOptions.ALL_OPTIONS) {
			if (TweakifyModConfigScreen.matchesTab(option, TweakifyModConfigScreen.ConfigTab.OPTIMIZATION)) {
				assertFalse(option instanceof IHotkey,
					"optimization option must not expose a hotkey: " + option.getName());
			}
		}
	}

	// “修复”标签只放开关类修复，不提供快捷键设置。
	@Test
	void fixingTabHasNoHotkeyOptions() {
		for (IConfigBase option : TweakifyModConfigOptions.ALL_OPTIONS) {
			if (TweakifyModConfigScreen.matchesTab(option, TweakifyModConfigScreen.ConfigTab.FIXING)) {
				assertFalse(option instanceof IHotkey,
					"fix option must not expose a hotkey: " + option.getName());
			}
		}
	}

	@Test
	void tabsFollowRequestedOrder() {
		assertArrayEquals(new TweakifyModConfigScreen.ConfigTab[] {
			TweakifyModConfigScreen.ConfigTab.ALL,
			TweakifyModConfigScreen.ConfigTab.GENERAL,
			TweakifyModConfigScreen.ConfigTab.FUNCTIONS,
			// 显示标签位于功能右侧。
			TweakifyModConfigScreen.ConfigTab.DISPLAY,
			TweakifyModConfigScreen.ConfigTab.MODIFIED,
			TweakifyModConfigScreen.ConfigTab.FIXING,
			TweakifyModConfigScreen.ConfigTab.OPTIMIZATION,
			TweakifyModConfigScreen.ConfigTab.TESTING,
			// 收藏标签固定在“测试”右侧。
			TweakifyModConfigScreen.ConfigTab.FAVORITES
		}, TweakifyModConfigScreen.ConfigTab.values());
	}

	// 收藏标签只显示收藏项本身：按配置树顺序、保留真实缩进，未收藏的父分组不出现。
	// 取消收藏后重新取列表，该行应立即消失。
	@Test
	void favoritesTabShowsOnlyFavoritedOptions(@TempDir Path tempDir) {
		TweakifyModConfig previousConfig = TweakifyMod.config();
		TweakifyModConfig config = new TweakifyModConfig();
		// 本用例验证「逐项收藏」旧行为：显式关闭父项连带子项。
		config.general.favoriteIncludeChildren = false;
		TweakifyModTestSupport.installConfig(config);
		FavoritesStorage.resetForTest(tempDir.resolve(FavoritesStorage.FILE_NAME));
		try {
			assertTrue(FavoritesStorage.toggleConfigFavorite("cameraMotion"));
			assertTrue(FavoritesStorage.toggleConfigFavorite("cameraMotionIntensity"));
			List<String> names = TweakifyModConfigScreen.visibleConfigs(
				TweakifyModConfigScreen.ConfigTab.FAVORITES).stream().map(IConfigBase::getName).toList();
			assertEquals(List.of("cameraMotion", "cameraMotionIntensity"), names);
			assertFalse(names.contains("smoothScroll"), "unfavorited option must not appear");

			FavoritesStorage.toggleConfigFavorite("cameraMotion");
			assertFalse(TweakifyModConfigScreen.visibleConfigs(
				TweakifyModConfigScreen.ConfigTab.FAVORITES).stream()
				.anyMatch(option -> option.getName().equals("cameraMotion")),
				"unfavorited option must disappear immediately");
		} finally {
			// 等待异步写盘完成任务，避免后台线程在临时目录清理时仍写文件。
			AsyncFileWriter.drain(15_000L);
			FavoritesStorage.clearForTest();
			TweakifyModTestSupport.installConfig(previousConfig);
			// 恢复 ALL 标签的 NODE_INFO，避免影响其它用例。
			TweakifyModConfigScreen.visibleConfigs(TweakifyModConfigScreen.ConfigTab.ALL);
		}
	}

	// 收藏系统总开关关闭时：收藏标签为空、星标禁用，但收藏数据仍在存储层保留。
	@Test
	void disablingFavoriteSystemHidesFavoritesContent(@TempDir Path tempDir) {
		TweakifyModConfig previousConfig = TweakifyMod.config();
		TweakifyModConfig config = new TweakifyModConfig();
		config.general.favoriteSystem = false;
		TweakifyModTestSupport.installConfig(config);
		FavoritesStorage.resetForTest(tempDir.resolve(FavoritesStorage.FILE_NAME));
		try {
			assertTrue(FavoritesStorage.toggleConfigFavorite("cameraMotion"));
			assertFalse(FavoriteStarRenderer.isEnabled(), "the master switch must disable star rendering");
			assertTrue(TweakifyModConfigScreen.visibleConfigs(
				TweakifyModConfigScreen.ConfigTab.FAVORITES).isEmpty(),
				"the favorites tab must be empty while the system is disabled");
		} finally {
			AsyncFileWriter.drain(15_000L);
			FavoritesStorage.clearForTest();
			TweakifyModTestSupport.installConfig(previousConfig);
			TweakifyModConfigScreen.visibleConfigs(TweakifyModConfigScreen.ConfigTab.ALL);
		}
	}

	// 「收藏包含子项」开启时，收藏父项会连带显示全部后代；关闭后回到逐项收藏。
	@Test
	void favoritesTabIncludesChildrenOfFavoritedParents(@TempDir Path tempDir) {
		TweakifyModConfig previousConfig = TweakifyMod.config();
		TweakifyModConfig config = new TweakifyModConfig();
		config.general.favoriteIncludeChildren = true;
		TweakifyModTestSupport.installConfig(config);
		FavoritesStorage.resetForTest(tempDir.resolve(FavoritesStorage.FILE_NAME));
		try {
			assertTrue(FavoritesStorage.toggleConfigFavorite("cameraMotion"));
			List<String> names = TweakifyModConfigScreen.visibleConfigs(
				TweakifyModConfigScreen.ConfigTab.FAVORITES).stream().map(IConfigBase::getName).toList();
			assertTrue(names.contains("cameraMotion"), "the favorited parent must appear");
			assertTrue(names.contains("cameraMotionIntensity"),
				"children of a favorited parent must appear when the option is enabled");
			assertTrue(names.contains("cameraMotionTurningRoll"),
				"all descendants of a favorited parent must appear");
			assertFalse(names.contains("smoothScroll"),
				"unrelated options must stay hidden");

			config.general.favoriteIncludeChildren = false;
			names = TweakifyModConfigScreen.visibleConfigs(
				TweakifyModConfigScreen.ConfigTab.FAVORITES).stream().map(IConfigBase::getName).toList();
			assertEquals(List.of("cameraMotion"), names,
				"disabling the option must return to per-item favorites only");
		} finally {
			AsyncFileWriter.drain(15_000L);
			FavoritesStorage.clearForTest();
			TweakifyModTestSupport.installConfig(previousConfig);
			TweakifyModConfigScreen.visibleConfigs(TweakifyModConfigScreen.ConfigTab.ALL);
		}
	}

	// 收藏标签由收藏集合驱动，不按目录分类命中。
	@Test
	void favoritesTabIsCollectionDriven() {
		for (IConfigBase option : TweakifyModConfigOptions.ALL_OPTIONS) {
			assertFalse(TweakifyModConfigScreen.matchesTab(
				option, TweakifyModConfigScreen.ConfigTab.FAVORITES));
		}
	}

	// 搜索必须能看到折叠分组里的子项，否则搜索父项后子项永远打不开。
	@Test
	void searchConfigsIgnoreGroupExpansion() {
		try {
			List<String> names = TweakifyModConfigScreen.searchConfigs(
				TweakifyModConfigScreen.ConfigTab.FUNCTIONS).stream().map(IConfigBase::getName).toList();
			assertTrue(names.contains("betterBlockPlacement"));
			assertTrue(names.contains("betterBlockPlacementMode"));
			assertTrue(names.contains("helpfulBlockSelection"));
			assertTrue(names.indexOf("betterBlockPlacement") < names.indexOf("betterBlockPlacementMode"),
				"搜索列表必须保持父项在子项之前的树顺序");
		} finally {
			TweakifyModConfigScreen.visibleConfigs(TweakifyModConfigScreen.ConfigTab.ALL);
		}
	}

	// 父项命中时带上整组子项；子项命中时只补父级路径，不拉入无关兄弟项。
	@Test
	void searchResultsFollowGroupStructure() {
		Set<String> parentMatch = TweakifyModConfigListWidget.expandMatchedNames(
			Set.of("betterBlockPlacement"));
		assertTrue(parentMatch.contains("betterBlockPlacementSneakBypass"));
		assertTrue(parentMatch.contains("betterBlockPlacementMode"));
		assertFalse(parentMatch.contains("helpfulBlockSelection"),
			"辅助方块选区已成为独立根开关，不属于该分组");

		Set<String> childMatch = TweakifyModConfigListWidget.expandMatchedNames(
			Set.of("betterBlockPlacementMode"));
		assertTrue(childMatch.contains("betterBlockPlacement"), "子项命中时应补父级路径");
		assertFalse(childMatch.contains("betterBlockPlacementSneakBypass"),
			"不应拉入未命中的兄弟项");
	}

	// 搜索结果显示为已展开、无折叠按钮，避免出现点了没反应的 [+]。
	@Test
	void searchResultsHideExpandButtons() {
		try {
			Set<String> included = TweakifyModConfigListWidget.expandMatchedNames(
				Set.of("betterBlockPlacement"));
			TweakifyModConfigScreen.markSearchVisible(included);
			TweakifyModConfigScreen.NodeInfo info = TweakifyModConfigScreen.getNodeInfo(
				TweakifyModConfigOptions.optionOf("betterBlockPlacement"));
			assertNotNull(info);
			assertFalse(info.hasChildren(), "搜索结果的父项不应再显示展开按钮");
			assertTrue(info.expanded(), "搜索结果按已展开排版");
			assertEquals(0, ConfigCatalog.depthOf("betterBlockPlacement"));
			assertEquals(1, ConfigCatalog.depthOf("betterBlockPlacementMode"));
		} finally {
			TweakifyModConfigScreen.visibleConfigs(TweakifyModConfigScreen.ConfigTab.ALL);
		}
	}

	// 只对纯文本搜索启用树感知；快捷键过滤（单独或与文本组合）完全交回 MaLiLib。
	@Test
	void treeSearchOnlyAppliesToPlainText() {
		assertTrue(TweakifyModConfigListWidget.shouldUseTreeSearch(true, false, "better"));
		assertFalse(TweakifyModConfigListWidget.shouldUseTreeSearch(true, true, "better"),
			"文本与快捷键组合时不能补父子项");
		assertFalse(TweakifyModConfigListWidget.shouldUseTreeSearch(true, true, ""),
			"快捷键过滤不能被树感知扩展影响");
		assertFalse(TweakifyModConfigListWidget.shouldUseTreeSearch(true, true, "   "));
		assertFalse(TweakifyModConfigListWidget.shouldUseTreeSearch(false, false, ""));
		assertFalse(TweakifyModConfigListWidget.shouldUseTreeSearch(true, false, "   "));
	}

	// 收藏标签不参与树扩展，否则会把未收藏的父子项带进结果。
	@Test
	void treeExpansionSkipsFavoritesTab() {
		assertTrue(TweakifyModConfigListWidget.shouldApplyTreeExpansion(
			TweakifyModConfigScreen.ConfigTab.ALL, true));
		assertFalse(TweakifyModConfigListWidget.shouldApplyTreeExpansion(
			TweakifyModConfigScreen.ConfigTab.FAVORITES, true));
		assertFalse(TweakifyModConfigListWidget.shouldApplyTreeExpansion(
			TweakifyModConfigScreen.ConfigTab.ALL, false));
	}

	// 收藏标签搜索仍只列收藏项：父项命中不带未收藏子项，子项命中也不补父级路径。
	@Test
	void favoritesSearchKeepsOnlyFavoritedOptions(@TempDir Path tempDir) {
		TweakifyModConfig previousConfig = TweakifyMod.config();
		TweakifyModConfig config = new TweakifyModConfig();
		config.general.favoriteIncludeChildren = false;
		TweakifyModTestSupport.installConfig(config);
		FavoritesStorage.resetForTest(tempDir.resolve(FavoritesStorage.FILE_NAME));
		try {
			assertTrue(FavoritesStorage.toggleConfigFavorite("betterBlockPlacement"));
			List<String> names = TweakifyModConfigScreen.searchConfigs(
				TweakifyModConfigScreen.ConfigTab.FAVORITES).stream().map(IConfigBase::getName).toList();
			assertEquals(List.of("betterBlockPlacement"), names,
				"收藏标签搜索不能把未收藏的子项带进来");

			assertFalse(FavoritesStorage.toggleConfigFavorite("betterBlockPlacement"));
			assertTrue(FavoritesStorage.toggleConfigFavorite("betterBlockPlacementMode"));
			names = TweakifyModConfigScreen.searchConfigs(
				TweakifyModConfigScreen.ConfigTab.FAVORITES).stream().map(IConfigBase::getName).toList();
			assertEquals(List.of("betterBlockPlacementMode"), names,
				"收藏标签搜索不能给命中的子项补父级路径");
		} finally {
			AsyncFileWriter.drain(15_000L);
			FavoritesStorage.clearForTest();
			TweakifyModTestSupport.installConfig(previousConfig);
			TweakifyModConfigScreen.visibleConfigs(TweakifyModConfigScreen.ConfigTab.ALL);
		}
	}

	@Test
	void newFeaturesAreClassifiedInRequestedTabs() {
		assertTab("betterBoneMeal", TweakifyModConfigScreen.ConfigTab.FUNCTIONS);
		assertTab("boneMealSugarCane", TweakifyModConfigScreen.ConfigTab.FUNCTIONS);
		assertTab("boneMealFlowers", TweakifyModConfigScreen.ConfigTab.FUNCTIONS);
		assertTab("forceCrawl", TweakifyModConfigScreen.ConfigTab.FUNCTIONS);
		assertTab("forceCrawlKey", TweakifyModConfigScreen.ConfigTab.FUNCTIONS);
		assertTab("climbablePassNoSlowdown", TweakifyModConfigScreen.ConfigTab.MODIFIED);
		assertTab("disableSpecifiedMobSpawn", TweakifyModConfigScreen.ConfigTab.FUNCTIONS);
		assertTab("disabledSpawnAllowPlayerSpawn", TweakifyModConfigScreen.ConfigTab.FUNCTIONS);
		assertTab("peacefulModeMonsters", TweakifyModConfigScreen.ConfigTab.FUNCTIONS);
		assertTab("throwSpawnEgg", TweakifyModConfigScreen.ConfigTab.FUNCTIONS);
		assertTab("thirdPersonPlayerName", TweakifyModConfigScreen.ConfigTab.MODIFIED);
		assertTab("betterBold", TweakifyModConfigScreen.ConfigTab.FIXING);
		assertTab("miniHudFpsOptimization", TweakifyModConfigScreen.ConfigTab.OPTIMIZATION);
		assertTab("signTextOptimization", TweakifyModConfigScreen.ConfigTab.OPTIMIZATION);
		// 冰霜行者跳跃修复归入修复标签。
		assertTab("frostWalkerJumpFix", TweakifyModConfigScreen.ConfigTab.FIXING);
		// 更好的定位栏回到「功能」标签：主开关与子设置都归入功能。
		assertTab("locatorBarHeads", TweakifyModConfigScreen.ConfigTab.FUNCTIONS);
		assertTab("locatorBarAlwaysShowXp", TweakifyModConfigScreen.ConfigTab.FUNCTIONS);
		assertTab("locatorBarHeadRenderHeads", TweakifyModConfigScreen.ConfigTab.FUNCTIONS);
		assertTab("locatorBarHeadScale", TweakifyModConfigScreen.ConfigTab.FUNCTIONS);
		assertTab("locatorBarHeadHatLayer", TweakifyModConfigScreen.ConfigTab.FUNCTIONS);
		assertTab("locatorBarHeadBorder", TweakifyModConfigScreen.ConfigTab.FUNCTIONS);
		assertTab("locatorBarHeadBorderThickness", TweakifyModConfigScreen.ConfigTab.FUNCTIONS);
		assertTab("locatorBarHeadBorderColor", TweakifyModConfigScreen.ConfigTab.FUNCTIONS);
		assertTab("locatorBarHeadNames", TweakifyModConfigScreen.ConfigTab.FUNCTIONS);
		assertTab("locatorBarHeadFilter", TweakifyModConfigScreen.ConfigTab.FUNCTIONS);
		assertTab("locatorBarHeadIncludedPlayers", TweakifyModConfigScreen.ConfigTab.FUNCTIONS);
		assertTab("locatorBarHeadExcludedPlayers", TweakifyModConfigScreen.ConfigTab.FUNCTIONS);
		assertTab("locatorBarHeadNearbyOnly", TweakifyModConfigScreen.ConfigTab.FUNCTIONS);
		assertTab("locatorBarCompass", TweakifyModConfigScreen.ConfigTab.FUNCTIONS);
		assertTab("locatorBarCompassColor", TweakifyModConfigScreen.ConfigTab.FUNCTIONS);
		assertTab("locatorBarCompassShadow", TweakifyModConfigScreen.ConfigTab.FUNCTIONS);
		assertTab("locatorBarCompassCoordinates", TweakifyModConfigScreen.ConfigTab.FUNCTIONS);
		// 新增功能按需求分类。
		assertTab("creativeFlightNoclip", TweakifyModConfigScreen.ConfigTab.FUNCTIONS);
		assertTab("chunkCenterIndicator", TweakifyModConfigScreen.ConfigTab.DISPLAY);
		assertTab("itemCountScale", TweakifyModConfigScreen.ConfigTab.MODIFIED);
		assertTab("betterAdvancements", TweakifyModConfigScreen.ConfigTab.MODIFIED);
		assertTab("betterAdvancementsPagination", TweakifyModConfigScreen.ConfigTab.MODIFIED);
		assertTab("betterAdvancementsPinTabs", TweakifyModConfigScreen.ConfigTab.MODIFIED);
		assertTab("betterAdvancementsRememberTab", TweakifyModConfigScreen.ConfigTab.MODIFIED);
		assertTab("betterAdvancementsDisableFade", TweakifyModConfigScreen.ConfigTab.MODIFIED);
		assertTab("betterAdvancementsCopyId", TweakifyModConfigScreen.ConfigTab.MODIFIED);
		assertTab("betterAdvancementsTabSpacing", TweakifyModConfigScreen.ConfigTab.MODIFIED);
		assertTab("betterAdvancementsPinnedTabSpacing", TweakifyModConfigScreen.ConfigTab.MODIFIED);
		// 辅助方块选区是功能标签下的独立根开关（与更好的方块放置分开放置）。
		assertTab("helpfulBlockSelection", TweakifyModConfigScreen.ConfigTab.FUNCTIONS);
		// 聊天签名隐藏与自定义游戏图标归入修改标签。
		assertTab("hideChatSigning", TweakifyModConfigScreen.ConfigTab.MODIFIED);
		assertTab("customWindowIcon", TweakifyModConfigScreen.ConfigTab.MODIFIED);
		assertTab("customWindowIconPath", TweakifyModConfigScreen.ConfigTab.MODIFIED);
		assertTab("bedFallDamageFix", TweakifyModConfigScreen.ConfigTab.FIXING);
		// 物品分组的子设置同样归入功能标签。
		assertTab("itemGroupsShowItems", TweakifyModConfigScreen.ConfigTab.FUNCTIONS);
		assertTab("itemGroupsAddAll", TweakifyModConfigScreen.ConfigTab.FUNCTIONS);
		assertTab("customItemGroups", TweakifyModConfigScreen.ConfigTab.FUNCTIONS);
		// 穿牌打开容器与四个移动物理禁用项归入修改标签。
		assertTab("signOpenContainer", TweakifyModConfigScreen.ConfigTab.MODIFIED);
		assertTab("disableCobwebSlowdown", TweakifyModConfigScreen.ConfigTab.MODIFIED);
		assertTab("cobwebSlowdownAllEntities", TweakifyModConfigScreen.ConfigTab.MODIFIED);
		assertTab("disableFluidPush", TweakifyModConfigScreen.ConfigTab.MODIFIED);
		assertTab("disableWaterPush", TweakifyModConfigScreen.ConfigTab.MODIFIED);
		assertTab("disableLavaPush", TweakifyModConfigScreen.ConfigTab.MODIFIED);
		assertTab("disableSlimeBlockSlowdown", TweakifyModConfigScreen.ConfigTab.MODIFIED);
		assertTab("disableSlimeBlockBounce", TweakifyModConfigScreen.ConfigTab.MODIFIED);
		assertTab("disableHoneyBlockSlowdown", TweakifyModConfigScreen.ConfigTab.MODIFIED);
		assertTab("disableHoneyBlockStick", TweakifyModConfigScreen.ConfigTab.MODIFIED);
		// 盔甲架手臂与隐藏展示框归入修改标签。
		assertTab("armorStandArms", TweakifyModConfigScreen.ConfigTab.MODIFIED);
		assertTab("hideItemFrames", TweakifyModConfigScreen.ConfigTab.MODIFIED);
		// 附魔台附魔信息归入功能标签；收藏包含子项归入通用标签。
		assertTab("enchantingTableInfo", TweakifyModConfigScreen.ConfigTab.FUNCTIONS);
		// 收藏系统为通用标签父项，星标样式与包含子项为子设置。
		assertTab("favoriteSystem", TweakifyModConfigScreen.ConfigTab.GENERAL);
		assertTab("favoriteStarStyle", TweakifyModConfigScreen.ConfigTab.GENERAL);
		assertTab("favoriteIncludeChildren", TweakifyModConfigScreen.ConfigTab.GENERAL);
		// 世界内可视化迁移到显示标签。
		for (String name : new String[] {
			"attackRangeDisplay", "attackRangeDisplayMode", "attackRangeEntities",
			"highlightSingleLayerBedrock", "barrierVisibility", "entityHighlight",
			"highlightPreset1", "customBlockOutline"
		}) {
			assertTab(name, TweakifyModConfigScreen.ConfigTab.DISPLAY);
		}
		assertTab("explosionRangeDisplay", TweakifyModConfigScreen.ConfigTab.DISPLAY);
		assertTab("sweepingView", TweakifyModConfigScreen.ConfigTab.DISPLAY);
		for (String name : new String[] {
			"sweepingViewWireframe", "sweepingViewWireframeColor", "sweepingViewFace",
			"sweepingViewFaceSolid", "sweepingViewFaceColor", "sweepingViewSphere",
			"sweepingViewSphereColor", "sweepingViewVehicles", "betterScreenshot",
			"betterScreenshotF9", "betterScreenshotFormat"
		}) {
			assertTab(name, TweakifyModConfigScreen.ConfigTab.DISPLAY);
		}
		// 取消速度上限归入修改标签。
		assertTab("removeSpeedLimit", TweakifyModConfigScreen.ConfigTab.MODIFIED);
		// 调试辅助功能归入测试标签。
		assertTab("debugInfoOverlay", TweakifyModConfigScreen.ConfigTab.TESTING);
		assertTab("debugInfoShowPerformance", TweakifyModConfigScreen.ConfigTab.TESTING);
		assertTab("debugProbeReport", TweakifyModConfigScreen.ConfigTab.TESTING);
	}

	private static void assertTab(String name, TweakifyModConfigScreen.ConfigTab expected) {
		IConfigBase option = TweakifyModConfigOptions.optionOf(name);
		assertTrue(option != null, name + " option must exist");
		assertTrue(TweakifyModConfigScreen.matchesTab(option, expected),
			name + " should be shown in the " + expected + " tab");
	}

	@Test
	void testingTabHasProfileEditorAction() {
		assertNotNull(TestingConfigOptions.PROFILE_EDITOR,
			"profile editor action must exist");
		assertEquals("profileEditor", TestingConfigOptions.PROFILE_EDITOR.getName());
	}

	@Test
	void profileEditorIsUiOnly() {
		String name = TestingConfigOptions.PROFILE_EDITOR.getName();
		assertFalse(TweakifyModConfigOptions.ALL_OPTIONS.stream()
			.anyMatch(option -> option.getName().equals(name)),
			"profile editor must not be a persisted config option");
		assertFalse(TweakifyModConfigOptions.TOGGLE_OPTIONS.stream()
			.anyMatch(option -> option.getName().equals(name)),
			"profile editor must not be counted as a toggle");
		assertFalse(TweakifyModConfigOptions.HOTKEY_OPTIONS.stream()
			.anyMatch(option -> option.getName().equals(name)),
			"profile editor must not be a hotkey option");
		assertFalse(TweakifyModConfig.hasField(name),
			"profile editor must not have a config field");
		assertNull(ConfigCatalog.entryOf(name),
			"profile editor must not be registered in ConfigCatalog");
		assertFalse(TweakifyModConfigScreen.treeConfigNames().contains(name),
			"profile editor is appended by the testing tab, not the config tree");
	}

	@Test
	void functionAndModifiedTabsAreDisjoint() {
		for (IConfigBase option : TweakifyModConfigOptions.ALL_OPTIONS) {
			boolean functions = TweakifyModConfigScreen.matchesTab(
				option, TweakifyModConfigScreen.ConfigTab.FUNCTIONS);
			boolean display = TweakifyModConfigScreen.matchesTab(
				option, TweakifyModConfigScreen.ConfigTab.DISPLAY);
			boolean modified = TweakifyModConfigScreen.matchesTab(
				option, TweakifyModConfigScreen.ConfigTab.MODIFIED);
			boolean general = TweakifyModConfigScreen.matchesTab(
				option, TweakifyModConfigScreen.ConfigTab.GENERAL);
			boolean fixing = TweakifyModConfigScreen.matchesTab(
				option, TweakifyModConfigScreen.ConfigTab.FIXING);
			boolean optimization = TweakifyModConfigScreen.matchesTab(
				option, TweakifyModConfigScreen.ConfigTab.OPTIMIZATION);
			boolean testing = TweakifyModConfigScreen.matchesTab(
				option, TweakifyModConfigScreen.ConfigTab.TESTING);
			assertFalse(functions && modified, "option is in both tabs: " + option.getName());
			assertFalse(functions && display, "option is in both tabs: " + option.getName());
			assertFalse(display && modified, "option is in both tabs: " + option.getName());
			assertTrue(functions || display || modified || general || fixing || optimization || testing,
				"option has no concrete tab: " + option.getName());
		}
	}

	// code → 标签页映射：每个 ConfigGuiTab 的翻译键必须与目标标签一致，未知 code 回退 ALL。
	@Test
	void tabMappingCoversEveryConfigGuiTab() {
		for (ConfigGuiTab tab : ConfigGuiTab.values()) {
			assertEquals(tab.translationKey(), TweakifyModConfigScreen.tabOfCode(tab.code()).translationKey(),
				"mismatched tab for code: " + tab.code());
		}
		assertEquals(TweakifyModConfigScreen.ConfigTab.ALL,
			TweakifyModConfigScreen.tabOfCode("not-a-tab"));
		assertEquals(TweakifyModConfigScreen.ConfigTab.ALL,
			TweakifyModConfigScreen.tabOfCode(null));
	}
}
