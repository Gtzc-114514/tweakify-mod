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

	// 提交序号更小的旧任务即使晚于新任务执行，也不能覆盖新内容。
	@Test
	void staleWriteAfterNewerWriteIsSkipped() throws IOException {
		Path path = tempDir.resolve("ordered.json");

		assertTrue(ConfigStorage.writeAtomically("{\"v\":2}", path, 2L));
		assertTrue(ConfigStorage.writeAtomically("{\"v\":1}", path, 1L),
			"a stale write should report success because a newer write already covered it");

		assertEquals("{\"v\":2}", Files.readString(path),
			"a stale write must not overwrite newer content");
	}

	// 失败的写不能占用提交序号，否则更旧（但真正成功）的写会被错误跳过。
	@Test
	void failedWriteDoesNotOccupySequence() throws IOException {
		Path blockedParent = tempDir.resolve("blocked-parent");
		Files.writeString(blockedParent, "x");
		Path path = blockedParent.resolve("config.json");

		assertFalse(ConfigStorage.writeAtomically("{\"v\":2}", path, 5L));

		Files.delete(blockedParent);
		Files.createDirectories(blockedParent);

		assertTrue(ConfigStorage.writeAtomically("{\"v\":1}", path, 4L),
			"a failed write must not mark its sequence as written");
		assertEquals("{\"v\":1}", Files.readString(path));
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
