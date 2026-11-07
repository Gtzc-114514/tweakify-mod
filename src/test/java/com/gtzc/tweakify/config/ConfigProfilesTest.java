package com.gtzc.tweakify.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 覆盖配置档案的读写、失败语义与损坏文件处理。
 * 使用包私有的路径重载，避免依赖 FabricLoader 环境。
 */
class ConfigProfilesTest {
	@TempDir
	Path tempDir;

	private Path profilesPath() {
		return tempDir.resolve("tweakify-mod-profiles.json");
	}

	private TweakifyModConfig newConfig() {
		return new TweakifyModConfig(tempDir.resolve("tweakify-mod.json"));
	}

	// --- 基本读写 ---

	@Test
	void saveAndReadRoundTrip() {
		Path path = profilesPath();
		TweakifyModConfig config = newConfig();
		config.miningCooldown = true;
		config.disableHurtShake = true;

		assertEquals(ConfigProfiles.Result.OK, ConfigProfiles.save(path, "PvP", config));
		assertEquals(List.of("PvP"), ConfigProfiles.names(path).names());

		ConfigProfiles.ReadResult read = ConfigProfiles.read(path, "PvP");
		assertTrue(read.ok());
		assertEquals(ConfigProfiles.Result.OK, read.status());
		assertTrue(read.profile().config().miningCooldown);
		assertTrue(read.profile().config().disableHurtShake);
		assertTrue(read.profile().fieldNames().contains("miningCooldown"));
		// 版本号会被记录在档案里，但 applyFrom 会跳过它
		assertTrue(read.profile().fieldNames().contains("configVersion"));
	}

	@Test
	void saveOverwritesExistingProfile() {
		Path path = profilesPath();
		TweakifyModConfig first = newConfig();
		first.miningCooldown = true;
		assertEquals(ConfigProfiles.Result.OK, ConfigProfiles.save(path, "P", first));

		TweakifyModConfig second = newConfig();
		second.miningCooldown = false;
		assertEquals(ConfigProfiles.Result.OK, ConfigProfiles.save(path, "P", second));

		assertEquals(List.of("P"), ConfigProfiles.names(path).names());
		assertFalse(ConfigProfiles.read(path, "P").profile().config().miningCooldown);
	}

	@Test
	void deleteRemovesOnlyThatProfile() {
		Path path = profilesPath();
		assertEquals(ConfigProfiles.Result.OK, ConfigProfiles.save(path, "A", newConfig()));
		assertEquals(ConfigProfiles.Result.OK, ConfigProfiles.save(path, "B", newConfig()));

		assertEquals(ConfigProfiles.Result.OK, ConfigProfiles.delete(path, "A"));
		assertEquals(List.of("B"), ConfigProfiles.names(path).names());
		assertEquals(ConfigProfiles.Result.NOT_FOUND, ConfigProfiles.read(path, "A").status());
		assertEquals(ConfigProfiles.Result.NOT_FOUND, ConfigProfiles.delete(path, "A"),
			"deleting a missing profile must report NOT_FOUND");
	}

	// --- 失败语义 ---

	@Test
	void missingFileReportsNotFound() {
		Path path = profilesPath();
		assertTrue(ConfigProfiles.names(path).names().isEmpty());
		assertEquals(ConfigProfiles.Result.NOT_FOUND, ConfigProfiles.read(path, "PvP").status());
	}

	// 文件损坏：读取与删除都报 BROKEN，而不是误报“不存在”。
	@Test
	void brokenFileReportsBroken() throws IOException {
		Path path = profilesPath();
		Files.writeString(path, "{ this is not json");

		assertEquals(ConfigProfiles.Result.BROKEN, ConfigProfiles.read(path, "PvP").status());
		assertEquals(ConfigProfiles.Result.BROKEN, ConfigProfiles.delete(path, "PvP"));
		ConfigProfiles.NamesResult names = ConfigProfiles.names(path);
		assertTrue(names.names().isEmpty());
		assertEquals(ConfigProfiles.Result.BROKEN, names.status(),
			"broken file must be reported, not treated as an empty list");
	}

