package com.gtzc.tweakify.client.state;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogCleanupStateTest {
	@TempDir
	Path tempDir;

	@Test
	void archiveNamesOnlyMatchDatedRollovers() {
		assertTrue(LogCleanupRules.isArchivedLog("2024-01-01-1.log.gz"));
		assertTrue(LogCleanupRules.isArchivedLog("2024-12-31-12.log"));
		assertFalse(LogCleanupRules.isArchivedLog("latest.log"));
		assertFalse(LogCleanupRules.isArchivedLog("debug.log"));
		assertFalse(LogCleanupRules.isArchivedLog("2024-01-01-1.log.gz.tmp"));
		assertFalse(LogCleanupRules.isArchivedLog(null));
	}

	@Test
	void keepCountIsClamped() {
		assertEquals(0, LogCleanupRules.clampKeepCount(-5));
		assertEquals(100, LogCleanupRules.clampKeepCount(999));
		assertEquals(10, LogCleanupRules.clampKeepCount(10));
	}

	@Test
	void newestArchivesAreKeptAndLatestLogSurvives() throws IOException {
		Path older = archive("2024-01-01-1.log.gz", 1_000L);
		Path newer = archive("2024-01-02-1.log.gz", 2_000L);
		Path newest = archive("2024-01-03-1.log.gz", 3_000L);
		Path latest = Files.writeString(tempDir.resolve("latest.log"), "live");
		Path debug = Files.writeString(tempDir.resolve("debug.log"), "debug");

		LogCleanupState.CleanupResult result = LogCleanupState.cleanup(tempDir, 2);

		assertEquals(3, result.scanned());
		assertEquals(1, result.deleted());
		assertEquals(2, result.kept());
		assertFalse(Files.exists(older), "最旧的归档应被删除");
		assertTrue(Files.exists(newer));
		assertTrue(Files.exists(newest));
		assertTrue(Files.exists(latest), "latest.log 永远不能删除");
		assertTrue(Files.exists(debug), "非归档日志不能删除");
	}

	@Test
	void zeroKeepCountDeletesAllArchivesOnly() throws IOException {
		Path archive = archive("2024-01-01-1.log.gz", 1_000L);
		Path latest = Files.writeString(tempDir.resolve("latest.log"), "live");

		LogCleanupState.CleanupResult result = LogCleanupState.cleanup(tempDir, 0);

		assertEquals(1, result.deleted());
		assertEquals(0, result.kept());
		assertFalse(Files.exists(archive));
		assertTrue(Files.exists(latest));
	}

	@Test
	void sameModifiedTimeKeepsHigherArchiveIndex() throws IOException {
		Path nine = archive("2024-01-01-9.log.gz", 1_000L);
		Path ten = archive("2024-01-01-10.log.gz", 1_000L);

		LogCleanupState.CleanupResult result = LogCleanupState.cleanup(tempDir, 1);

		assertEquals(1, result.deleted());
		assertFalse(Files.exists(nine), "同修改时间时 -9 应视为更旧");
		assertTrue(Files.exists(ten), "同修改时间时 -10 应视为更新");
	}

	@Test
	void keepCountAboveArchiveCountDeletesNothing() throws IOException {
		archive("2024-01-01-1.log.gz", 1_000L);
		LogCleanupState.CleanupResult result = LogCleanupState.cleanup(tempDir, 10);
		assertEquals(0, result.deleted());
		assertEquals(1, result.kept());
	}

	@Test
	void missingDirectoryIsNoOp() {
		LogCleanupState.CleanupResult result = LogCleanupState.cleanup(tempDir.resolve("missing"), 10);
		assertEquals(0, result.scanned());
		assertEquals(0, result.deleted());
		assertEquals(0, result.kept());
	}

	private Path archive(String name, long modifiedMillis) throws IOException {
		Path path = Files.writeString(tempDir.resolve(name), "log");
		Files.setLastModifiedTime(path, FileTime.fromMillis(modifiedMillis));
		return path;
	}
}
