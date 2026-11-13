package com.gtzc.tweakify.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 配置文件的落盘层：原子替换写入、旧文件名迁移、损坏文件备份。
 * 不参与字段清洗与缓存维护，这些仍由 {@link TweakifyModConfig} 负责。
 */
final class ConfigStorage {
	private static final Logger LOGGER = LoggerFactory.getLogger("tweakify-mod-config");

	private static final String CONFIG_FILE_NAME = "tweakify-mod.json";
	// 改名前的配置文件名：首次启动时自动搬过来，避免丢失已有设置。
	private static final String LEGACY_CONFIG_FILE_NAME = "fantasy-mod.json";

	static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private ConfigStorage() {
	}

	// 所有落盘共享一把写锁：后台写盘线程与退出时的同步兜底写可能并发，
	// 只有让「创建临时文件 + 原子替换」整体串行，后拿锁的那份内容才是最终磁盘状态。
	// 用 ReentrantLock 而不是 synchronized：退出兜底写可以 tryLock 限时，
	// 避免旧任务卡在不可中断的 I/O 段时让客户端无限期卡在退出界面。
	private static final ReentrantLock WRITE_LOCK = new ReentrantLock();

	// 提交序号：在任务提交时分配，而不是执行时分配。
	// 落盘前在写锁内比较每个文件已写成功的最新序号：序号更小的旧任务直接跳过，
	// 因此同步兜底写（最后分配序号）之后，任何延迟执行的旧任务都无法再覆盖最终快照。
	// key 为绝对规范化路径；生产上只有主配置与档案文件两个固定路径，表不会随档案数量增长。
	private static final AtomicLong WRITE_SEQUENCE = new AtomicLong();
	private static final Map<Path, Long> LAST_WRITTEN_SEQUENCE = new HashMap<>();

	static long nextWriteSequence() {
		return WRITE_SEQUENCE.incrementAndGet();
	}