	// 根节点不是对象（例如数组）同样按损坏处理。
	@Test
	void nonObjectRootReportsBroken() throws IOException {
		Path path = profilesPath();
		Files.writeString(path, "[]");

		assertEquals(ConfigProfiles.Result.BROKEN, ConfigProfiles.read(path, "PvP").status());
	}

	// 档案值是非法 JSON 类型时（例如 {"bad": 42}），该条目被忽略：
	// list 不应列出它，load 也不应因为它存在就报“文件损坏”。
	@Test
	void nonObjectProfileEntryIsIgnored() throws IOException {
		Path path = profilesPath();
		Files.writeString(path, """
			{
			  "good": {"configVersion": 2},
			  "bad": 42
			}
			""");

		ConfigProfiles.NamesResult names = ConfigProfiles.names(path);
		assertEquals(List.of("good"), names.names(), "non-object entry must not be listed");
		assertEquals(ConfigProfiles.Result.OK, names.status());

		assertEquals(ConfigProfiles.Result.OK, ConfigProfiles.read(path, "good").status());
		assertEquals(ConfigProfiles.Result.NOT_FOUND, ConfigProfiles.read(path, "bad").status(),
			"non-object entry must be reported as not found, not broken");
	}

	// 损坏文件在 save 覆盖前先备份：不能一次性丢掉已有的全部档案。
	@Test
	void saveBacksUpBrokenFileBeforeOverwriting() throws IOException {
		Path path = profilesPath();
		Files.writeString(path, "{ broken");

		assertEquals(ConfigProfiles.Result.OK, ConfigProfiles.save(path, "PvP", newConfig()));

		// 原文件已被重建为合法档案
		assertEquals(List.of("PvP"), ConfigProfiles.names(path).names());
		// 损坏内容被移到带时间戳的备份文件
		try (Stream<Path> files = Files.list(tempDir)) {
			List<Path> backups = files
				.filter(p -> p.getFileName().toString().startsWith("tweakify-mod-profiles.json.broken-"))
				.toList();
			assertEquals(1, backups.size(), "broken profile file should be backed up exactly once");
			assertEquals("{ broken", Files.readString(backups.get(0)));
		}
	}

	// 读取失败（IO 异常）不能被当成“内容损坏”：否则 save 会把完好档案搬走并清空。
	// 这里用“路径是目录”构造读取失败（Files.readString 对目录抛 IOException）。
	@Test
	void unreadableFileIsNotTreatedAsCorrupt() throws IOException {
		Path dir = tempDir.resolve("profiles-is-a-directory");
		Files.createDirectories(dir);

		assertEquals(ConfigProfiles.Result.WRITE_FAILED, ConfigProfiles.read(dir, "PvP").status());
		assertEquals(ConfigProfiles.Result.WRITE_FAILED, ConfigProfiles.delete(dir, "PvP"));
		assertEquals(ConfigProfiles.Result.WRITE_FAILED, ConfigProfiles.save(dir, "PvP", newConfig()));
		ConfigProfiles.NamesResult names = ConfigProfiles.names(dir);
		assertTrue(names.names().isEmpty());
		assertEquals(ConfigProfiles.Result.WRITE_FAILED, names.status(),
			"unreadable path must be reported instead of an empty list");

		// 目录必须原样保留：不能被当成损坏文件搬走或覆盖
		assertTrue(Files.isDirectory(dir));
		try (Stream<Path> files = Files.list(tempDir)) {
			assertEquals(1, files.count(), "no backup file should be created for an unreadable path");
		}
	}

	@Test
	void blankNameIsRejected() {
		Path path = profilesPath();
		TweakifyModConfig config = newConfig();
		assertEquals(ConfigProfiles.Result.NOT_FOUND, ConfigProfiles.save(path, "", config));
		assertEquals(ConfigProfiles.Result.NOT_FOUND, ConfigProfiles.save(path, null, config));
		assertEquals(ConfigProfiles.Result.NOT_FOUND, ConfigProfiles.read(path, "").status());
	}
}
