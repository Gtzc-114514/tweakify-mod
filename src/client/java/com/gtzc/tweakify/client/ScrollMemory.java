package com.gtzc.tweakify.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.AsyncFileWriter;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/**
 * 滚动列表记忆：记住列表滚到了哪里，关闭界面再打开时回到该位置。
 *
 * <p>默认只在本次游戏内有效；开启“跨重启记忆”后写入
 * {@code config/tweakify-mod-scroll-memory.json}，下次启动仍然生效。
 *
 * <p>写盘不在每次记录时进行（Mod Menu 渲染每帧都会记录一次）：
 * 只标脏，由界面关闭或 {@link #tick()} 定时刷出，避免每帧同步磁盘 IO。
 */
public final class ScrollMemory {
	private static final String MOD_MENU_KEY = "modmenu";
	private static final String MALILIB_PREFIX = "malilib:";
	private static final String FILE_NAME = "tweakify-mod-scroll-memory.json";
	// 兜底刷盘间隔：10 秒（20 tick/秒）。正常退出走界面关闭的立即刷出。
	private static final int FLUSH_INTERVAL_TICKS = 200;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Type FILE_TYPE = new TypeToken<Map<String, Integer>>() {
	}.getType();

	private static final Map<String, Integer> POSITIONS = new HashMap<>();
	private static boolean loaded;
	// 写盘在后台线程进行，失败时由该线程恢复标记，因此需要 volatile。
	private static volatile boolean dirty;
	private static int flushTicks;
	private static Path cachedPath;

	private ScrollMemory() {
	}

	private static TweakifyModConfig config() {
		return TweakifyMod.CONFIG;
	}

	private static boolean persistEnabled() {
		TweakifyModConfig config = config();
		return config != null && config.scrollListMemoryPersist;
	}

	public static boolean isMaLiLibEnabled() {
		TweakifyModConfig config = config();
		return config != null && config.scrollListMemory && config.malilibScrollMemory;
	}

	public static boolean isModMenuEnabled() {
		TweakifyModConfig config = config();
		return config != null && config.scrollListMemory && config.modMenuScrollMemory;
	}

	// 配置界面每次打开都是新实例，因此用界面类作键。
	public static Integer getMaLiLib(Object screen) {
		return screen == null ? null : get(MALILIB_PREFIX + screen.getClass().getName());
	}

	public static void putMaLiLib(Object screen, int scroll) {
		if (screen != null) {
			put(MALILIB_PREFIX + screen.getClass().getName(), scroll);
		}
	}

	public static Integer getModMenu() {
		return get(MOD_MENU_KEY);
	}

	public static void putModMenu(int scroll) {
		put(MOD_MENU_KEY, scroll);
	}

	private static Integer get(String key) {
		ensureLoaded();
		return POSITIONS.get(key);
	}

	private static void put(String key, int value) {
		ensureLoaded();
		Integer previous = POSITIONS.put(key, value);
		// 位置未变时不标脏：Mod Menu 每帧都会调用到这里。
		if (previous == null || previous != value) {
			dirty = true;
		}
	}

	// 界面关闭时调用：只在开启跨重启记忆且确有变化时写盘。
	public static void flush() {
		if (!dirty || !persistEnabled()) {
			return;
		}
		// 先合并磁盘上已有的记录：本次会话可能是在开关打开之前就开始记录的。
		ensureLoaded();
		submitAsyncFlush(storagePath());
	}

	// 写盘搬到后台线程：滚动位置可能在界面渲染路径上变化，不能同步占主线程做磁盘 IO。
	// 传出去的是不可变快照，后台线程不再触碰 POSITIONS。
	private static void submitAsyncFlush(Path path) {
		Map<String, Integer> snapshot = Map.copyOf(POSITIONS);
		dirty = false;
		AsyncFileWriter.execute(() -> {
			if (!saveSnapshotTo(path, snapshot)) {
				// 失败保留待写标记：客户端 tick 或下一次界面关闭时重试。
				dirty = true;
			}
		});
	}

	// 包私有：同步写盘，供测试使用；写盘成功才清 dirty。
	static boolean flushTo(Path path) {
		if (!saveSnapshotTo(path, Map.copyOf(POSITIONS))) {
			return false;
		}
		dirty = false;
		return true;
	}

	// 客户端退出：先排空后台队列，再同步写最终位置。
	// 顺序不能反：队列里可能还排着更早的快照，它会在 shutdown() 时落盘并覆盖新内容。
	public static void flushOnShutdown() {
		AsyncFileWriter.drain(2000L);
		if (!dirty || !persistEnabled()) {
			return;
		}
		ensureLoaded();
		flushTo(storagePath());
	}

	// 包私有：把给定快照写成 JSON，先写临时文件再原子替换。
	// 快照不可变，因此可以在后台线程调用。
	static boolean saveSnapshotTo(Path path, Map<String, Integer> snapshot) {
		Path temporaryPath = null;
		try {
			Path directory = path.toAbsolutePath().getParent();
			if (directory == null) {
				return false;
			}
			Files.createDirectories(directory);
			temporaryPath = Files.createTempFile(directory, "tweakify-scroll-", ".tmp");
			Files.writeString(temporaryPath, GSON.toJson(snapshot, FILE_TYPE));
			try {
				Files.move(temporaryPath, path,
					StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temporaryPath, path, StandardCopyOption.REPLACE_EXISTING);
			}
			return true;
		} catch (IOException e) {
			TweakifyMod.LOGGER.warn("Failed to write scroll memory: {}", e.getMessage());
			return false;
		} finally {
			if (temporaryPath != null) {
				try {
					Files.deleteIfExists(temporaryPath);
				} catch (IOException ignored) {
					// 临时文件删除失败不影响本次写盘结果。
				}
			}
		}
	}

	// 客户端 tick 调用：定时兜底刷出，防止异常退出丢掉本次位置。
	public static void tick() {
		if (!dirty) {
			flushTicks = 0;
			return;
		}
		if (++flushTicks >= FLUSH_INTERVAL_TICKS) {
			flushTicks = 0;
			flush();
		}
	}

	// 仅在开启跨重启记忆时读取文件：文件缺失或损坏都退回空表，不影响界面。
	private static void ensureLoaded() {
		if (loaded || !persistEnabled()) {
			return;
		}
		loaded = true;
		Path path = storagePath();
		if (!Files.exists(path)) {
			return;
		}
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			Map<String, Integer> values = GSON.fromJson(reader, FILE_TYPE);
			if (values != null) {
				POSITIONS.putAll(values);
			}
		} catch (IOException | JsonParseException e) {
			TweakifyMod.LOGGER.warn("Failed to read scroll memory: {}", e.getMessage());
		}
	}

	private static Path storagePath() {
		Path path = cachedPath;
		if (path == null) {
			path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
			cachedPath = path;
		}
		return path;
	}

	// 包私有：供测试确认写盘失败后仍保留待写标记。
	static boolean isDirty() {
		return dirty;
	}

	// 包私有：测试之间重置内存状态，避免共享静态字段互相影响。
	static void resetForTest() {
		POSITIONS.clear();
		loaded = false;
		dirty = false;
		flushTicks = 0;
	}
}
