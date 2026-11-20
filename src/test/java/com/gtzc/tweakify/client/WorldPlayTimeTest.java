package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 存档时长读取与格式化：stats 累加、损坏文件跳过、各显示格式的边界。
 */
class WorldPlayTimeTest {
	@TempDir
	Path tempDir;

	@Test
	void readPlayTimeTicksSumsAllStatsFiles() throws IOException {
		Path stats = Files.createDirectories(this.tempDir.resolve("players").resolve("stats"));
		writeStats(stats.resolve("a.json"), 1200);
		writeStats(stats.resolve("b.json"), 800);

		assertEquals(2000L, WorldPlayTime.readPlayTimeTicks(this.tempDir));
	}

	@Test
	void readPlayTimeTicksPrefersPlayerStatsOverLegacyDirectory() throws IOException {
		Path playerStats = Files.createDirectories(this.tempDir.resolve("players").resolve("stats"));
		writeStats(playerStats.resolve("current.json"), 600);
		Path legacyStats = Files.createDirectories(this.tempDir.resolve("stats"));
		writeStats(legacyStats.resolve("old.json"), 9999);

		assertEquals(600L, WorldPlayTime.readPlayTimeTicks(this.tempDir),
			"players/stats 存在时不能再累加旧的根目录 stats");
	}

	@Test
	void readPlayTimeTicksFallsBackToLegacyStatsDirectory() throws IOException {
		Path legacyStats = Files.createDirectories(this.tempDir.resolve("stats"));
		writeStats(legacyStats.resolve("old.json"), 3600);

		assertEquals(3600L, WorldPlayTime.readPlayTimeTicks(this.tempDir));
	}

	@Test
	void readPlayTimeTicksIgnoresBrokenAndIrrelevantFiles() throws IOException {
		Path stats = Files.createDirectories(this.tempDir.resolve("players").resolve("stats"));
		writeStats(stats.resolve("valid.json"), 600);
		Files.writeString(stats.resolve("broken.json"), "{not json");
		Files.writeString(stats.resolve("other.json"),
			"{\"stats\":{\"minecraft:custom\":{\"minecraft:jump\":5}}}");
		Files.writeString(stats.resolve("notes.txt"), "ignored");

		assertEquals(600L, WorldPlayTime.readPlayTimeTicks(this.tempDir));
	}

	@Test
	void readPlayTimeTicksClampsNegativeValues() throws IOException {
		Path stats = Files.createDirectories(this.tempDir.resolve("players").resolve("stats"));
		writeStats(stats.resolve("a.json"), -50);

		assertEquals(0L, WorldPlayTime.readPlayTimeTicks(this.tempDir));
	}

	@Test
	void missingStatsDirectoryReturnsZero() {
		assertEquals(0L, WorldPlayTime.readPlayTimeTicks(this.tempDir.resolve("no-such-world")));
	}

	@Test
	void clockTextFormatsHoursAndMinutes() {
		assertEquals("0:01", WorldPlayTime.clockText(1200L));
		assertEquals("1:01", WorldPlayTime.clockText(73200L));
		assertEquals("", WorldPlayTime.clockText(0L));
	}

	@Test
	void hoursTextUsesOneDecimalAndIntegerAfterHundred() {
		assertEquals("1.0", WorldPlayTime.hoursText(72000L));
		assertEquals("12.5", WorldPlayTime.hoursText(900000L));
		assertEquals("100", WorldPlayTime.hoursText(7200000L));
	}

	@Test
	void durationPartsKeepRemainderMinutes() {
		assertEquals(1L, WorldPlayTime.durationHours(73200L));
		assertEquals(1L, WorldPlayTime.durationMinutes(73200L));
		assertEquals(0L, WorldPlayTime.durationMinutes(72000L));
	}

	@Test
	void minutesTextNeverShowsZero() {
		assertEquals("1", WorldPlayTime.minutesText(20L));
		assertEquals("1", WorldPlayTime.minutesText(60L));
		assertEquals("2", WorldPlayTime.minutesText(2400L));
	}

	@Test
	void rightAlignedTextStaysInsideContent() {
		assertEquals(76, WorldPlayTimeRenderer.xForRightAligned(100, 20));
	}

	private static void writeStats(Path file, long ticks) throws IOException {
		Files.writeString(file,
			"{\"stats\":{\"minecraft:custom\":{\"minecraft:play_time\":" + ticks + "}}}");
	}
}
