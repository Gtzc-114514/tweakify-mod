package com.gtzc.tweakify.config;

import com.gtzc.tweakify.util.AsyncFileWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 收藏夹存储：切换语义、跨重启持久化、坏文件回退与非字符串元素容错。
 *
 * <p>写盘与主配置共用后台队列，测试结束必须等待队列排空，避免后台任务在
 * @TempDir 清理后仍写文件。
 */
class FavoritesStorageTest {
	private static final long DRAIN_TIMEOUT_MILLIS = 15_000L;

	@TempDir
	Path tempDir;

	private Path file;

	@BeforeEach
	void setUp() {
		file = tempDir.resolve(FavoritesStorage.FILE_NAME);
		FavoritesStorage.resetForTest(file);
	}

	@AfterEach
	void tearDown() {
		AsyncFileWriter.drain(DRAIN_TIMEOUT_MILLIS);
		FavoritesStorage.clearForTest();
	}

	@Test
	void togglingConfigFavoriteAddsAndRemoves() {
		assertTrue(FavoritesStorage.toggleConfigFavorite("cameraMotion"), "first toggle should favorite");
		assertTrue(FavoritesStorage.isConfigFavorite("cameraMotion"));
		assertFalse(FavoritesStorage.toggleConfigFavorite("cameraMotion"), "second toggle should unfavorite");
		assertFalse(FavoritesStorage.isConfigFavorite("cameraMotion"));
		assertTrue(FavoritesStorage.configFavorites().isEmpty());
	}

	@Test
	void togglingWorldFavoriteIsIndependentFromConfigFavorites() {
		assertTrue(FavoritesStorage.toggleWorldFavorite("world"));
		assertTrue(FavoritesStorage.isWorldFavorite("world"));
		assertFalse(FavoritesStorage.isConfigFavorite("world"));
		assertFalse(FavoritesStorage.toggleWorldFavorite("world"), "second toggle should unfavorite");
		assertFalse(FavoritesStorage.isWorldFavorite("world"));
	}

	@Test
	void favoritesPersistAcrossReload() throws IOException {
		assertTrue(FavoritesStorage.toggleConfigFavorite("cameraMotion"));
		assertTrue(FavoritesStorage.toggleWorldFavorite("world"));
		FavoritesStorage.flushOnShutdown();

		// 模拟重启：清空内存后从同一文件重新加载。
		FavoritesStorage.resetForTest(file);
		assertTrue(FavoritesStorage.isConfigFavorite("cameraMotion"));
		assertTrue(FavoritesStorage.isWorldFavorite("world"));
	}

	@Test
	void jsonContainsBothLists() {
		assertTrue(FavoritesStorage.toggleConfigFavorite("cameraMotion"));
		assertTrue(FavoritesStorage.toggleWorldFavorite("world"));
		String json = FavoritesStorage.toJson();
		assertTrue(json.contains("\"configs\""));
		assertTrue(json.contains("\"worlds\""));
		assertTrue(json.contains("cameraMotion"));
		assertTrue(json.contains("world"));
	}

	@Test
	void brokenFileFallsBackToEmptyAndIsBackedUp() throws IOException {
		Files.writeString(file, "{not-json");
		assertFalse(FavoritesStorage.isConfigFavorite("cameraMotion"));
		assertTrue(FavoritesStorage.configFavorites().isEmpty());
		// 损坏文件被移到 .broken-<时间戳>，原路径留空等待下一次写盘。
		assertFalse(Files.exists(file));
		try (var files = Files.list(tempDir)) {
			assertTrue(files.anyMatch(path -> path.getFileName().toString()
				.startsWith(FavoritesStorage.FILE_NAME + ".broken-")));
		}
	}

	@Test
	void nonStringEntriesAreIgnored() throws IOException {
		Files.writeString(file,
			"{\"configs\":[\"cameraMotion\",1,true,null],\"worlds\":\"not-an-array\"}");
		assertEquals(List.of("cameraMotion"), FavoritesStorage.configFavorites());
		assertTrue(FavoritesStorage.worldFavorites().isEmpty());
	}

	@Test
	void retainWorldFavoritesDropsStaleIds() {
		assertTrue(FavoritesStorage.toggleWorldFavorite("a"));
		assertTrue(FavoritesStorage.toggleWorldFavorite("b"));
		assertTrue(FavoritesStorage.toggleWorldFavorite("c"));
		FavoritesStorage.retainWorldFavorites(List.of("b"));
		assertEquals(List.of("b"), FavoritesStorage.worldFavorites());
		// 第二次收敛：集合无变化时保持原样，也不会把不存在的存档加回来。
		FavoritesStorage.retainWorldFavorites(List.of("b", "d"));
		assertEquals(List.of("b"), FavoritesStorage.worldFavorites());
		assertFalse(FavoritesStorage.isWorldFavorite("d"));
	}

	@Test
	void blankNamesAreIgnored() {
		assertFalse(FavoritesStorage.toggleConfigFavorite("  "));
		assertFalse(FavoritesStorage.toggleWorldFavorite(null));
		assertTrue(FavoritesStorage.configFavorites().isEmpty());
		assertTrue(FavoritesStorage.worldFavorites().isEmpty());
	}
}
