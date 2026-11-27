package com.gtzc.tweakify.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.gtzc.tweakify.util.AsyncFileWriter;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 收藏夹数据：配置项收藏与存档收藏共用一份独立的小文件。
 *
 * <p>收藏数据不进入主配置：主配置的字段表与界面选项一一对应，收藏集合是纯运行期 UI 状态，
 * 混进主配置会让“已修改/统计/档案快照”多出一批无意义的条目。
 *
 * <p>落盘复用 {@link ConfigStorage} 的原子替换与提交序号机制：
 * 序号在提交时分配，退出时的同步兜底写拿到更晚的序号，排队中的旧快照不会反向覆盖新内容。
 */
public final class FavoritesStorage {
	private static final Logger LOGGER = LoggerFactory.getLogger("tweakify-mod-favorites");

	public static final String FILE_NAME = "tweakify-mod-favorites.json";
	private static final String CONFIGS_KEY = "configs";
	private static final String WORLDS_KEY = "worlds";

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static final Set<String> CONFIG_FAVORITES = new LinkedHashSet<>();
	private static final Set<String> WORLD_FAVORITES = new LinkedHashSet<>();

	// 测试可注入的路径；生产环境走 config 目录。
	private static Path filePathOverride;
	private static boolean loaded;

	private FavoritesStorage() {
	}

	public static synchronized boolean isConfigFavorite(String configName) {
		ensureLoaded();
		return configName != null && CONFIG_FAVORITES.contains(configName);
	}

	// 切换收藏并返回切换后的状态；空名字不写盘，直接按未收藏处理。
	public static synchronized boolean toggleConfigFavorite(String configName) {
		ensureLoaded();
		if (configName == null || configName.isBlank()) {
			return false;
		}
		boolean favorite = !CONFIG_FAVORITES.remove(configName);
		if (favorite) {
			CONFIG_FAVORITES.add(configName);
		}
		scheduleSave();
		return favorite;
	}

	public static synchronized boolean isWorldFavorite(String levelId) {
		ensureLoaded();
		return levelId != null && WORLD_FAVORITES.contains(levelId);
	}

	public static synchronized boolean toggleWorldFavorite(String levelId) {
		ensureLoaded();
		if (levelId == null || levelId.isBlank()) {
			return false;
		}
		boolean favorite = !WORLD_FAVORITES.remove(levelId);
		if (favorite) {
			WORLD_FAVORITES.add(levelId);
		}
		scheduleSave();
		return favorite;
	}

	public static synchronized List<String> configFavorites() {
		ensureLoaded();
		return List.copyOf(CONFIG_FAVORITES);
	}

	public static synchronized List<String> worldFavorites() {
		ensureLoaded();
		return List.copyOf(WORLD_FAVORITES);
	}

	// 收敛世界收藏：只保留仍然存在的存档（外部删档/改名后清理悬空记录）。
	// 调用方只在读到有效的单人存档列表时调用，列表为空时不应清理以免误删。
	public static synchronized void retainWorldFavorites(Collection<String> levelIds) {
		ensureLoaded();
		Set<String> existing = new HashSet<>(levelIds);
		if (WORLD_FAVORITES.retainAll(existing)) {
			scheduleSave();
		}
	}

	// 只读快照：供收藏标签在遍历配置树时反复查询，避免外部修改内部集合。
	public static synchronized Set<String> configFavoriteSnapshot() {
		ensureLoaded();
		return Collections.unmodifiableSet(new LinkedHashSet<>(CONFIG_FAVORITES));
	}

	private static Path path() {
		return filePathOverride != null
			? filePathOverride
			: FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
	}

	private static void ensureLoaded() {
		if (loaded) {
			return;
		}
		loaded = true;
		loadFromDisk();
	}

