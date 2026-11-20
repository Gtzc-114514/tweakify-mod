package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
	void playerStatsWinOverLegacyFileWithSameName() throws IOException {
		Path playerStats = Files.createDirectories(this.tempDir.resolve("players").resolve("stats"));
		writeStats(playerStats.resolve("uuid.json"), 600);
		Path legacyStats = Files.createDirectories(this.tempDir.resolve("stats"));
		writeStats(legacyStats.resolve("uuid.json"), 9999);

		assertEquals(600L, WorldPlayTime.readPlayTimeTicks(this.tempDir),
			"同名统计以 players/stats 为准，不能相加");
	}

	@Test
	void mergesLegacyFilesMissingFromPlayerStats() throws IOException {
		Path playerStats = Files.createDirectories(this.tempDir.resolve("players").resolve("stats"));
		writeStats(playerStats.resolve("current.json"), 600);
		Path legacyStats = Files.createDirectories(this.tempDir.resolve("stats"));
		writeStats(legacyStats.resolve("partially-migrated.json"), 900);

		assertEquals(1500L, WorldPlayTime.readPlayTimeTicks(this.tempDir),
			"新目录缺失的旧文件必须补回，但同名文件不能重复累加");
	}

	@Test
	void emptyPlayerStatsDirectoryStillReadsLegacyFiles() throws IOException {
		Files.createDirectories(this.tempDir.resolve("players").resolve("stats"));
		Path legacyStats = Files.createDirectories(this.tempDir.resolve("stats"));
		writeStats(legacyStats.resolve("old.json"), 900);

		assertEquals(900L, WorldPlayTime.readPlayTimeTicks(this.tempDir));
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
		Files.writeString(stats.resolve("notes.dat"), "ignored");

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
	void textPartsSelectTheExpectedTranslation() {
		WorldPlayTime.PlayTimeText minute = WorldPlayTime.textParts(1200L, "hours");
		assertEquals(WorldPlayTime.MINUTES_TRANSLATION_KEY, minute.key());
		assertEquals("1", minute.args()[0]);

		WorldPlayTime.PlayTimeText hour = WorldPlayTime.textParts(72000L, "hours");
		assertEquals(WorldPlayTime.HOURS_TRANSLATION_KEY, hour.key());
		assertEquals("1.0", hour.args()[0]);

		WorldPlayTime.PlayTimeText wholeHour = WorldPlayTime.textParts(72000L, "duration");
		assertEquals(WorldPlayTime.HOURS_TRANSLATION_KEY, wholeHour.key(),
			"整小时不应显示 0 分");
		assertEquals("1", wholeHour.args()[0]);

		WorldPlayTime.PlayTimeText duration = WorldPlayTime.textParts(73200L, "duration");
		assertEquals(WorldPlayTime.DURATION_TRANSLATION_KEY, duration.key());
		assertEquals(1L, duration.args()[0]);
		assertEquals(1L, duration.args()[1]);

		WorldPlayTime.PlayTimeText clock = WorldPlayTime.textParts(1200L, "clock");
		assertNull(clock.key());
		assertEquals("0:01", clock.args()[0]);

		assertNull(WorldPlayTime.textParts(0L, "hours"));
		assertNull(WorldPlayTime.textParts(-1L, "clock"));
	}

	@Test
	void unknownOrNullFormatFallsBackToHours() {
		assertEquals(SavePlayTimeFormat.HOURS, SavePlayTimeFormat.fromCode(null));
		assertEquals(SavePlayTimeFormat.HOURS, SavePlayTimeFormat.fromCode("bogus"));
		assertEquals(SavePlayTimeFormat.CLOCK, SavePlayTimeFormat.fromCode("clock"));
	}

	@Test
	void displayTextReturnsPlainClockAndHidesNonPositiveValues() {
		assertEquals("", WorldPlayTime.displayText(0L, "clock"));
		assertEquals("", WorldPlayTime.displayText(-1L, "hours"));
		assertEquals("0:01", WorldPlayTime.displayText(1200L, "clock"));
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
