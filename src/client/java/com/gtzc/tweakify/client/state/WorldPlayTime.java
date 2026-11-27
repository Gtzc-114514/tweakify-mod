package com.gtzc.tweakify.client.state;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.config.SavePlayTimeFormat;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 存档游玩时长：读取存档 stats 中每个玩家的 play_time（tick）并累加，
 * 供存档列表条目在右上角显示。参考 Khajiitos/WorldPlayTime 的做法。
 *
 * <p>数据在存档列表刷新（fillLevels）时读取；搜索过滤会复用同一列表实例，
 * 因此按引用去重，不会每个按键都重扫磁盘。渲染帧只按 levelId 查一次 HashMap。
 * 损坏/半写入的文件按 0 跳过，不影响列表本身。
 */
public final class WorldPlayTime {
	private static final String PLAYER_STATS_PATH = "players/stats";
	// 旧版存档把统计放在根目录 stats；迁移过的存档只有 players/stats。
	private static final String LEGACY_STATS_PATH = "stats";
	private static final String JSON_SUFFIX = ".json";
	private static final String STATS_KEY = "stats";
	private static final String CUSTOM_KEY = "minecraft:custom";
	private static final String PLAY_TIME_KEY = "minecraft:play_time";
	static final String HOURS_TRANSLATION_KEY = "tweakify-mod.world_play_time.hours";
	static final String DURATION_TRANSLATION_KEY = "tweakify-mod.world_play_time.duration";
	static final String MINUTES_TRANSLATION_KEY = "tweakify-mod.world_play_time.minutes";
	private static final long TICKS_PER_MINUTE = 20L * 60L;
	private static final long TICKS_PER_HOUR = TICKS_PER_MINUTE * 60L;

	// levelId -> 累计 play_time（tick）；只在客户端主线程读写。
	private static final Map<String, Long> CACHE = new HashMap<>();
	// 最近一次刷新使用的列表实例：搜索过滤复用同一实例，引用相同就跳过重读。
	private static List<LevelSummary> lastRefreshed;

	private WorldPlayTime() {
	}

	// 存档列表刷新：重新读取全部单人存档的时长；功能关闭时清空缓存。
	// public：由存档列表 mixin 调用。
	public static void refresh(List<LevelSummary> levels) {
		if (levels == lastRefreshed) {
			// 搜索过滤不会换列表实例：同一批存档无需重扫磁盘。
			return;
		}
		lastRefreshed = levels;
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
		lastRefreshed = null;
	}

	public static long ticksFor(LevelSummary summary) {
		if (summary == null || summary.getLevelId() == null) {
			return 0L;
		}
		return CACHE.getOrDefault(summary.getLevelId(), 0L);
	}

	// 读取一个存档目录下的 play_time 之和。
	// 26.3 的统计目录是 LevelResource.PLAYER_STATS_DIR（players/stats）；
	// 旧存档仍可能只有根目录 stats，且迁移是逐文件 move（中断会留下新旧两份），
	// 因此按文件名（玩家 UUID）取并集：同名以 players/stats 为准，旧目录只补缺失项。
	// 整份忽略旧目录会漏计，直接把两个目录相加会重复计数。
	static long readPlayTimeTicks(Path levelDirectory) {
		Map<String, Path> files = new LinkedHashMap<>(collectStatsFiles(
			levelDirectory.resolve(PLAYER_STATS_PATH)));
		collectStatsFiles(levelDirectory.resolve(LEGACY_STATS_PATH)).forEach(files::putIfAbsent);
		long total = 0L;
		for (Path file : files.values()) {
			total += readFileTicks(file);
		}
		return total;
	}

	// 按文件名收集统计文件；目录不存在/不可读或遍历中单条目异常时，
	// 保留已收集的部分，不影响另一个目录。
	private static Map<String, Path> collectStatsFiles(Path directory) {
		Map<String, Path> files = new LinkedHashMap<>();
		if (!Files.isDirectory(directory)) {
			return files;
		}
		try (Stream<Path> stream = Files.list(directory)) {
			Iterator<Path> iterator = stream.iterator();
			while (iterator.hasNext()) {
				Path file = iterator.next();
				if (isStatsFile(file)) {
					files.putIfAbsent(file.getFileName().toString(), file);
				}
			}
		} catch (IOException | UncheckedIOException e) {
			// 保留已收集的部分（包括单条目被并发删除的情况）。
		}
		return files;
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

	// 显示文本的装配结果：key 为 null 时 args[0] 本身就是纯文本（clock）。
	// 包私有：测试直接断言键与参数，不依赖语言资源是否已加载。
	record PlayTimeText(String key, Object[] args) {
	}

	// 渲染文本：0/负数不显示。时钟格式是纯文本，其余按语言键装配。
	public static String displayText(long ticks, String formatCode) {
		PlayTimeText parts = textParts(ticks, formatCode);
		if (parts == null) {
			return "";
		}
		if (parts.key() == null) {
			return (String) parts.args()[0];
		}
		return Component.translatable(parts.key(), parts.args()).getString();
	}

	// 包私有：按格式选择语言键与参数；测试锁定分发逻辑（键 + 参数）。
	static PlayTimeText textParts(long ticks, String formatCode) {
		if (ticks <= 0L) {
			return null;
		}
		SavePlayTimeFormat format = SavePlayTimeFormat.fromCode(formatCode);
		return switch (format) {
			case CLOCK -> new PlayTimeText(null, new Object[]{clockText(ticks)});
			case DURATION -> durationParts(ticks);
			case HOURS -> durationHours(ticks) > 0
				? new PlayTimeText(HOURS_TRANSLATION_KEY, new Object[]{hoursText(ticks)})
				: new PlayTimeText(MINUTES_TRANSLATION_KEY, new Object[]{minutesText(ticks)});
		};
	}

	private static PlayTimeText durationParts(long ticks) {
		long hours = durationHours(ticks);
		long minutes = durationMinutes(ticks);
		if (hours <= 0) {
			return new PlayTimeText(MINUTES_TRANSLATION_KEY, new Object[]{minutesText(ticks)});
		}
		if (minutes <= 0) {
			// 整小时不再显示“0 分”。
			return new PlayTimeText(HOURS_TRANSLATION_KEY, new Object[]{String.valueOf(hours)});
		}
		return new PlayTimeText(DURATION_TRANSLATION_KEY, new Object[]{hours, minutes});
	}

	// 小时文本：100 小时以上取整，否则按配置保留小数位。
	static String hoursText(long ticks) {
		TweakifyModConfig config = TweakifyMod.config();
		int decimals = config.functions.savePlayTimeDecimals;
		return hoursText(ticks, decimals);
	}

	static String hoursText(long ticks, int decimals) {
		double hours = (double) ticks / TICKS_PER_HOUR;
		if (hours >= 100.0) {
			return String.valueOf((long) hours);
		}
		int clamped = Math.max(TweakifyModConfig.MIN_SAVE_PLAY_TIME_DECIMALS,
			Math.min(TweakifyModConfig.MAX_SAVE_PLAY_TIME_DECIMALS, decimals));
		return String.format(Locale.ROOT, "%." + clamped + "f", hours);
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
