package com.gtzc.tweakify.client;

import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import fi.dy.masa.malilib.config.options.ConfigHotkey;

/**
 * 「测试」标签的配置项声明。
 *
 * <p>字段通过 {@link TweakifyModConfigOptions} 反射按名字收集，界面顺序由
 * {@code ConfigCatalog} 决定，本文件的声明顺序不影响界面与命令顺序。
 * 新增配置项时在这里加字段，并在 {@code ConfigCatalog} 登记分类。
 */
@SuppressWarnings("unused")
public final class TestingConfigOptions {
	private TestingConfigOptions() {
	}

	// 仅由“测试”标签追加显示的界面操作项：打开配置档案编辑器。
	// 使用 ConfigBoolean 以复用 MaLiLib 的配置行布局；条目控件会移除其原生开关/重置按钮，只保留“打开”。
	public static final ConfigBoolean PROFILE_EDITOR = new ConfigBoolean("profileEditor", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 调试信息叠加层：HUD 左下角显示运行时信息。
	public static final ConfigBooleanHotkeyed DEBUG_INFO_OVERLAY = new ConfigBooleanHotkeyed(
		"debugInfoOverlay", false, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean DEBUG_INFO_SHOW_PERFORMANCE = new ConfigBoolean(
		"debugInfoShowPerformance", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean DEBUG_INFO_SHOW_TARGET = new ConfigBoolean(
		"debugInfoShowTarget", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean DEBUG_INFO_SHOW_BIOME = new ConfigBoolean(
		"debugInfoShowBiome", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean DEBUG_INFO_SHOW_SPEED = new ConfigBoolean(
		"debugInfoShowSpeed", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean DEBUG_INFO_SHOW_ENTITIES = new ConfigBoolean(
		"debugInfoShowEntities", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean DEBUG_INFO_SHOW_CONFIG_STATS = new ConfigBoolean(
		"debugInfoShowConfigStats", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean DEBUG_INFO_SHOW_EFFECTS = new ConfigBoolean(
		"debugInfoShowEffects", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean DEBUG_INFO_SHOW_HOOKS = new ConfigBoolean(
		"debugInfoShowHooks", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 探针报表：纯快捷键动作，把已激活的 Mixin 行为探针输出到聊天栏。
	public static final ConfigHotkey DEBUG_PROBE_REPORT = new ConfigHotkey(
		"debugProbeReport", "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	// 探针输出到剪贴板而非聊天栏。
	public static final ConfigBoolean DEBUG_PROBE_REPORT_COPY = new ConfigBoolean(
		"debugProbeReportCopy", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 已启用功能报表：纯快捷键动作，把启用的功能根开关输出到聊天栏。
	public static final ConfigHotkey DEBUG_ENABLED_FEATURES_REPORT = new ConfigHotkey(
		"debugEnabledFeaturesReport", "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	// 已启用功能报表输出到剪贴板。
	public static final ConfigBoolean DEBUG_ENABLED_FEATURES_REPORT_COPY = new ConfigBoolean(
		"debugEnabledFeaturesReportCopy", false).apply(ConfigOptionKeys.GENERAL_KEY);
}