	private static void loadFromDisk() {
		Path path = path();
		if (!Files.exists(path)) {
			return;
		}
		try {
			String json = Files.readString(path);
			JsonElement parsed = JsonParser.parseString(json);
			if (!parsed.isJsonObject()) {
				throw new JsonParseException("Favorites root must be a JSON object");
			}
			JsonObject root = parsed.getAsJsonObject();
			readSet(root.get(CONFIGS_KEY), CONFIG_FAVORITES);
			readSet(root.get(WORLDS_KEY), WORLD_FAVORITES);
			LOGGER.info("Loaded {} config favorites and {} world favorites",
				CONFIG_FAVORITES.size(), WORLD_FAVORITES.size());
		} catch (JsonParseException e) {
			// 内容损坏：备份后从空收藏重新开始，不让坏文件阻止游戏启动。
			LOGGER.error("Failed to parse favorites file {}, backing it up", path, e);
			CONFIG_FAVORITES.clear();
			WORLD_FAVORITES.clear();
			ConfigStorage.backupBroken(path);
		} catch (IOException e) {
			// 读取失败（权限/被占用）：保留原文件不备份，本次会话按空收藏处理。
			LOGGER.error("Failed to read favorites file {}", path, e);
			CONFIG_FAVORITES.clear();
			WORLD_FAVORITES.clear();
		} catch (RuntimeException e) {
			LOGGER.error("Failed to load favorites file {}, backing it up", path, e);
			CONFIG_FAVORITES.clear();
			WORLD_FAVORITES.clear();
			ConfigStorage.backupBroken(path);
		}
	}

	// 只接受字符串元素；其它类型（手工编辑写坏）静默跳过，不影响其余收藏。
	private static void readSet(JsonElement element, Set<String> target) {
		if (element == null || !element.isJsonArray()) {
			return;
		}
		for (JsonElement value : element.getAsJsonArray()) {
			if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
				continue;
			}
			String name = value.getAsString().trim();
			if (!name.isEmpty()) {
				target.add(name);
			}
		}
	}

	// 收藏切换频率很低：每次切换提交一份完整快照即可，不做额外节流。
	private static void scheduleSave() {
		String json = toJson();
		Path path = path();
		// 序号必须在提交时分配：排队中的旧快照不能覆盖退出兜底写的新内容。
		long sequence = ConfigStorage.nextWriteSequence();
		AsyncFileWriter.execute(() -> {
			if (!ConfigStorage.writeAtomically(json, path, sequence)) {
				LOGGER.warn("Failed to save favorites to {}", path);
			}
		});
	}

	// 包私有：JSON 组装与磁盘读写拆开，测试可直接校验序列化结果。
	static String toJson() {
		JsonObject root = new JsonObject();
		root.add(CONFIGS_KEY, toJsonArray(CONFIG_FAVORITES));
		root.add(WORLDS_KEY, toJsonArray(WORLD_FAVORITES));
		return GSON.toJson(root);
	}

	private static JsonArray toJsonArray(Set<String> values) {
		JsonArray array = new JsonArray();
		for (String value : values) {
			array.add(value);
		}
		return array;
	}

	// 客户端退出：直接同步写当前内存快照。
	// 写盘线程随后会被关闭，排队中的旧快照可能被丢弃，因此必须有一次同步兜底。
	// 用限时版本：写盘线程卡在慢盘/被杀软占用时不能让客户端无限期卡在退出界面。
	public static synchronized void flushOnShutdown() {
		ensureLoaded();
		Path path = path();
		long sequence = ConfigStorage.nextWriteSequence();
		Boolean written = ConfigStorage.tryWriteAtomically(toJson(), path, sequence, 2000L);
		if (!Boolean.TRUE.equals(written)) {
			LOGGER.warn("Failed or timed out writing favorites synchronously on shutdown: {}", path);
		}
	}

	// 客户端退出（非阻塞）：把当前快照提交到共享写盘队列后立即返回。
	// 由 AsyncFileWriter.shutdownAsync 的非 daemon 收尾线程在后台排空队列。
	public static synchronized void flushOnShutdownAsync() {
		ensureLoaded();
		Path path = path();
		String json = toJson();
		long sequence = ConfigStorage.nextWriteSequence();
		if (!AsyncFileWriter.execute(() -> {
			if (!ConfigStorage.writeAtomically(json, path, sequence)) {
				LOGGER.warn("Failed to save favorites on shutdown: {}", path);
			}
		})) {
			LOGGER.warn("File writer already shut down; favorites may not be saved on shutdown: {}", path);
		}
	}

	// 测试钩子（生产代码不得调用）：清空内存并指向临时文件；首次查询时从该文件加载。
	public static synchronized void resetForTest(Path path) {
		CONFIG_FAVORITES.clear();
		WORLD_FAVORITES.clear();
		loaded = false;
		filePathOverride = path;
	}

	// 测试钩子（生产代码不得调用）：清空内存并恢复生产路径。
	public static synchronized void clearForTest() {
		CONFIG_FAVORITES.clear();
		WORLD_FAVORITES.clear();
		loaded = false;
		filePathOverride = null;
	}
}
