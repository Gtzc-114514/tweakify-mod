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

	// 序列化：纯 CPU 操作，由调用方在自己的线程完成（配置字段由调用方的锁保护）。
	static String toJson(TweakifyModConfig config) {
		return GSON.toJson(config);
	}

	// 序列化成 JSON 树：供配置档案保存整套配置（调用方需在配置锁内调用）。
	static JsonObject toJsonTree(TweakifyModConfig config) {
		return GSON.toJsonTree(config).getAsJsonObject();
	}

	// 同步写盘：序列化 + 原子替换。低频路径（启动期写默认值、命令改值）使用。
	static boolean save(TweakifyModConfig config, Path configPath) {
		return writeAtomically(toJson(config), configPath);
	}

	// 只负责落盘：先写临时文件再原子替换，避免写到一半崩溃时留下半截 JSON。
	// 不触碰配置对象，可在后台线程调用。
	static boolean writeAtomically(String json, Path configPath) {
		Path temporaryPath = null;
		try {
			Path configDirectory = configPath.getParent();
			if (configDirectory == null) {
				// 没有父目录就无法建临时文件；直接返回失败而不是把 NPE 抛给后台线程。
				LOGGER.error("Config path has no parent directory: {}", configPath);
				return false;
			}
			Files.createDirectories(configDirectory);
			temporaryPath = Files.createTempFile(configDirectory, "tweakify-mod-", ".tmp");
			Files.writeString(temporaryPath, json);
			try {
				Files.move(temporaryPath, configPath,
					StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temporaryPath, configPath, StandardCopyOption.REPLACE_EXISTING);
			}
			return true;
		} catch (IOException e) {
			LOGGER.error("Failed to write config", e);
			return false;
		} finally {
			if (temporaryPath != null) {
				try {
					Files.deleteIfExists(temporaryPath);
				} catch (IOException e) {
					LOGGER.warn("Failed to delete temporary config file: {}", temporaryPath, e);
				}
			}
		}
	}

	// 损坏的配置移到带时间戳的备份文件，返回是否备份成功（失败时不覆盖原文件）。
	static boolean backupBroken(Path configPath) {
		Path backupPath = configPath.resolveSibling(
			configPath.getFileName() + ".broken-" + System.currentTimeMillis());
		try {
			Files.move(configPath, backupPath);
			LOGGER.warn("Broken config moved to {}", backupPath);
			return true;
		} catch (IOException e) {
			LOGGER.error("Failed to back up broken config; the original file will not be overwritten", e);
			return false;
		}
	}
}
