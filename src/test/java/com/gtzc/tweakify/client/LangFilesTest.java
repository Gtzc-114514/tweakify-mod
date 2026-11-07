package com.gtzc.tweakify.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gtzc.tweakify.config.ConfigCatalog;
import com.gtzc.tweakify.config.TweakifyModConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 语言文件必须保持相同键集：缺失键会静默回退到游戏语言，游戏内很难发现漏翻。
 */
class LangFilesTest {
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
					String key = "tweakify-mod.config.general." + kind + "." + optionName;
					assertTrue(keys.contains(key), "missing " + key + " in " + code);
				}
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
				String key = "tweakify-mod.config.general." + kind
					+ ".disableResourcePackCompatibilityCheck";
				assertTrue(keys.contains(key), "missing " + key + " in " + code);
			}
		}
	}

	private static Set<String> keysOf(String code) throws IOException {
		String path = "/assets/tweakify-mod/lang/" + code + ".json";
		try (InputStream stream = LangFilesTest.class.getResourceAsStream(path)) {
			assertNotNull(stream, "missing resource " + path);
			JsonObject json = JsonParser.parseReader(
				new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
			return json.keySet();
		}
	}
}
