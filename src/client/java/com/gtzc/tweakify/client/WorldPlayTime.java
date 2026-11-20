package com.gtzc.tweakify.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 存档游玩时长：读取存档 stats 中每个玩家的 play_time（tick）并累加，
 * 供存档列表条目在右上角显示。参考 Khajiitos/WorldPlayTime 的做法。
 *
 * <p>读取发生在存档列表刷新（fillLevels）时，不在渲染帧：渲染帧只按 levelId
 * 查一次 HashMap。stats 文件很小，损坏/半写入的文件按 0 跳过，不影响列表本身。
 */
public final class WorldPlayTime {
	private static final String PLAYER_STATS_PATH = "players/stats";
	// 旧版存档把统计放在根目录 stats；迁移过的存档只有 players/stats。
	private static final String LEGACY_STATS_PATH = "stats";
	private static final String JSON_SUFFIX = ".json";
	private static final String STATS_KEY = "stats";
	private static final String CUSTOM_KEY = "minecraft:custom";
	private static final String PLAY_TIME_KEY = "minecraft:play_time";
	private static final long TICKS_PER_MINUTE = 20L * 60L;
	private static final long TICKS_PER_HOUR = TICKS_PER_MINUTE * 60L;

	// levelId -> 累计 play_time（tick）；只在客户端主线程读写。
	private static final Map<String, Long> CACHE = new HashMap<>();

	private WorldPlayTime() {
	}

	// 存档列表刷新：重新读取全部单人存档的时长；功能关闭时清空缓存。
	// public：由存档列表 mixin 调用。
	public static void refresh(List<LevelSummary> levels) {
		CACHE.clear();
		LevelStorageSource source = Minecraft.getInstance().getLevelSource();
		for (LevelSummary summary : levels) {
			String levelId = summary.getLevelId();
			if (levelId == null) {
				continue;
			}
			try {
				CACHE.put(levelId, readPlayTimeTicks(source.getLevelPath(levelId)));
			} catch (RuntimeException e) {
				// 路径异常按 0 处理，不影响其它存档。
				CACHE.put(levelId, 0L);
			}
		}
	}

	public static void clear() {
		CACHE.clear();
	}

	public static long ticksFor(LevelSummary summary) {
		if (summary == null || summary.getLevelId() == null) {
			return 0L;
		}
		return CACHE.getOrDefault(summary.getLevelId(), 0L);
	}

	// 读取一个存档目录下的 play_time 之和。
	// 26.3 的统计目录是 LevelResource.PLAYER_STATS_DIR（players/stats）；
	// 旧存档仍可能只有根目录 stats，仅在前者不存在时回退，避免两个目录并存时重复计数。
	// 单人存档通常只有一个玩家文件；多人存档按所有玩家累加（与参考实现一致）。
	static long readPlayTimeTicks(Path levelDirectory) {
		Path playerStats = levelDirectory.resolve(PLAYER_STATS_PATH);
		if (Files.isDirectory(playerStats)) {
			return readStatsDirectory(playerStats);
		}
		return readStatsDirectory(levelDirectory.resolve(LEGACY_STATS_PATH));
	}

	private static long readStatsDirectory(Path statsDirectory) {
		if (!Files.isDirectory(statsDirectory)) {
			return 0L;
		}
		long total = 0L;
		try (Stream<Path> files = Files.list(statsDirectory)) {
			for (Path file : files.toList()) {
				if (!isStatsFile(file)) {
					continue;
				}
				total += readFileTicks(file);
			}
		} catch (IOException e) {
			return total;
		}
		return total;
	}

	private static boolean isStatsFile(Path file) {
		return Files.isRegularFile(file) && file.getFileName().toString().endsWith(JSON_SUFFIX);
	}

	private static long readFileTicks(Path file) {
		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			JsonElement root = JsonParser.parseReader(reader);
			if (!root.isJsonObject()) {
				return 0L;
			}
			JsonObject stats = getObject(root.getAsJsonObject(), STATS_KEY);
			JsonObject custom = stats == null ? null : getObject(stats, CUSTOM_KEY);
			if (custom == null || !custom.has(PLAY_TIME_KEY)) {
				return 0L;
			}
			JsonElement value = custom.get(PLAY_TIME_KEY);
			if (value == null || !value.isJsonPrimitive()
				|| !value.getAsJsonPrimitive().isNumber()) {
				return 0L;
			}
			return Math.max(0L, value.getAsLong());
		} catch (IOException | RuntimeException e) {
			// 损坏/半写入的 stats 文件：跳过该玩家，不阻塞列表。
			return 0L;
		}
	}

	private static JsonObject getObject(JsonObject parent, String key) {
		JsonElement element = parent.get(key);
		return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
	}

	// 渲染文本：0/负数不显示。单位为语言文件键，切换语言后立即生效。
	public static String displayText(long ticks, String formatCode) {
		if (ticks <= 0L) {
			return "";
		}
		SavePlayTimeFormat format = SavePlayTimeFormat.fromCode(formatCode);
		return switch (format) {
			case CLOCK -> clockText(ticks);
			case DURATION -> durationText(ticks);
			case HOURS -> durationHours(ticks) > 0
				? Component.translatable("tweakify-mod.world_play_time.hours", hoursText(ticks)).getString()
				: Component.translatable("tweakify-mod.world_play_time.minutes", minutesText(ticks)).getString();
		};
	}

	private static String durationText(long ticks) {
		long hours = durationHours(ticks);
		long minutes = durationMinutes(ticks);
		if (hours <= 0) {
			return Component.translatable("tweakify-mod.world_play_time.minutes", minutesText(ticks)).getString();
		}
		if (minutes <= 0) {
			return Component.translatable("tweakify-mod.world_play_time.hours", String.valueOf(hours)).getString();
		}
		return Component.translatable("tweakify-mod.world_play_time.duration", hours, minutes).getString();
	}

	// 小时文本：100 小时以上取整，否则保留 1 位小数（参考实现）。
	static String hoursText(long ticks) {
		double hours = (double) ticks / TICKS_PER_HOUR;
		return hours >= 100.0 ? String.valueOf((long) hours) : String.format(Locale.ROOT, "%.1f", hours);
	}

	// 时:分（不足 1 小时为 0:MM）；负数/0 返回空。
	static String clockText(long ticks) {
		if (ticks <= 0L) {
			return "";
		}
		long totalMinutes = ticks / TICKS_PER_MINUTE;
		return String.format(Locale.ROOT, "%d:%02d", totalMinutes / 60L, totalMinutes % 60L);
	}

	static long durationHours(long ticks) {
		return Math.max(0L, ticks / TICKS_PER_HOUR);
	}

	// 去掉整小时后剩余的分钟（可为 0，显示层会省略“0 分”）。
	static long durationMinutes(long ticks) {
		return Math.max(0L, (ticks % TICKS_PER_HOUR) / TICKS_PER_MINUTE);
	}

	// 不足 1 小时的分钟文本；至少 1 分钟，避免显示“0 分钟”。
	static String minutesText(long ticks) {
		return String.valueOf(Math.max(1L, ticks / TICKS_PER_MINUTE));
	}
}
