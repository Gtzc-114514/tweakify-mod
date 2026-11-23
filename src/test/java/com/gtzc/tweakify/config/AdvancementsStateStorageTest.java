package com.gtzc.tweakify.config;

import com.gtzc.tweakify.util.AsyncFileWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 进度界面状态：固定标签切换、上次标签与跨重启持久化。
 */
class AdvancementsStateStorageTest {
	private static final long DRAIN_TIMEOUT_MILLIS = 15_000L;

	@TempDir
	Path tempDir;

	private Path file;

	@BeforeEach
	void setUp() {
		file = tempDir.resolve(AdvancementsStateStorage.FILE_NAME);
		AdvancementsStateStorage.resetForTest(file);
	}

	@AfterEach
	void tearDown() {
		AsyncFileWriter.drain(DRAIN_TIMEOUT_MILLIS);
		AdvancementsStateStorage.resetForTest(null);
	}

	@Test
	void togglingPinnedAddsAndRemoves() {
		assertTrue(AdvancementsStateStorage.togglePinned("minecraft:story/root"));
		assertTrue(AdvancementsStateStorage.isPinned("minecraft:story/root"));
		assertEquals(0, AdvancementsStateStorage.pinIndex("minecraft:story/root"));
		assertFalse(AdvancementsStateStorage.togglePinned("minecraft:story/root"));
		assertFalse(AdvancementsStateStorage.isPinned("minecraft:story/root"));
		assertEquals(-1, AdvancementsStateStorage.pinIndex("minecraft:story/root"));
	}

	@Test
	void pinnedOrderFollowsInsertion() {
		AdvancementsStateStorage.togglePinned("minecraft:story/root");
		AdvancementsStateStorage.togglePinned("minecraft:nether/root");
		assertEquals(0, AdvancementsStateStorage.pinIndex("minecraft:story/root"));
		assertEquals(1, AdvancementsStateStorage.pinIndex("minecraft:nether/root"));
	}

	@Test
	void lastSelectedTabIsRemembered() {
		assertEquals("", AdvancementsStateStorage.lastSelectedTab());
		AdvancementsStateStorage.setLastSelectedTab("minecraft:story/root");
		assertEquals("minecraft:story/root", AdvancementsStateStorage.lastSelectedTab());
	}

	@Test
	void statePersistsAcrossReload() {
		AdvancementsStateStorage.togglePinned("minecraft:story/root");
		AdvancementsStateStorage.setLastSelectedTab("minecraft:story/root");
		AsyncFileWriter.drain(DRAIN_TIMEOUT_MILLIS);

		AdvancementsStateStorage.resetForTest(file);
		assertTrue(AdvancementsStateStorage.isPinned("minecraft:story/root"));
		assertEquals("minecraft:story/root", AdvancementsStateStorage.lastSelectedTab());
	}

	@Test
	void jsonContainsPinnedAndLastTab() {
		AdvancementsStateStorage.togglePinned("minecraft:story/root");
		AdvancementsStateStorage.setLastSelectedTab("minecraft:story/root");
		String json = AdvancementsStateStorage.toJson();
		assertTrue(json.contains("\"pinnedTabs\""));
		assertTrue(json.contains("\"lastSelectedTab\""));
		assertTrue(json.contains("minecraft:story/root"));
	}
}
