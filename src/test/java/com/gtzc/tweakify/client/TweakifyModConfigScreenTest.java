package com.gtzc.tweakify.client;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import com.gtzc.tweakify.config.ConfigCatalog;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
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
			TweakifyModConfigScreen.ConfigTab.MODIFIED,
			TweakifyModConfigScreen.ConfigTab.FIXING,
			TweakifyModConfigScreen.ConfigTab.OPTIMIZATION,
			TweakifyModConfigScreen.ConfigTab.TESTING
		}, TweakifyModConfigScreen.ConfigTab.values());
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
	}

	private static void assertTab(String name, TweakifyModConfigScreen.ConfigTab expected) {
		IConfigBase option = TweakifyModConfigOptions.optionOf(name);
		assertTrue(option != null, name + " option must exist");
		assertTrue(TweakifyModConfigScreen.matchesTab(option, expected),
			name + " should be shown in the " + expected + " tab");
	}

	@Test
	void testingTabHasProfileEditorAction() {
		assertNotNull(TweakifyModConfigOptions.PROFILE_EDITOR,
			"profile editor action must exist");
		assertEquals("profileEditor", TweakifyModConfigOptions.PROFILE_EDITOR.getName());
	}

	@Test
	void profileEditorIsUiOnly() {
		String name = TweakifyModConfigOptions.PROFILE_EDITOR.getName();
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
			assertTrue(functions || modified || general || fixing || optimization || testing,
				"option has no concrete tab: " + option.getName());
		}
	}
}
