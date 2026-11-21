package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
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

	// MC-577：物品栏里绑定到鼠标键的关闭/丢弃按键生效。
	public static final ConfigBoolean FIX_MOUSE_BUTTON_INVENTORY = new ConfigBoolean(
		"fixMouseButtonInventory", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// MC-247973：聊天消息触发的屏蔽名单拉取改为后台执行。
	public static final ConfigBoolean CHAT_LAG_FIX = new ConfigBoolean(
		"chatLagFix", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// MC-79545：等级过高时经验条仍然显示。
	public static final ConfigBoolean EXPERIENCE_BAR_HIGH_LEVEL_FIX = new ConfigBoolean(
		"experienceBarHighLevelFix", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// MC-61489：书本查看/编辑界面垂直居中。
	public static final ConfigBoolean BOOK_GUI_VERTICAL_CENTER_FIX = new ConfigBoolean(
		"bookGuiVerticalCenterFix", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// MC-84661 / MC-170462：药水效果分类修复。
	public static final ConfigBoolean POTION_TOOLTIP_CATEGORY_FIX = new ConfigBoolean(
		"potionTooltipCategoryFix", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// MC-105068：举盾受伤音效修复。
	public static final ConfigBoolean SHIELD_BLOCK_HURT_SOUND_FIX = new ConfigBoolean(
		"shieldBlockHurtSoundFix", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// MC-217716：旁观者覆盖层修复。
	public static final ConfigBoolean SPECTATOR_OVERLAY_FIX = new ConfigBoolean(
		"spectatorOverlayFix", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// MC-268420：冷却指示器闪烁修复。
	public static final ConfigBoolean COOLDOWN_INDICATOR_FIX = new ConfigBoolean(
		"cooldownIndicatorFix", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// MC-4490：第三人称鱼线修复。
	public static final ConfigBoolean FISHING_LINE_THIRD_PERSON_FIX = new ConfigBoolean(
		"fishingLineThirdPersonFix", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// MC-143474：重生快捷栏修复。
	public static final ConfigBoolean RESPAWN_HOTBAR_FIX = new ConfigBoolean(
		"respawnHotbarFix", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// MC-159163：潜行动画重复修复。
	public static final ConfigBoolean SNEAK_ANIMATION_FIX = new ConfigBoolean(
		"sneakAnimationFix", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// MC-121706：骷髅瞄准修复。
	public static final ConfigBoolean SKELETON_AIMING_FIX = new ConfigBoolean(
		"skeletonAimingFix", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// MC-8187：2×2 树苗生长修复。
	public static final ConfigBoolean SAPLING_GROWTH_FIX = new ConfigBoolean(
		"saplingGrowthFix", false).apply(ConfigOptionKeys.GENERAL_KEY);
}
