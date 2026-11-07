package com.gtzc.tweakify.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 覆盖落盘层的边界：无父目录的路径、损坏文件备份。
 * 异步写盘任务里的异常会静默丢失回滚与日志，所以这里必须直接返回失败而不是抛异常。
 */
class ConfigStorageTest {
	@TempDir
	Path tempDir;

	@Test
	void writeAtomicallyRejectsPathWithoutParent() {
		assertFalse(ConfigStorage.writeAtomically("{}", Path.of("no-parent.json")));
	}

	// 备份成功时返回备份路径：调用方据此告诉用户从哪里恢复旧内容。
	@Test
	void backupBrokenToReturnsBackupPath() throws IOException {
		Path original = tempDir.resolve("config.json");
		Files.writeString(original, "broken");

		Path backup = ConfigStorage.backupBrokenTo(original);

		assertNotNull(backup);
		assertTrue(backup.getFileName().toString().startsWith("config.json.broken-"),
			"backup name must follow <original>.broken-<timestamp>");
		assertFalse(Files.exists(original), "original must be moved away");
		assertEquals("broken", Files.readString(backup));
	}

	// 源文件不存在时备份失败，返回 null（调用方据此拒绝覆盖）。
	@Test
	void backupBrokenToReturnsNullWhenSourceMissing() {
		assertNull(ConfigStorage.backupBrokenTo(tempDir.resolve("missing.json")));
	}

	// boolean 包装与返回路径的版本结果一致。
	@Test
	void backupBrokenReflectsPathVersion() throws IOException {
		Path original = tempDir.resolve("config.json");
		Files.writeString(original, "broken");

		assertTrue(ConfigStorage.backupBroken(original));
		assertFalse(Files.exists(original));
	}
}
