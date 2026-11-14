package com.gtzc.tweakify.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gtzc.tweakify.config.ConfigCatalog;
import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigStringList;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 语言文件必须保持相同键集：缺失键会静默回退到游戏语言，游戏内很难发现漏翻。
 */
class LangFilesTest {
	// 选项实例需要枚举/注册表静态数据：先按其它测试类的方式引导一次。
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void allLanguageFilesShareTheSameKeys() throws IOException {
		Set<String> reference = keysOf(TweakifyModConfig.SUPPORTED_LANGUAGES.get(0));
		assertFalse(reference.isEmpty(), "language file must not be empty");
		for (String code : TweakifyModConfig.SUPPORTED_LANGUAGES) {
			assertEquals(reference, keysOf(code), "key set mismatch in " + code);
		}
	}

	// 代码里引用的枚举翻译键必须在每种语言都存在，否则界面会显示原始键名。
	@Test
	void blockOutlineOptionTranslationsExistInEveryLanguage() throws IOException {
		for (String code : TweakifyModConfig.SUPPORTED_LANGUAGES) {
			Set<String> keys = keysOf(code);
			for (BlockOutlinePalette palette : BlockOutlinePalette.values()) {
				assertTrue(keys.contains(palette.translationKey()),
					"missing " + palette.translationKey() + " in " + code);
			}
			for (BlockOutlineColorMode mode : BlockOutlineColorMode.values()) {
				assertTrue(keys.contains(mode.translationKey()),
					"missing " + mode.translationKey() + " in " + code);
			}
		}
	}

	// 新增配置项的名称/提示/说明键必须在四种语言都存在。
	@Test
	void blockOutlineConfigTranslationsExistInEveryLanguage() throws IOException {
		List<String> optionNames = List.of(
			"customBlockOutline", "blockOutlineWidth", "blockOutlineAnimationSpeed",
			"blockOutlinePalette", "blockOutlineColorMode");
		for (String code : TweakifyModConfig.SUPPORTED_LANGUAGES) {
			Set<String> keys = keysOf(code);
			for (String optionName : optionNames) {
				for (String kind : List.of("name", "prettyName", "comment")) {
					String key = expectedKeyPrefix(TweakifyModConfigOptions.optionOf(optionName))
						+ kind + "." + optionName;
					assertTrue(keys.contains(key), "missing " + key + " in " + code);
				}
			}
		}
	}

	// 药水时间、地狱门延迟与肩膀鹦鹉的配置项翻译键必须在四种语言都存在。
	@Test
	void hudPortalAndParrotTranslationsExistInEveryLanguage() throws IOException {
		List<String> optionNames = List.of(
			"statusEffectDurationDisplay", "netherPortalTeleportDelay",
			"netherPortalSurvivalInstantTeleport", "netherPortalCreativeDelayedTeleport",
			"parrotDanceOnShoulder");
		for (String code : TweakifyModConfig.SUPPORTED_LANGUAGES) {
			Set<String> keys = keysOf(code);
			for (String optionName : optionNames) {
				for (String kind : List.of("name", "prettyName", "comment")) {
					String key = expectedKeyPrefix(TweakifyModConfigOptions.optionOf(optionName))
						+ kind + "." + optionName;
					assertTrue(keys.contains(key), "missing " + key + " in " + code);
				}
			}
		}
	}

	// 生成控制、和平模式怪物与更好的粗体的翻译键必须在四种语言都存在。
	@Test
	void spawnBoldAndPeacefulTranslationsExistInEveryLanguage() throws IOException {
		List<String> optionNames = List.of(
			"betterBold", "disableSpecifiedMobSpawn", "disabledSpawnEntities",
			"disabledSpawnAllowPlayerSpawn",
			"peacefulModeMonsters", "peacefulModeMonsterEntities",
			"peacefulModeMonsterSpawn", "peacefulModeMonsterNoAttack");
		for (String code : TweakifyModConfig.SUPPORTED_LANGUAGES) {
			Set<String> keys = keysOf(code);
			for (String optionName : optionNames) {
				for (String kind : List.of("name", "prettyName", "comment")) {
					String key = expectedKeyPrefix(TweakifyModConfigOptions.optionOf(optionName))
						+ kind + "." + optionName;
					assertTrue(keys.contains(key), "missing " + key + " in " + code);
				}
			}
		}
	}

