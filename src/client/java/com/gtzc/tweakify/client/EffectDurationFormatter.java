package com.gtzc.tweakify.client;

import java.util.Locale;

/**
 * 药水效果剩余时间的紧凑格式：秒 / 分:秒 / 2h05 / 2d05。
 *
 * <p>向上取整，保证效果仍存在时不会提前显示 0；无尽效果由调用方跳过，不进入这里。
 * 最长只输出 5 个字符（例如 {@code 99d23}、{@code 100d}），避免长时间效果把文字压得过小。
 */
public final class EffectDurationFormatter {
	// 1 秒 = 20 tick，与原版药水时间单位一致。
	private static final int TICKS_PER_SECOND = 20;

	private EffectDurationFormatter() {
	}

	public static String format(int ticks) {
		int totalSeconds = Math.max(0, (int) Math.ceil(Math.max(0, ticks) / (double) TICKS_PER_SECOND));
		int days = totalSeconds / 86400;
		int hours = (totalSeconds % 86400) / 3600;
		int minutes = (totalSeconds % 3600) / 60;
		int seconds = totalSeconds % 60;
		// 长时间效果使用小时/天单位，避免 6 位以上数字挤在图标底部。
		if (days > 0) {
			return days >= 100
				? String.format(Locale.ROOT, "%dd", days)
				: String.format(Locale.ROOT, "%dd%02d", days, hours);
		}
		if (hours > 0) {
			return String.format(Locale.ROOT, "%dh%02d", hours, minutes);
		}
		if (minutes > 0) {
			return String.format(Locale.ROOT, "%d:%02d", minutes, seconds);
		}
		return Integer.toString(seconds);
	}
}
