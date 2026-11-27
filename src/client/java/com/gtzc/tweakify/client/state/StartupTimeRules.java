package com.gtzc.tweakify.client.state;

import java.util.Locale;

/**
 * 启动耗时的显示格式：保留一位小数，固定使用点号（不随系统 Locale 变成逗号）。
 */
public final class StartupTimeRules {
	private StartupTimeRules() {
	}

	public static String formatSeconds(long uptimeMillis) {
		return String.format(Locale.ROOT, "%.1f", Math.max(0L, uptimeMillis) / 1000.0);
	}
}