	// 新增功能的配置项与选项翻译键必须在四种语言都存在。
	@Test
	void newFeatureTranslationsExistInEveryLanguage() throws IOException {
		List<String> optionNames = List.of(
			"piglinGoldArmorSwap", "piglinGoldArmorSwapMode", "chatInputMemory",
			"snowGolemBiomeImmunity", "cameraMotion", "cameraMotionIntensity",
			"cameraMotionTurningRoll", "cameraMotionStrafingRoll", "cameraMotionForwardPitch",
			"cameraMotionVerticalPitch", "cameraMotionIdleSway", "cameraMotionScreenShake",
			"cameraMotionMouseSmoothing", "cameraMotionFirstPerson", "cameraMotionThirdPerson",
			"itemGroups", "itemGroupsSort", "itemGroupsShowItems", "itemGroupsAddAll",
			"customItemGroups");
		for (String code : TweakifyModConfig.SUPPORTED_LANGUAGES) {
			Set<String> keys = keysOf(code);
			for (String optionName : optionNames) {
				for (String kind : List.of("name", "prettyName", "comment")) {
					String key = expectedKeyPrefix(TweakifyModConfigOptions.optionOf(optionName))
						+ kind + "." + optionName;
					assertTrue(keys.contains(key), "missing " + key + " in " + code);
				}
			}
			for (PiglinArmorSwapMode mode : PiglinArmorSwapMode.values()) {
				assertTrue(keys.contains(mode.translationKey()),
					"missing " + mode.translationKey() + " in " + code);
			}
			for (ItemGroupSort sort : ItemGroupSort.values()) {
				assertTrue(keys.contains(sort.translationKey()),
					"missing " + sort.translationKey() + " in " + code);
			}
		}
	}

	// 默认物品组的组名翻译键必须在四种语言都存在：
	// 键格式与参考模组（inventory-item-groups）一致，已有资源包可直接复用。
	@Test
	void defaultGroupNameTranslationsExistInEveryLanguage() throws IOException {
		Set<String> names = new LinkedHashSet<>();
		for (String tabId : List.of(
			"minecraft:building_blocks", "minecraft:colored_blocks", "minecraft:natural_blocks",
			"minecraft:functional_blocks", "minecraft:redstone_blocks",
			"minecraft:tools_and_utilities", "minecraft:combat",
			"minecraft:food_and_drinks", "minecraft:ingredients")) {
			for (CreativeItemGroups.GroupDefinition definition : CreativeItemGroups.definitionsFor(tabId)) {
				names.add(definition.name());
			}
		}
		assertFalse(names.isEmpty(), "default groups must be defined");
		for (String code : TweakifyModConfig.SUPPORTED_LANGUAGES) {
			Set<String> keys = keysOf(code);
			for (String name : names) {
				String key = "group_name.inventory_item_groups." + name;
				assertTrue(keys.contains(key), "missing " + key + " in " + code);
			}
		}
	}

	// 本轮新增功能（冰霜行者修复、定位栏头像、移动物理禁用、穿牌开容器、调试工具）的
	// 名称/提示/说明键必须在四种语言都存在。
	@Test
	void physicsAndLocatorFeatureTranslationsExistInEveryLanguage() throws IOException {
		List<String> optionNames = List.of(
			"frostWalkerJumpFix", "locatorBarHeads", "locatorBarHeadScale",
			"locatorBarHeadHatLayer", "locatorBarHeadBorder", "locatorBarHeadBorderThickness",
			"locatorBarHeadBorderColor", "locatorBarHeadNames", "locatorBarHeadFilter",
			"locatorBarHeadIncludedPlayers", "locatorBarHeadExcludedPlayers", "locatorBarHeadNearbyOnly", "locatorBarCompass",
			"locatorBarCompassColor", "locatorBarCompassShadow", "locatorBarCompassCoordinates",
			"locatorBarAlwaysShowXp",
			"signOpenContainer",
			"disableCobwebSlowdown", "cobwebSlowdownAllEntities",
			"disableFluidPush", "disableWaterPush", "disableLavaPush",
			"disableSlimeBlockSlowdown", "disableSlimeBlockBounce",
			"disableHoneyBlockSlowdown", "disableHoneyBlockStick",
			"debugInfoOverlay", "debugInfoShowPerformance", "debugInfoShowTarget",
			"debugInfoShowBiome", "debugInfoShowSpeed", "debugInfoShowEntities",
			"debugInfoShowConfigStats", "debugInfoShowEffects",
			"debugInfoShowHooks", "debugProbeReport", "debugProbeReportCopy",
			"debugEnabledFeaturesReport",
			"barrierVisibilityMaxMarkers", "durabilityOverlayColorMode",
			"copyItemIdNamespace", "statusEffectDurationAlwaysSeconds",
			"attackRangeHostileOnly", "attackRangeIncludeInvisible");
		for (String code : TweakifyModConfig.SUPPORTED_LANGUAGES) {
			Set<String> keys = keysOf(code);
			for (String optionName : optionNames) {
				for (String kind : List.of("name", "prettyName", "comment")) {
					String key = expectedKeyPrefix(TweakifyModConfigOptions.optionOf(optionName))
						+ kind + "." + optionName;
					assertTrue(keys.contains(key), "missing " + key + " in " + code);
				}
			}
		}
	}