	static Path defaultPath() {
		return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);
	}

	// 旧版配置文件名仍存在而新文件名缺失时，直接整体搬家。
	static void migrateLegacy(Path configPath) {
		if (Files.exists(configPath)) {
			return;
		}
		Path legacyPath = configPath.resolveSibling(LEGACY_CONFIG_FILE_NAME);
		if (!Files.exists(legacyPath)) {
			return;
		}
		try {
			Files.move(legacyPath, configPath);
			LOGGER.info("Migrated legacy config {} to {}", legacyPath, configPath);
		} catch (IOException e) {
			LOGGER.warn("Failed to migrate legacy config {}", legacyPath, e);
		}
	}

	// 同步调度：写盘任务由 TweakifyModConfig 提交到全局串行队列（见 AsyncFileWriter），
	// 这里只保留纯序列化与落盘，确保与后台写盘使用同一套原子替换逻辑。
	// 序列化以扁平字段表为唯一来源：字段拆进分区对象后，JSON 仍是「字段名 -> 值」的平铺结构，
	// 键名与拆分前完全一致。
	static String toJson(TweakifyModConfig config) {
		return GSON.toJson(toJsonTree(config));
	}

	// 序列化成 JSON 树：供配置档案保存整套配置（调用方需在配置锁内调用）。
	static JsonObject toJsonTree(TweakifyModConfig config) {
		JsonObject root = new JsonObject();
		for (Map.Entry<String, ConfigFieldRef> entry : TweakifyModConfig.fieldRefs().entrySet()) {
			Object value = entry.getValue().get(config);
			if (value == null) {
				// 与 Gson 默认行为一致：null 字段不写出，读取时保留字段初始化器的默认值。
				continue;
			}
			root.add(entry.getKey(), GSON.toJsonTree(value));
		}
		return root;
	}

	// 按扁平字段表逐项读取：缺键保留默认值，显式 null 原样写入（交给后续清洗），
	// 类型不匹配抛 JsonSyntaxException，与拆分前直接交给 Gson 反序列化的行为一致。
	static TweakifyModConfig fromJsonTree(JsonObject root) {
		TweakifyModConfig config = new TweakifyModConfig();
		for (Map.Entry<String, ConfigFieldRef> entry : TweakifyModConfig.fieldRefs().entrySet()) {
			if (!root.has(entry.getKey())) {
				continue;
			}
			// 用完整泛型类型而不是裸 List.class：后者会把元素退化为 Object，
			// 非字符串元素（如 [1, true]）不再被 Gson 强制转换为字符串，
			// 会在后续 sanitizeItemList 的 String 循环处抛 ClassCastException，
			// 导致整份配置被误判为损坏并回退默认值。
			Object value = GSON.fromJson(root.get(entry.getKey()), entry.getValue().genericType());
			if (value == null && entry.getValue().type().isPrimitive()) {
				// 与 Gson 反射绑定一致：原始类型字段收到 null 时保留默认值，而不是写入 null。
				// （实测 Gson 2.14：boolean/int/double 字段的 JSON null 不会覆盖初始化器结果。）
				continue;
			}
			entry.getValue().set(config, value);
		}
		return config;
	}

	// 只负责落盘：先写临时文件再原子替换，避免写到一半崩溃时留下半截 JSON。
	// 自动分配序号的同步写：仅供档案等“调用即执行”的同步路径（ConfigProfiles）使用；
	// 异步写盘任务必须在提交时用带序号的版本，否则执行期序号会破坏新旧顺序。
	static boolean writeAtomically(String json, Path configPath) {
		return writeAtomically(json, configPath, nextWriteSequence());
	}

	// 带提交序号的落盘：序号小于该文件已成功写盘的最新序号时直接跳过并返回 true
	//（语义：更晚提交的写已经覆盖了这份内容，不能把旧内容再写回去）。
	static boolean writeAtomically(String json, Path configPath, long sequence) {
		WRITE_LOCK.lock();
		try {
			return writeAtomicallyLocked(json, configPath, sequence);
		} finally {
			WRITE_LOCK.unlock();
		}
	}

	// 退出同步兜底专用：拿不到写锁时在超时后放弃，返回 null（区别于写盘失败 false）；
	// 此时排队任务仍在写，最终状态由它保证；宁可少写一次也不能让客户端无法退出。
	static Boolean tryWriteAtomically(String json, Path configPath, long sequence, long timeoutMillis) {
		boolean acquired;
		try {
			acquired = WRITE_LOCK.tryLock(timeoutMillis, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}
		if (!acquired) {
			LOGGER.warn("Timed out waiting for the config write lock; giving up the synchronous fallback");
			return null;
		}
		try {
			return writeAtomicallyLocked(json, configPath, sequence);
		} finally {
			WRITE_LOCK.unlock();
		}
	}

	// 包私有：测试验证 tryWriteAtomically 的锁超时语义。
	static ReentrantLock writeLockForTest() {
		return WRITE_LOCK;
	}

	private static boolean writeAtomicallyLocked(String json, Path configPath, long sequence) {
		// 路径校验必须在陈旧写比较之前：非法路径即使序号陈旧也必须报告失败，
		// 不能因为“还有更新的写”就把它当成成功。
		Path configDirectory = configPath.getParent();
		if (configDirectory == null) {
			// 没有父目录就无法建临时文件；直接返回失败而不是把 NPE 抛给后台线程。
			LOGGER.error("Config path has no parent directory: {}", configPath);
			return false;
		}
		Path key = configPath.toAbsolutePath().normalize();
		Long lastWritten = LAST_WRITTEN_SEQUENCE.get(key);
		if (lastWritten != null && sequence < lastWritten) {
			LOGGER.debug("Skipping stale config write for {} (sequence {} < {})",
				configPath, sequence, lastWritten);
			return true;
		}
		Path temporaryPath = null;
		boolean written = false;
		try {
			Files.createDirectories(configDirectory);
			temporaryPath = Files.createTempFile(configDirectory, "tweakify-mod-", ".tmp");
			Files.writeString(temporaryPath, json);
			try {
				Files.move(temporaryPath, configPath,
					StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temporaryPath, configPath, StandardCopyOption.REPLACE_EXISTING);
			}
			written = true;
			return true;
		} catch (IOException e) {
			LOGGER.error("Failed to write config", e);
			return false;
		} finally {
			if (written) {
				LAST_WRITTEN_SEQUENCE.put(key, sequence);
			}
			if (temporaryPath != null) {
				try {
					Files.deleteIfExists(temporaryPath);
				} catch (IOException e) {
					LOGGER.warn("Failed to delete temporary config file: {}", temporaryPath, e);
				}
			}
		}
	}

	// 损坏的文件移到带时间戳的备份文件，返回备份路径；失败返回 null（原文件保留）。
	// 主配置与档案共用同一套命名规则：<原名>.broken-<时间戳>。
	static Path backupBrokenTo(Path path) {
		Path backupPath = path.resolveSibling(
			path.getFileName() + ".broken-" + System.currentTimeMillis());
		try {
			Files.move(path, backupPath);
			LOGGER.warn("Broken file moved to {}", backupPath);
			return backupPath;
		} catch (IOException e) {
			LOGGER.error("Failed to back up broken file; the original file will not be overwritten", e);
			return null;
		}
	}

	// 只关心成功与否的调用方使用。
	static boolean backupBroken(Path configPath) {
		return backupBrokenTo(configPath) != null;
	}
}
