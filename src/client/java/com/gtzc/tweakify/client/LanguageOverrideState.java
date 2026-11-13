package com.gtzc.tweakify.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 模组界面语言的覆盖表：跟随系统语言开启时完全不介入（使用游戏当前语言）；
 * 关闭时按配置语言从内置语言文件读取文本，只覆盖 tweakify-mod.* 键，
 * 缺失的键回退到游戏语言，避免影响其他模组与原版文本。
 */
public final class LanguageOverrideState {
	private static final Logger LOGGER = LoggerFactory.getLogger(TweakifyMod.MOD_ID + "-language");
	private static final Gson GSON = new Gson();
	private static final Type TABLE_TYPE = new TypeToken<Map<String, String>>() { }.getType();
	private static final String KEY_PREFIX = TweakifyMod.MOD_ID + ".";

	private static volatile Map<String, Map<String, String>> translations;

	// 当前语言表的缓存：翻译查询每帧成千上万次（Mixin 挂在所有一参 getOrDefault 上），
	// 热路径上不能每次都走 synchronized 或重复取表。
	// 语言与表合成一条不可变记录一起发布，避免读到“新语言 + 旧表”的组合。
	private static volatile LangTable cachedTable;

	private record LangTable(String language, Map<String, String> table) {
	}

	private LanguageOverrideState() {
	}

	// 返回覆盖文本；null 表示不覆盖，调用方继续使用原版翻译。
	public static String translate(String key) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || config.general.followSystemLanguage || key == null || !key.startsWith(KEY_PREFIX)) {
			return null;
		}
		String language = config.general.language;
		LangTable cached = cachedTable;
		if (cached == null || !language.equals(cached.language())) {
			cached = new LangTable(language, translations().get(language));
			cachedTable = cached;
		}
		Map<String, String> table = cached.table();
		return table == null ? null : table.get(key);
	}

	// 无锁读取已加载的语言表；首次查询时才真正读文件（双检锁）。
	private static Map<String, Map<String, String>> translations() {
		Map<String, Map<String, String>> loaded = translations;
		if (loaded == null) {
			loaded = loadAll();
			translations = loaded;
		}
		return loaded;
	}

	private static synchronized Map<String, Map<String, String>> loadAll() {
		Map<String, Map<String, String>> loaded = translations;
		if (loaded != null) {
			return loaded;
		}
		loaded = new HashMap<>();
		for (String code : TweakifyModConfig.SUPPORTED_LANGUAGES) {
			Map<String, String> table = loadTable(code);
			if (table != null) {
				loaded.put(code, table);
			}
		}
		return loaded;
	}

	private static Map<String, String> loadTable(String code) {
		String path = "/assets/" + TweakifyMod.MOD_ID + "/lang/" + code + ".json";
		try (InputStream stream = LanguageOverrideState.class.getResourceAsStream(path)) {
			if (stream == null) {
				LOGGER.warn("Missing language file {}", path);
				return null;
			}
			Map<String, String> table = GSON.fromJson(
				new InputStreamReader(stream, StandardCharsets.UTF_8), TABLE_TYPE);
			return table == null ? null : Map.copyOf(table);
		} catch (Exception e) {
			LOGGER.warn("Failed to load language file {}", path, e);
			return null;
		}
	}
}
