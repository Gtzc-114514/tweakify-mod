package com.gtzc.tweakify.client.config;

import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.config.options.ConfigOptionList;

/**
 * 「通用」标签的配置项声明。
 *
 * <p>字段通过 {@link TweakifyModConfigOptions} 反射按名字收集，界面顺序由
 * {@code ConfigCatalog} 决定，本文件的声明顺序不影响界面与命令顺序。
 * 新增配置项时在这里加字段，并在 {@code ConfigCatalog} 登记分类。
 */
@SuppressWarnings("unused")
public final class GeneralConfigOptions {
	private GeneralConfigOptions() {
	}

	// 打开配置界面的快捷键，默认 G + C，置顶显示。
	public static final ConfigOptionList LANGUAGE = new ConfigOptionList(
		"language", ModLanguage.ZH_CN).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean FOLLOW_SYSTEM_LANGUAGE = new ConfigBoolean(
		"followSystemLanguage", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// 标签记忆：打开配置界面时停在上次选择的分类标签。
	public static final ConfigBoolean REMEMBER_SELECTED_TAB = new ConfigBoolean(
		"rememberSelectedTab", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 未记忆时的默认标签（关闭标签记忆或首次打开）。
	public static final ConfigOptionList REMEMBER_SELECTED_TAB_FALLBACK = new ConfigOptionList(
		"rememberSelectedTabFallback", ConfigGuiTab.ALL).apply(ConfigOptionKeys.GENERAL_KEY);

	// 收藏系统总开关：关闭后隐藏配置行与存档列表的收藏星。
	public static final ConfigBoolean FAVORITE_SYSTEM = new ConfigBoolean(
		"favoriteSystem", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// 收藏星样式：自绘星形 / 原版下界之星，同时作用于配置行与存档列表的收藏星。
	public static final ConfigOptionList FAVORITE_STAR_STYLE = new ConfigOptionList(
		"favoriteStarStyle", FavoriteStarStyle.STAR).apply(ConfigOptionKeys.GENERAL_KEY);

	// 收藏包含子项：收藏父项时，它的全部子项也显示在收藏标签中。
	public static final ConfigBoolean FAVORITE_INCLUDE_CHILDREN = new ConfigBoolean(
		"favoriteIncludeChildren", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigHotkey OPEN_CONFIG_GUI = new ConfigHotkey(
		"openConfigGui", "G,C", ConfigOptionKeys.OPEN_CONFIG_GUI_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	// 滚动列表记忆：关闭界面时记住滚动位置，下次打开回到该位置。
	public static final ConfigBoolean SCROLL_LIST_MEMORY = new ConfigBoolean(
		"scrollListMemory", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean SCROLL_LIST_MEMORY_PERSIST = new ConfigBoolean(
		"scrollListMemoryPersist", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean MALILIB_SCROLL_MEMORY = new ConfigBoolean(
		"malilibScrollMemory", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean MOD_MENU_SCROLL_MEMORY = new ConfigBoolean(
		"modMenuScrollMemory", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// 配置界面搜索：命中的关键词在条目名称与配置键上高亮。
	public static final ConfigBoolean SEARCH_HIGHLIGHT = new ConfigBoolean(
		"searchHighlight", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// 搜索命中的文字颜色。
	public static final ConfigOptionList SEARCH_HIGHLIGHT_COLOR = new ConfigOptionList(
		"searchHighlightColor", SearchHighlightColor.GOLD).apply(ConfigOptionKeys.GENERAL_KEY);

	// 平滑滚动：配置界面列表、创造物品栏、聊天栏与 Mod Menu 列表共用总开关。
	public static final ConfigBoolean SMOOTH_SCROLL = new ConfigBoolean(
		"smoothScroll", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// 子开关：按界面分别启用。
	public static final ConfigBoolean SMOOTH_SCROLL_CONFIG_LIST = new ConfigBoolean(
		"smoothScrollConfigList", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean SMOOTH_SCROLL_CREATIVE = new ConfigBoolean(
		"smoothScrollCreative", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean SMOOTH_SCROLL_CHAT = new ConfigBoolean(
		"smoothScrollChat", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean SMOOTH_SCROLL_MOD_MENU = new ConfigBoolean(
		"smoothScrollModMenu", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean SMOOTH_SCROLL_ADVANCEMENTS = new ConfigBoolean(
		"smoothScrollAdvancements", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// 平滑时长（毫秒）：越大越柔和、越小越跟手。
	public static final ConfigInteger SMOOTH_SCROLL_DURATION = new ConfigInteger(
		"smoothScrollDuration", TweakifyModConfig.DEFAULT_SMOOTH_SCROLL_DURATION,
		TweakifyModConfig.MIN_SMOOTH_SCROLL_DURATION,
		TweakifyModConfig.MAX_SMOOTH_SCROLL_DURATION).apply(ConfigOptionKeys.GENERAL_KEY);

	// 日志清理：启动时把 logs 目录的旧日志归档清理到指定数量。
	public static final ConfigBooleanHotkeyed LOG_CLEANUP = new ConfigBooleanHotkeyed(
		"logCleanup", false, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	// 保留的最新日志归档数量（0 ~ 100，0 = 删除全部归档），使用滑条调节。
	public static final ConfigInteger LOG_CLEANUP_KEEP_COUNT = new ConfigInteger(
		"logCleanupKeepCount", TweakifyModConfig.DEFAULT_LOG_CLEANUP_KEEP_COUNT,
		TweakifyModConfig.MIN_LOG_CLEANUP_KEEP_COUNT,
		TweakifyModConfig.MAX_LOG_CLEANUP_KEEP_COUNT, true).apply(ConfigOptionKeys.GENERAL_KEY);

	// 启动时自动执行一次清理。
	public static final ConfigBoolean LOG_CLEANUP_ON_STARTUP = new ConfigBoolean(
		"logCleanupOnStartup", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// 仅界面操作项：立刻清理一次，不写入配置（与档案编辑器同一种接线方式）。
	public static final ConfigBoolean LOG_CLEANUP_NOW = new ConfigBoolean(
		"logCleanupNow", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 启动时间：加载完成后显示本次启动耗时。
	public static final ConfigBoolean STARTUP_TIME = new ConfigBoolean(
		"startupTime", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// 同时把启动耗时写入日志。
	public static final ConfigBoolean STARTUP_TIME_LOG = new ConfigBoolean(
		"startupTimeLog", true).apply(ConfigOptionKeys.GENERAL_KEY);
}
