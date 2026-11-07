    package com.gtzc.tweakify.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScrollMemoryTest {
	@TempDir
	Path tempDir;

	@BeforeEach
	void resetState() {
		ScrollMemory.resetForTest();
	}

	@Test
	void successfulFlushWritesJsonAndClearsDirty() throws IOException {
		ScrollMemory.putModMenu(7);
		assertTrue(ScrollMemory.isDirty());

		Path target = tempDir.resolve("scroll-memory.json");
		assertTrue(ScrollMemory.flushTo(target));
		assertFalse(ScrollMemory.isDirty(), "写盘成功后应清除 dirty");

		String json = Files.readString(target);
		assertTrue(json.contains("\"modmenu\""));
		assertTrue(json.contains("7"));

		try (Stream<Path> files = Files.list(tempDir)) {
			assertEquals(1, files.count(), "原子写不应留下临时文件");
		}
	}

	@Test
	void successfulFlushReplacesExistingFile() throws IOException {
		Path target = tempDir.resolve("scroll-memory.json");
		Files.writeString(target, "{\"stale\":1}");
		ScrollMemory.putModMenu(9);

		assertTrue(ScrollMemory.flushTo(target));

		String json = Files.readString(target);
		assertFalse(json.contains("stale"), "旧内容应被完整替换");
		assertTrue(json.contains("9"));
	}

	@Test
	void failedFlushKeepsDirtyAndLeavesTargetIntact() throws IOException {
		ScrollMemory.putModMenu(42);
		assertTrue(ScrollMemory.isDirty());

		// 目标是非空目录：移动临时文件必然失败，用来模拟写盘错误。
		Path blocked = tempDir.resolve("blocked-scroll");
		Files.createDirectories(blocked);
		Path keep = blocked.resolve("keep.txt");
		Files.writeString(keep, "keep");

		assertFalse(ScrollMemory.flushTo(blocked));
		assertTrue(ScrollMemory.isDirty(), "写盘失败时必须保留 dirty，等待下一次重试");
		assertTrue(Files.isDirectory(blocked));
		assertEquals("keep", Files.readString(keep), "失败时不能破坏已有内容");

		try (Stream<Path> files = Files.list(tempDir)) {
			assertEquals(1, files.count(), "失败后临时文件必须被清理");
		}
	}
}
