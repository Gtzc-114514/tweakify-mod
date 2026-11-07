package com.gtzc.tweakify.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 配置档案：把当前整套配置存成命名档案，需要时一键切换。
 *
 * <p>档案独立存放在 {@code config/tweakify-mod-profiles.json}：
 * 主配置表示「当前状态」，档案表示「备选状态」。两者语义分离，
 * 主配置的体积也不会随档案数量膨胀。
 *
 * <p>加载时只覆盖档案里实际存在的字段：模组升级新增的字段保持当前值，
 * 不会被旧档案重置成默认值；档案里已废弃的字段名会被静默跳过。
 *
 * <p>失败语义分三种（{@link Result}）：档案不存在、档案文件损坏、写盘失败，
 * 便于命令层给出可操作的提示；文件损坏时 {@link #save} 会先备份再重建，
 * 不会静默覆盖掉已有的全部档案。
 */
public final class ConfigProfiles {
	private static final Logger LOGGER = LoggerFactory.getLogger("tweakify-mod-config");
	private static final String FILE_NAME = "tweakify-mod-profiles.json";
	private static final String BACKUP_SUFFIX = ".broken-";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Type FILE_TYPE = new TypeToken<Map<String, JsonObject>>() {
	}.getType();

	// 档案文件只有一个：读-改-写整体串行化，避免并发的 save/delete 互相吞掉更新。
	// 这把锁只保护文件内容，不参与 TweakifyModConfig 的 monitor，因此不会与之形成锁环。
	private static final Object FILE_LOCK = new Object();

	private ConfigProfiles() {
	}

	// 档案操作结果：区分「不存在 / 文件损坏 / 写盘失败」三种失败语义。
	public enum Result {
		OK,
		// 档案不存在（或参数非法）。
		NOT_FOUND,
		// 档案文件损坏，且无法安全处理（备份失败时不覆盖原文件）。
		BROKEN,
		// 文件写入失败。
		WRITE_FAILED
	}

	// 已解析的档案：配置对象 + 档案里实际存在的字段名。
	public record Loaded(TweakifyModConfig config, Set<String> fieldNames) {
	}

	// 读取结果：profile 为 null 时用 status 区分「不存在」与「损坏」。
	public record ReadResult(Loaded profile, Result status) {
		public boolean ok() {
			return profile != null;
		}
	}

	// 已读取的档案文件：
	// corrupt = 内容无法解析，可以备份后重建；
	// readFailed = 读取本身失败（文件被独占、权限不足等），不能当作损坏处理。
	private record ProfilesFile(Map<String, JsonObject> profiles, boolean corrupt, boolean readFailed) {
		static ProfilesFile empty() {
			return new ProfilesFile(new LinkedHashMap<>(), false, false);
		}

		static ProfilesFile corrupted() {
			return new ProfilesFile(new LinkedHashMap<>(), true, false);
		}

		static ProfilesFile unreadable() {
			return new ProfilesFile(new LinkedHashMap<>(), false, true);
		}
	}

	// 档案名列表（按写入顺序）。文件缺失或损坏时返回空表，不影响游戏。
	public static List<String> names() {
		return names(storagePath());
	}

	// 读取并解析档案。
	public static ReadResult read(String name) {
		return read(storagePath(), name);
	}

	// 把当前配置整套保存为档案（同名覆盖）。
	public static Result save(String name, TweakifyModConfig config) {
		return save(storagePath(), name, config);
	}

	// 删除档案。
	public static Result delete(String name) {
		return delete(storagePath(), name);
	}

	// --- 包私有入口：测试注入临时路径，避免依赖 FabricLoader 环境 ---

	static List<String> names(Path path) {
		return List.copyOf(readAll(path).profiles().keySet());
	}

	static ReadResult read(Path path, String name) {
		if (name == null || name.isBlank()) {
			return new ReadResult(null, Result.NOT_FOUND);
		}
		ProfilesFile file = readAll(path);
		if (file.readFailed()) {
			// 读失败：既不能确认档案存在，也不能断言它损坏。
			return new ReadResult(null, Result.WRITE_FAILED);
		}
		if (file.corrupt()) {
			return new ReadResult(null, Result.BROKEN);
		}
		JsonObject json = file.profiles().get(name);
		if (json == null) {
			return new ReadResult(null, Result.NOT_FOUND);
		}
		try {
			TweakifyModConfig config = GSON.fromJson(json, TweakifyModConfig.class);
			if (config == null) {
				return new ReadResult(null, Result.BROKEN);
			}
			return new ReadResult(new Loaded(config, Set.copyOf(json.keySet())), Result.OK);
		} catch (RuntimeException e) {
			// 单份档案损坏只影响这一份，不能连带其它档案一起失效。
			LOGGER.error("Failed to parse config profile: {}", name, e);
			return new ReadResult(null, Result.BROKEN);
		}
	}

	static Result save(Path path, String name, TweakifyModConfig config) {
		if (name == null || name.isBlank() || config == null) {
			return Result.NOT_FOUND;
		}
		// 序列化在配置锁内完成（toJsonTree 是 synchronized）：与客户端线程的写共用同一把锁，
		// 避免拖滑块时存下“半新半旧”的档案（例如成对字段只更新了一半）。
		// 放在 FILE_LOCK 之外，避免两把锁嵌套形成潜在死锁。
		JsonObject profile = config.toJsonTree();
		synchronized (FILE_LOCK) {
			ProfilesFile file = readAll(path);
			if (file.readFailed()) {
				// 读不出来时既不能备份也不能重建：直接报写盘失败，绝不覆盖原文件。
				return Result.WRITE_FAILED;
			}
			if (file.corrupt()) {
				// 损坏的档案文件不直接覆盖：先备份再重建，避免一次性丢掉已有的全部档案。
				if (!backupBroken(path)) {
					return Result.BROKEN;
				}
				file = ProfilesFile.empty();
			}
			Map<String, JsonObject> profiles = file.profiles();
			profiles.put(name, profile);
			return writeAll(path, profiles) ? Result.OK : Result.WRITE_FAILED;
		}
	}

	static Result delete(Path path, String name) {
		if (name == null || name.isBlank()) {
			return Result.NOT_FOUND;
		}
		synchronized (FILE_LOCK) {
			ProfilesFile file = readAll(path);
			if (file.readFailed()) {
				return Result.WRITE_FAILED;
			}
			if (file.corrupt()) {
				// 文件损坏时既不删也不覆盖：留给 save 走「备份后重建」的流程。
				return Result.BROKEN;
			}
			if (file.profiles().remove(name) == null) {
				return Result.NOT_FOUND;
			}
			return writeAll(path, file.profiles()) ? Result.OK : Result.WRITE_FAILED;
		}
	}

	private static ProfilesFile readAll(Path path) {
		if (!Files.exists(path)) {
			return ProfilesFile.empty();
		}
		String json;
		try {
			json = Files.readString(path);
		} catch (IOException e) {
			// 读失败不等于内容损坏：不能把内容完好的文件当成损坏搬走并清空。
			LOGGER.error("Failed to read config profiles", e);
			return ProfilesFile.unreadable();
		}
		try {
			JsonElement parsed = JsonParser.parseString(json);
			if (!parsed.isJsonObject()) {
				throw new JsonParseException("Profiles root must be a JSON object");
			}
			Map<String, JsonObject> profiles = GSON.fromJson(parsed, FILE_TYPE);
			return new ProfilesFile(
				profiles == null ? new LinkedHashMap<>() : new LinkedHashMap<>(profiles), false, false);
		} catch (JsonParseException | IllegalStateException e) {
			LOGGER.error("Failed to parse config profiles, treating as broken", e);
			return ProfilesFile.corrupted();
		}
	}

	private static boolean writeAll(Path path, Map<String, JsonObject> profiles) {
		return ConfigStorage.writeAtomically(GSON.toJson(profiles, FILE_TYPE), path);
	}

	// 损坏的档案移到带时间戳的备份文件；失败时不覆盖原文件。
	private static boolean backupBroken(Path path) {
		Path backupPath = path.resolveSibling(path.getFileName() + BACKUP_SUFFIX + System.currentTimeMillis());
		try {
			Files.move(path, backupPath);
			LOGGER.warn("Broken config profiles moved to {}", backupPath);
			return true;
		} catch (IOException e) {
			LOGGER.error("Failed to back up broken config profiles; the original file will not be overwritten", e);
			return false;
		}
	}

	private static Path storagePath() {
		return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
	}
}
