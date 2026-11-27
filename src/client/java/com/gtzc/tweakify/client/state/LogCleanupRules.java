package com.gtzc.tweakify.client.state;

import com.gtzc.tweakify.config.TweakifyModConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日志清理的文件选择规则：只认 Minecraft/log4j 的日期编号归档
 * （{@code YYYY-MM-DD-N.log} 或 {@code YYYY-MM-DD-N.log.gz}）。
 *
 * <p>latest.log、debug.log 以及任何未知文件都不参与清理，
 * 避免误删正在写入或用户自己放置的文件。
 */
public final class LogCleanupRules {
	// 归档命名：2024-01-01-1.log.gz / 2024-01-01-1.log；分组 1 为日期，分组 2 为编号。
	private static final Pattern ARCHIVE_NAME =
		Pattern.compile("^(\\d{4}-\\d{2}-\\d{2})-(\\d+)\\.log(?:\\.gz)?$");

	private LogCleanupRules() {
	}

	public static boolean isArchivedLog(String fileName) {
		return fileName != null && ARCHIVE_NAME.matcher(fileName).matches();
	}

	// 保留数量夹到 0 ~ 100：0 表示删除全部归档。
	public static int clampKeepCount(int keepCount) {
		return Math.max(TweakifyModConfig.MIN_LOG_CLEANUP_KEEP_COUNT,
			Math.min(TweakifyModConfig.MAX_LOG_CLEANUP_KEEP_COUNT, keepCount));
	}

	// 按最后修改时间从新到旧排序；保留前 keepCount 个，其余作为待删除项返回。
	// 修改时间相同（同一秒滚动多次，或从备份恢复全部相同 mtime）时按归档序号倒序：
	// 必须比较编号数值，否则字典序会把 -9 当成比 -10 更新。
	public static List<Path> selectForDeletion(List<Path> archives, int keepCount) {
		int clamped = clampKeepCount(keepCount);
		List<Path> sorted = new ArrayList<>(archives);
		Comparator<Path> byModifiedDesc =
			Comparator.comparingLong((Path path) -> lastModifiedMillis(path)).reversed();
		Comparator<Path> byArchiveOrderDesc = Comparator
			.comparingLong((Path path) -> archiveOrder(path)).reversed();
		sorted.sort(byModifiedDesc.thenComparing(byArchiveOrderDesc));
		if (clamped >= sorted.size()) {
			return List.of();
		}
		return List.copyOf(sorted.subList(clamped, sorted.size()));
	}

	// 归档序号 = yyyyMMdd * 1_000_000 + 编号，用于修改时间相同时的先后判定。
	// 编号实际很小，超过 6 位时截断，避免手工改名把 long 乘爆。
	private static long archiveOrder(Path path) {
		Matcher matcher = ARCHIVE_NAME.matcher(path.getFileName().toString());
		if (!matcher.matches()) {
			return Long.MIN_VALUE;
		}
		try {
			long date = Long.parseLong(matcher.group(1).replace("-", ""));
			long index = Math.min(Long.parseLong(matcher.group(2)), 999_999L);
			return date * 1_000_000L + index;
		} catch (NumberFormatException e) {
			return Long.MIN_VALUE;
		}
	}

	private static long lastModifiedMillis(Path path) {
		try {
			return Files.getLastModifiedTime(path).toMillis();
		} catch (IOException e) {
			return 0L;
		}
	}
}