	// 26.3 全部原版附魔都应有描述键，防止漏配。
	// 清单对齐 26.3 的 data/minecraft/tags/enchantment/tooltip_order.json（ 43 个），升级 MC 时需同步。
	// 四语言的键集一致性由 allLanguageFilesShareTheSameKeys 保证。
	@Test
	void everyVanillaEnchantmentHasDescriptionKey() throws IOException {
		List<String> enchantments = List.of(
			"aqua_affinity", "bane_of_arthropods", "binding_curse", "blast_protection", "breach",
			"channeling", "density", "depth_strider", "efficiency", "feather_falling", "fire_aspect",
			"fire_protection", "flame", "fortune", "frost_walker", "impaling", "infinity", "knockback",
			"looting", "loyalty", "luck_of_the_sea", "lunge", "lure", "mending", "multishot", "piercing",
			"power", "projectile_protection", "protection", "punch", "quick_charge", "respiration",
			"riptide", "sharpness", "silk_touch", "smite", "soul_speed", "sweeping_edge", "swift_sneak",
			"thorns", "unbreaking", "vanishing_curse", "wind_burst");
		for (String code : TweakifyModConfig.SUPPORTED_LANGUAGES) {
			Map<String, String> values = valuesOf(code);
			for (String id : enchantments) {
				String key = "enchantment.minecraft." + id + ".desc";
				String value = values.get(key);
				assertNotNull(value, code + " is missing enchantment description for " + id);
				assertFalse(value.isBlank(), code + " has blank enchantment description for " + id);
				assertNotEquals(key, value, code + " uses the raw key as description for " + id);
			}
		}
	}

	@Test
	void commandLabelTranslationsExistInEveryLanguage() throws IOException {
		for (String code : TweakifyModConfig.SUPPORTED_LANGUAGES) {
			Set<String> keys = keysOf(code);
			for (ConfigCatalog.Entry entry : ConfigCatalog.commandEntries()) {
				String key = "tweakify-mod.command.label." + entry.name();
				assertTrue(keys.contains(key), "missing " + key + " in " + code);
			}
		}
	}

	@Test
	void resourcePackCompatibilityTranslationsExistInEveryLanguage() throws IOException {
		for (String code : TweakifyModConfig.SUPPORTED_LANGUAGES) {
			Set<String> keys = keysOf(code);
			for (String kind : List.of("name", "prettyName", "comment")) {
				String key = expectedKeyPrefix(
					TweakifyModConfigOptions.optionOf("disableResourcePackCompatibilityCheck")) + kind
					+ ".disableResourcePackCompatibilityCheck";
				assertTrue(keys.contains(key), "missing " + key + " in " + code);
			}
		}
	}

