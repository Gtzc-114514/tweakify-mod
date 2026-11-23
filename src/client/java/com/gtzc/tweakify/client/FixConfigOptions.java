package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigDouble;
import fi.dy.masa.malilib.config.options.ConfigInteger;

/**
 * 「修复」标签的配置项声明。
 *
 * <p>字段通过 {@link TweakifyModConfigOptions} 反射按名字收集，界面顺序由
 * {@code ConfigCatalog} 决定，本文件的声明顺序不影响界面与命令顺序。
 * 新增配置项时在这里加字段，并在 {@code ConfigCatalog} 登记分类。
 */
@SuppressWarnings("unused")
public final class FixConfigOptions {
	private FixConfigOptions() {
	}

	// 草径卡船修复：土径对船使用满方块碰撞。
	public static final ConfigBoolean FIX_BOAT_STUCK_ON_DIRT_PATH = new ConfigBoolean(
		"fixBoatStuckOnDirtPath", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 只对载有玩家的船只生效。
	public static final ConfigBoolean FIX_BOAT_STUCK_ON_DIRT_PATH_PLAYER_ONLY = new ConfigBoolean(
		"fixBoatStuckOnDirtPathPlayerOnly", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 鹦鹉肩膀跳舞：唱片机播放音乐时，肩膀上的鹦鹉也跳舞。
	public static final ConfigBoolean PARROT_DANCE_ON_SHOULDER = new ConfigBoolean(
		"parrotDanceOnShoulder", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 只让本地玩家肩膀上的鹦鹉跳舞。
	public static final ConfigBoolean PARROT_DANCE_ON_SHOULDER_LOCAL_PLAYER_ONLY = new ConfigBoolean(
		"parrotDanceOnShoulderLocalPlayerOnly", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 更好的粗体：粗体偏移从 1.0 改为 0.5。
	public static final ConfigBoolean BETTER_BOLD = new ConfigBoolean(
		"betterBold", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 粗体偏移百分比（10 ~ 100，默认 50 = 0.5 像素）。
	public static final ConfigInteger BETTER_BOLD_OFFSET_PERCENT = new ConfigInteger(
		"betterBoldOffsetPercent", TweakifyModConfig.DEFAULT_BETTER_BOLD_OFFSET_PERCENT,
		TweakifyModConfig.MIN_BETTER_BOLD_OFFSET_PERCENT,
		TweakifyModConfig.MAX_BETTER_BOLD_OFFSET_PERCENT).apply(ConfigOptionKeys.GENERAL_KEY);

	// 冰霜行者跳跃修复：跳跃途中也继续在脚下结冰。
	public static final ConfigBoolean FROST_WALKER_JUMP_FIX = new ConfigBoolean(
		"frostWalkerJumpFix", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 只在向上跳跃时放宽。
	public static final ConfigBoolean FROST_WALKER_JUMP_FIX_JUMP_ONLY = new ConfigBoolean(
		"frostWalkerJumpFixJumpOnly", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean FROST_WALKER_JUMP_FIX_SPRINT_ONLY = new ConfigBoolean(
		"frostWalkerJumpFixSprintOnly", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// MC-577：物品栏里绑定到鼠标键的关闭/丢弃按键生效。
	public static final ConfigBoolean FIX_MOUSE_BUTTON_INVENTORY = new ConfigBoolean(
		"fixMouseButtonInventory", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 鼠标键关闭背包。
	public static final ConfigBoolean MOUSE_BUTTON_INVENTORY_CLOSE = new ConfigBoolean(
		"mouseButtonInventoryClose", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// 鼠标键丢弃悬停物品。
	public static final ConfigBoolean MOUSE_BUTTON_INVENTORY_DROP = new ConfigBoolean(
		"mouseButtonInventoryDrop", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// MC-247973：聊天消息触发的屏蔽名单拉取改为后台执行。
	public static final ConfigBoolean CHAT_LAG_FIX = new ConfigBoolean(
		"chatLagFix", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 异步拉取屏蔽名单的超时时间（秒）。
	public static final ConfigInteger CHAT_LAG_FIX_TIMEOUT_SECONDS = new ConfigInteger(
		"chatLagFixTimeoutSeconds", TweakifyModConfig.DEFAULT_CHAT_LAG_TIMEOUT_SECONDS,
		TweakifyModConfig.MIN_CHAT_LAG_TIMEOUT_SECONDS,
		TweakifyModConfig.MAX_CHAT_LAG_TIMEOUT_SECONDS).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigInteger CHAT_LAG_FIX_CACHE_SECONDS = new ConfigInteger(
		"chatLagFixCacheSeconds", TweakifyModConfig.DEFAULT_CHAT_LAG_CACHE_SECONDS,
		TweakifyModConfig.MIN_CHAT_LAG_CACHE_SECONDS,
		TweakifyModConfig.MAX_CHAT_LAG_CACHE_SECONDS).apply(ConfigOptionKeys.GENERAL_KEY);

	// MC-79545：等级过高时经验条仍然显示。
	public static final ConfigBoolean EXPERIENCE_BAR_HIGH_LEVEL_FIX = new ConfigBoolean(
		"experienceBarHighLevelFix", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 只在等级不低于该值时修复。
	public static final ConfigInteger EXPERIENCE_BAR_HIGH_LEVEL_FIX_MIN_LEVEL = new ConfigInteger(
		"experienceBarHighLevelFixMinLevel", TweakifyModConfig.DEFAULT_XP_HIGH_LEVEL_FIX_LEVEL,
		TweakifyModConfig.MIN_XP_HIGH_LEVEL_FIX_LEVEL,
		TweakifyModConfig.MAX_XP_HIGH_LEVEL_FIX_LEVEL).apply(ConfigOptionKeys.GENERAL_KEY);

	// MC-61489：书本查看/编辑界面垂直居中。
	public static final ConfigBoolean BOOK_GUI_VERTICAL_CENTER_FIX = new ConfigBoolean(
		"bookGuiVerticalCenterFix", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 只对书本查看界面生效。
	public static final ConfigBoolean BOOK_GUI_CENTER_VIEW_SCREEN = new ConfigBoolean(
		"bookGuiCenterViewScreen", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// 只对书与笔编辑界面生效。
	public static final ConfigBoolean BOOK_GUI_CENTER_EDIT_SCREEN = new ConfigBoolean(
		"bookGuiCenterEditScreen", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// MC-84661 / MC-170462：药水效果分类修复。
	public static final ConfigBoolean POTION_TOOLTIP_CATEGORY_FIX = new ConfigBoolean(
		"potionTooltipCategoryFix", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean POTION_TOOLTIP_CATEGORY_GLOWING = new ConfigBoolean(
		"potionTooltipCategoryGlowing", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean POTION_TOOLTIP_CATEGORY_BAD_OMEN = new ConfigBoolean(
		"potionTooltipCategoryBadOmen", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// MC-105068：举盾受伤音效修复。
	public static final ConfigBoolean SHIELD_BLOCK_HURT_SOUND_FIX = new ConfigBoolean(
		"shieldBlockHurtSoundFix", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean SHIELD_BLOCK_HURT_SOUND_FIX_PLAYERS_ONLY = new ConfigBoolean(
		"shieldBlockHurtSoundFixPlayersOnly", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// MC-217716：旁观者覆盖层修复。
	public static final ConfigBoolean SPECTATOR_OVERLAY_FIX = new ConfigBoolean(
		"spectatorOverlayFix", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean SPECTATOR_OVERLAY_HIDE_PORTAL = new ConfigBoolean(
		"spectatorOverlayHidePortal", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean SPECTATOR_OVERLAY_HIDE_NAUSEA = new ConfigBoolean(
		"spectatorOverlayHideNausea", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean SPECTATOR_OVERLAY_HIDE_SPINNING = new ConfigBoolean(
		"spectatorOverlayHideSpinning", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// MC-268420：冷却指示器闪烁修复。
	public static final ConfigBoolean COOLDOWN_INDICATOR_FIX = new ConfigBoolean(
		"cooldownIndicatorFix", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigInteger COOLDOWN_INDICATOR_FIX_MIN_PROGRESS = new ConfigInteger(
		"cooldownIndicatorFixMinProgress", TweakifyModConfig.DEFAULT_COOLDOWN_INDICATOR_PROGRESS,
		TweakifyModConfig.MIN_COOLDOWN_INDICATOR_PROGRESS,
		TweakifyModConfig.MAX_COOLDOWN_INDICATOR_PROGRESS).apply(ConfigOptionKeys.GENERAL_KEY);

	// MC-4490：第三人称鱼线修复。
	public static final ConfigBoolean FISHING_LINE_THIRD_PERSON_FIX = new ConfigBoolean(
		"fishingLineThirdPersonFix", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigDouble FISHING_LINE_THIRD_PERSON_FIX_OFFSET = new ConfigDouble(
		"fishingLineThirdPersonFixOffset", TweakifyModConfig.DEFAULT_FISHING_LINE_FIX_OFFSET,
		TweakifyModConfig.MIN_FISHING_LINE_FIX_OFFSET,
		TweakifyModConfig.MAX_FISHING_LINE_FIX_OFFSET).apply(ConfigOptionKeys.GENERAL_KEY);

	// MC-143474：重生快捷栏修复。
	public static final ConfigBoolean RESPAWN_HOTBAR_FIX = new ConfigBoolean(
		"respawnHotbarFix", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 维度切换时也保留快捷栏槽位。
	public static final ConfigBoolean RESPAWN_HOTBAR_KEEP_ON_DIMENSION_CHANGE = new ConfigBoolean(
		"respawnHotbarKeepOnDimensionChange", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// MC-159163：潜行动画重复修复。
	public static final ConfigBoolean SNEAK_ANIMATION_FIX = new ConfigBoolean(
		"sneakAnimationFix", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean SNEAK_ANIMATION_FIX_ONLY_ON_GROUND = new ConfigBoolean(
		"sneakAnimationFixOnlyOnGround", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean SNEAK_ANIMATION_FIX_ONLY_WHEN_SNEAKING = new ConfigBoolean(
		"sneakAnimationFixOnlyWhenSneaking", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// MC-121706：骷髅瞄准修复。
	public static final ConfigBoolean SKELETON_AIMING_FIX = new ConfigBoolean(
		"skeletonAimingFix", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 只修正骷髅类，不处理幻术师。
	public static final ConfigBoolean SKELETON_AIMING_ONLY_SKELETONS = new ConfigBoolean(
		"skeletonAimingOnlySkeletons", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean SKELETON_AIMING_ONLY_TARGET_ABOVE = new ConfigBoolean(
		"skeletonAimingOnlyTargetAbove", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// MC-8187：2×2 树苗生长修复。
	public static final ConfigBoolean SAPLING_GROWTH_FIX = new ConfigBoolean(
		"saplingGrowthFix", false).apply(ConfigOptionKeys.GENERAL_KEY);
}
