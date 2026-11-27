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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 进度界面的纯 UI 状态：上次选中的标签与右侧固定的标签。
 *
 * <p>与收藏夹一样不进入主配置：这些是运行期界面状态，混进主配置会让“已修改/统计”
 * 多出一批无意义条目，也会让档案快照包含临时选择。
 */
public final class AdvancementsStateStorage {
	private static final Logger LOGGER = LoggerFactory.getLogger("tweakify-mod-advancements");

	public static final String FILE_NAME = "tweakify-mod-advancements.json";
	private static final String PINNED_TABS_KEY = "pinnedTabs";
	private static final String LAST_SELECTED_TAB_KEY = "lastSelectedTab";

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static final Set<String> PINNED_TABS = new LinkedHashSet<>();
	private static String lastSelectedTab = "";

	// 测试可注入的路径；生产环境走 config 目录。
	private static Path filePathOverride;
	private static boolean loaded;

	private AdvancementsStateStorage() {
	}

	public static synchronized List<String> pinnedTabs() {
		ensureLoaded();
		return List.copyOf(PINNED_TABS);
	}

	public static synchronized boolean isPinned(String advancementId) {
		ensureLoaded();
		return advancementId != null && PINNED_TABS.contains(advancementId);
	}

	// 切换固定状态并返回切换后的状态；空 ID 不写盘。
	public static synchronized boolean togglePinned(String advancementId) {
		ensureLoaded();
		if (advancementId == null || advancementId.isBlank()) {
			return false;
		}
		boolean pinned = !PINNED_TABS.remove(advancementId);
		if (pinned) {
			PINNED_TABS.add(advancementId);
		}
		scheduleSave();
		return pinned;
	}

	public static synchronized int pinIndex(String advancementId) {
		ensureLoaded();
		return advancementId == null ? -1 : List.copyOf(PINNED_TABS).indexOf(advancementId);
	}

	public static synchronized String lastSelectedTab() {
		ensureLoaded();
		return lastSelectedTab;
	}

	public static synchronized void setLastSelectedTab(String advancementId) {
		ensureLoaded();
		String value = advancementId == null ? "" : advancementId;
		if (value.equals(lastSelectedTab)) {
			return;
		}
		lastSelectedTab = value;
		scheduleSave();
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
				throw new JsonParseException("Advancement state root must be a JSON object");
			}
			JsonObject root = parsed.getAsJsonObject();
			readSet(root.get(PINNED_TABS_KEY), PINNED_TABS);
			JsonElement last = root.get(LAST_SELECTED_TAB_KEY);
			if (last != null && last.isJsonPrimitive() && last.getAsJsonPrimitive().isString()) {
				lastSelectedTab = last.getAsString().trim();
			}
			LOGGER.info("Loaded {} pinned advancement tabs", PINNED_TABS.size());
		} catch (JsonParseException e) {
			// 内容损坏：备份后从空状态重新开始，不让坏文件阻止游戏启动。
			LOGGER.error("Failed to parse advancement state file {}, backing it up", path, e);
			PINNED_TABS.clear();
			lastSelectedTab = "";
			ConfigStorage.backupBroken(path);
		} catch (IOException e) {
			LOGGER.error("Failed to read advancement state file {}", path, e);
			PINNED_TABS.clear();
			lastSelectedTab = "";
		} catch (RuntimeException e) {
			LOGGER.error("Failed to load advancement state file {}, backing it up", path, e);
			PINNED_TABS.clear();
			lastSelectedTab = "";
			ConfigStorage.backupBroken(path);
		}
	}

	private static void readSet(JsonElement element, Set<String> target) {
		if (element == null || !element.isJsonArray()) {
			return;
		}
		for (JsonElement value : element.getAsJsonArray()) {
			if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
				continue;
			}
			String id = value.getAsString().trim();
			if (!id.isEmpty()) {
				target.add(id);
			}
		}
	}

	// 切换频率很低：每次提交一份完整快照；序号在提交时分配，排队中的旧快照不会覆盖新内容。
	private static void scheduleSave() {
		String json = toJson();
		Path path = path();
		long sequence = ConfigStorage.nextWriteSequence();
		AsyncFileWriter.execute(() -> {
			if (!ConfigStorage.writeAtomically(json, path, sequence)) {
				LOGGER.warn("Failed to save advancement state to {}", path);
			}
		});
	}

	// 包私有：JSON 组装与磁盘读写拆开，测试可直接校验序列化结果。
	static String toJson() {
		JsonObject root = new JsonObject();
		JsonArray pinned = new JsonArray();
		for (String id : PINNED_TABS) {
			pinned.add(id);
		}
		root.add(PINNED_TABS_KEY, pinned);
		root.addProperty(LAST_SELECTED_TAB_KEY, lastSelectedTab);
		return GSON.toJson(root);
	}

	// 客户端退出：直接同步写当前内存快照，避免排队的旧快照在写盘线程关闭时被丢弃。
	public static synchronized void flushOnShutdown() {
		ensureLoaded();
		Path path = path();
		long sequence = ConfigStorage.nextWriteSequence();
		Boolean written = ConfigStorage.tryWriteAtomically(toJson(), path, sequence, 2000L);
		if (!Boolean.TRUE.equals(written)) {
			LOGGER.warn("Failed or timed out writing advancement state synchronously on shutdown: {}", path);
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
				LOGGER.warn("Failed to save advancement state on shutdown: {}", path);
			}
		})) {
			LOGGER.warn("File writer already shut down; advancement state may not be saved on shutdown: {}", path);
		}
	}

	// 包私有：测试之间重置内存状态与路径。
	static synchronized void resetForTest(Path override) {
		PINNED_TABS.clear();
		lastSelectedTab = "";
		filePathOverride = override;
		loaded = false;
	}
}