	@Test
	void profileEditorTranslationsExistInEveryLanguage() throws IOException {
		List<String> keysToCheck = List.of(
			"tweakify-mod.gui.tab.testing",
			"tweakify-mod.config.general.name.profileEditor",
			"tweakify-mod.config.general.prettyName.profileEditor",
			"tweakify-mod.config.general.comment.profileEditor",
			"tweakify-mod.gui.profile_editor.title",
			"tweakify-mod.gui.profile_editor.open",
			"tweakify-mod.gui.profile_editor.name",
			"tweakify-mod.gui.profile_editor.save",
			"tweakify-mod.gui.profile_editor.refresh",
			"tweakify-mod.gui.profile_editor.load",
			"tweakify-mod.gui.profile_editor.delete",
			"tweakify-mod.gui.profile_editor.delete_confirm",
			"tweakify-mod.gui.profile_editor.back",
			"tweakify-mod.gui.profile_editor.list",
			"tweakify-mod.gui.profile_editor.empty",
			"tweakify-mod.gui.profile_editor.name_required",
			"tweakify-mod.gui.profile_editor.scroll_hint",
			"tweakify-mod.command.profile.load_failed",
			"tweakify-mod.command.profile.delete_failed");
		for (String code : TweakifyModConfig.SUPPORTED_LANGUAGES) {
			Set<String> keys = keysOf(code);
			for (String key : keysToCheck) {
				assertTrue(keys.contains(key), "missing " + key + " in " + code);
			}
		}
	}

	// 定位栏新增选项的枚举值翻译键必须在四种语言都存在（描边样式 / 名字时机 / 过滤 / 颜色）。
	@Test
	void locatorOptionValueTranslationsExistInEveryLanguage() throws IOException {
		for (String code : TweakifyModConfig.SUPPORTED_LANGUAGES) {
			Set<String> keys = keysOf(code);
			for (LocatorHeadBorder border : LocatorHeadBorder.values()) {
				assertTrue(keys.contains(border.translationKey()),
					"missing " + border.translationKey() + " in " + code);
			}
			for (LocatorHeadNames names : LocatorHeadNames.values()) {
				assertTrue(keys.contains(names.translationKey()),
					"missing " + names.translationKey() + " in " + code);
			}
			for (LocatorHeadFilter filter : LocatorHeadFilter.values()) {
				assertTrue(keys.contains(filter.translationKey()),
					"missing " + filter.translationKey() + " in " + code);
			}
			for (LocatorHeadColor color : LocatorHeadColor.values()) {
				assertTrue(keys.contains(color.translationKey()),
					"missing " + color.translationKey() + " in " + code);
			}
			for (LocatorHeadBorderThickness thickness : LocatorHeadBorderThickness.values()) {
				assertTrue(keys.contains(thickness.translationKey()),
					"missing " + thickness.translationKey() + " in " + code);
			}
		}
	}

	// 配置项的翻译键前缀由 TweakifyModConfigOptions 里的 .apply(...) 决定：
	// 高亮组 → config.highlight、字符串列表 → config.lists、其余 → config.general。
	// 前缀写错时界面会直接显示原始键名（英文），这里逐项校验。
	@Test
	void everyOptionUsesThePrefixMatchingItsRegistration() throws IOException {
		for (String code : TweakifyModConfig.SUPPORTED_LANGUAGES) {
			Set<String> keys = keysOf(code);
			for (IConfigBase option : TweakifyModConfigOptions.ALL_OPTIONS) {
				String prefix = expectedKeyPrefix(option);
				for (String kind : List.of("name", "prettyName", "comment")) {
					String key = prefix + kind + "." + option.getName();
					assertTrue(keys.contains(key), "missing " + key + " in " + code);
				}
			}
		}
	}

	private static String expectedKeyPrefix(IConfigBase option) {
		if (ConfigCatalog.isInsideGroup("entityHighlight", option.getName())) {
			return "tweakify-mod.config.highlight.";
		}
		if (option instanceof IConfigStringList) {
			return "tweakify-mod.config.lists.";
		}
		return "tweakify-mod.config.general.";
	}

	private static Set<String> keysOf(String code) throws IOException {
		return valuesOf(code).keySet();
	}

	// 读取完整键值对：附魔描述需要同时校验键存在、值非空且不是原始键。
	private static Map<String, String> valuesOf(String code) throws IOException {
		String path = "/assets/tweakify-mod/lang/" + code + ".json";
		try (InputStream stream = LangFilesTest.class.getResourceAsStream(path)) {
			assertNotNull(stream, "missing resource " + path);
			JsonObject json = JsonParser.parseReader(
				new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
			Map<String, String> values = new HashMap<>();
			for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
				JsonElement value = entry.getValue();
				// 非字符串值按空值处理，交给断言给出带键名的失败信息，而不是直接抛异常。
				values.put(entry.getKey(), value.isJsonPrimitive() ? value.getAsString() : "");
			}
			return values;
		}
	}
}
