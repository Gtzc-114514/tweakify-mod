package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigInteger;

/**
 * 「优化」标签的配置项声明。
 *
 * <p>字段通过 {@link TweakifyModConfigOptions} 反射按名字收集，界面顺序由
 * {@code ConfigCatalog} 决定，本文件的声明顺序不影响界面与命令顺序。
 * 新增配置项时在这里加字段，并在 {@code ConfigCatalog} 登记分类。
 */
@SuppressWarnings("unused")
public final class OptimizationConfigOptions {
	private OptimizationConfigOptions() {
	}

	// MiniHUD 掉帧优化：放宽 HUD 行刷新间隔，减少重复重算。
	public static final ConfigBoolean MINI_HUD_FPS_OPTIMIZATION = new ConfigBoolean(
		"miniHudFpsOptimization", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 告示牌文本优化：缓存原始/过滤文本状态并跳过客户端告示牌空 tick。
	public static final ConfigBoolean SIGN_TEXT_OPTIMIZATION = new ConfigBoolean(
		"signTextOptimization", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean SIGN_TEXT_OPTIMIZATION_CACHED_TEXT = new ConfigBoolean(
		"signTextOptimizationCachedText", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean SIGN_TEXT_OPTIMIZATION_SKIP_TICKING = new ConfigBoolean(
		"signTextOptimizationSkipTicking", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean SIGN_TEXT_OPTIMIZATION_SKIP_FILTERING = new ConfigBoolean(
		"signTextOptimizationSkipFiltering", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// HUD 行刷新间隔（毫秒，50 ~ 1000）。
	public static final ConfigInteger MINI_HUD_REFRESH_INTERVAL_MS = new ConfigInteger(
		"miniHudRefreshIntervalMs", TweakifyModConfig.DEFAULT_MINIHUD_REFRESH_INTERVAL_MS,
		TweakifyModConfig.MIN_MINIHUD_REFRESH_INTERVAL_MS,
		TweakifyModConfig.MAX_MINIHUD_REFRESH_INTERVAL_MS).apply(ConfigOptionKeys.GENERAL_KEY);
}
