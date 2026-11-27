package com.gtzc.tweakify.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gtzc.tweakify.client.config.CodeOptionEntry;
import com.gtzc.tweakify.client.config.TweakifyModConfigOptions;
import com.gtzc.tweakify.client.state.CreativeItemGroups;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 语言文件必须保持相同键集：缺失键会静默回退到游戏语言，游戏内很难发现漏翻。
 */
class LangFilesTest {
	private static final Path OPTION_SOURCE_DIR = Path.of("src/client/java/com/gtzc/tweakify/client/config");
	private static final String OPTION_PACKAGE = "com.gtzc.tweakify.client.config";
	// 只认类型声明里的 implements：注释里提到接口名不算实现。
	private static final Pattern IMPLEMENTS_OPTION_ENTRY =
		Pattern.compile("(?s)\\bimplements\\b[^{]*?\\bCodeOptionEntry\\b");

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

	// MaLiLib 的注释渲染会连续执行两次 String.format，comment 里的 % 必须写成 %%%%；
	// 写成单 % 或 %s 都会在 tooltip 里显示 Format Error。这里对全部 comment 键做冒烟。
	@Test
	void everyCommentSurvivesDoubleFormatting() throws IOException {
		for (String code : TweakifyModConfig.SUPPORTED_LANGUAGES) {
			Map<String, String> values = valuesOf(code);
			for (IConfigBase option : TweakifyModConfigOptions.ALL_OPTIONS) {
				String key = expectedKeyPrefix(option) + "comment." + option.getName();
				String raw = values.get(key);
				assertNotNull(raw, code + " is missing " + key);
				String rendered = assertDoesNotThrow(() ->
					String.format(Locale.ROOT, String.format(Locale.ROOT, raw)),
					code + " 的 " + key + " 无法通过两次 String.format");
				assertFalse(rendered.contains("Format Error"),
					code + " 的 " + key + " 出现了 Format Error");
			}
		}
	}

	// 上一项只保证“不报错”，这里保留内容基线：%%%% 转义后必须原样显示 50%/200%。
	@Test
	void scalePercentCommentRendersLiteralPercent() throws IOException {
		String key = "tweakify-mod.config.general.comment.statusEffectDurationScale";
		for (String code : TweakifyModConfig.SUPPORTED_LANGUAGES) {
			String raw = valuesOf(code).get(key);
			assertNotNull(raw, code + " is missing " + key);
			String rendered = String.format(Locale.ROOT, String.format(Locale.ROOT, raw));
			assertTrue(rendered.contains("50%"), code + " comment must show 50% after formatting");
			assertTrue(rendered.contains("200%"), code + " comment must show 200% after formatting");
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

	// 收藏相关的界面文本不在配置项目录里；配置项与枚举键由上面两个通用测试覆盖。
	@Test
	void favoriteTranslationsExistInEveryLanguage() throws IOException {
		for (String code : TweakifyModConfig.SUPPORTED_LANGUAGES) {
			Set<String> keys = keysOf(code);
			assertTrue(keys.contains("tweakify-mod.gui.tab.favorites"),
				"missing favorites tab label in " + code);
			assertTrue(keys.contains("tweakify-mod.gui.world_list.favorite_delete_blocked"),
				"missing favorite delete hint in " + code);
		}
	}

	// 存档列表的时长文本不在配置项目录里；格式枚举键由枚举通用测试覆盖。
	@Test
	void worldPlayTimeTranslationsExistInEveryLanguage() throws IOException {
		for (String code : TweakifyModConfig.SUPPORTED_LANGUAGES) {
			Set<String> keys = keysOf(code);
			for (String key : List.of(
				"tweakify-mod.world_play_time.hours",
				"tweakify-mod.world_play_time.duration",
				"tweakify-mod.world_play_time.minutes")) {
				assertTrue(keys.contains(key), "missing " + key + " in " + code);
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

	// 选项枚举的显示名走 CodeOptionEntry.translationKey()，漏配时界面会退回 code
	//（例如 "one_block"）。这里按源码扫描自动发现所有实现，不维护清单。
	@Test
	void everyOptionListEntryTranslationKeyExistsInEveryLanguage() throws IOException {
		Map<String, Set<String>> keysByLanguage = new HashMap<>();
		for (String code : TweakifyModConfig.SUPPORTED_LANGUAGES) {
			keysByLanguage.put(code, keysOf(code));
		}
		Set<Class<?>> entryEnums = optionEntryEnums();
		assertFalse(entryEnums.isEmpty(), "必须能发现选项枚举，否则本测试形同虚设");
		for (Class<?> type : entryEnums) {
			for (Object constant : type.getEnumConstants()) {
				String key = ((CodeOptionEntry) constant).translationKey();
				// 返回 null 表示由实现类自行提供显示名（如语言自称），不走语言文件。
				if (key == null) {
					continue;
				}
				for (String code : TweakifyModConfig.SUPPORTED_LANGUAGES) {
					assertTrue(keysByLanguage.get(code).contains(key),
						"missing " + key + " in " + code + "（" + type.getSimpleName() + "）");
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

	// 与 mixin 清单测试同一思路：按源码目录发现实现类，新增枚举无需登记到任何清单。
	// 文件名与类名一致；只接受枚举，接口与普通类直接跳过。
	private static Set<Class<?>> optionEntryEnums() throws IOException {
		Set<Class<?>> types = new TreeSet<>(Comparator.comparing(Class::getName));
		try (Stream<Path> files = Files.list(OPTION_SOURCE_DIR)) {
			for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
				if (!IMPLEMENTS_OPTION_ENTRY.matcher(
					Files.readString(file, StandardCharsets.UTF_8)).find()) {
					continue;
				}
				String fileName = file.getFileName().toString();
				String className = fileName.substring(0, fileName.length() - ".java".length());
				Class<?> type;
				try {
					type = Class.forName(OPTION_PACKAGE + "." + className);
				} catch (ClassNotFoundException e) {
					throw new AssertionError("源码扫描到 " + className + "，但测试类路径里没有该类", e);
				}
				if (type.isEnum()) {
					types.add(type);
				}
			}
		}
		return types;
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
