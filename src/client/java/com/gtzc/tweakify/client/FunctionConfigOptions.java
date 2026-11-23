package com.gtzc.tweakify.client;

import com.google.common.collect.ImmutableList;
import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import fi.dy.masa.malilib.config.options.ConfigDouble;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import fi.dy.masa.malilib.config.options.ConfigStringList;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;

/**
 * 「功能」标签的配置项声明。
 *
 * <p>字段通过 {@link TweakifyModConfigOptions} 反射按名字收集，界面顺序由
 * {@code ConfigCatalog} 决定，本文件的声明顺序不影响界面与命令顺序。
 * 新增配置项时在这里加字段，并在 {@code ConfigCatalog} 登记分类。
 */
@SuppressWarnings("unused")
public final class FunctionConfigOptions {
	private FunctionConfigOptions() {
	}

	// 复制物体 ID：开关默认关闭，条目自带的快捷键用于切换开关；
	// 真正执行复制的快捷键是它下面的子项 copyTargetId。
	public static final ConfigBooleanHotkeyed COPY_ITEM_ID = new ConfigBooleanHotkeyed(
		"copyItemId", false, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigHotkey COPY_TARGET_ID_HOTKEY = new ConfigHotkey(
		"copyTargetId", "", ConfigOptionKeys.COPY_ID_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean BLOCK_OFFHAND_FALLBACK = new ConfigBoolean(
		"blockOffhandFallback", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean INTERCEPT_BLOCK_INTERACTION = new ConfigBoolean(
		"interceptBlockInteraction", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean INTERCEPT_ENTITY_INTERACTION = new ConfigBoolean(
		"interceptEntityInteraction", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBooleanHotkeyed MINING_COOLDOWN = new ConfigBooleanHotkeyed(
		"miningCooldown", false, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean MINING_COOLDOWN_ATTACKS = new ConfigBoolean(
		"miningCooldownAttacks", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 一次挖掘/攻击成功后额外锁定的时间（tick）。
	public static final ConfigInteger MINING_COOLDOWN_COOLDOWN_TICKS = new ConfigInteger(
		"miningCooldownCooldownTicks", TweakifyModConfig.DEFAULT_MINING_COOLDOWN_TICKS,
		TweakifyModConfig.MIN_MINING_COOLDOWN_TICKS,
		TweakifyModConfig.MAX_MINING_COOLDOWN_TICKS).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBooleanHotkeyed USE_COOLDOWN = new ConfigBooleanHotkeyed(
		"useCooldown", true, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean MAIN_HAND_USE_COOLDOWN = new ConfigBoolean(
		"mainHandUseCooldown", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean OFFHAND_USE_COOLDOWN = new ConfigBoolean(
		"offhandUseCooldown", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBooleanHotkeyed DISABLE_FLIGHT_DRIFT = new ConfigBooleanHotkeyed(
		"disableFlightDrift", false, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean FLIGHT_DRIFT_VERTICAL = new ConfigBoolean(
		"flightDriftVertical", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 只对创造模式飞行生效。
	public static final ConfigBoolean FLIGHT_DRIFT_ONLY_CREATIVE = new ConfigBoolean(
		"flightDriftOnlyCreative", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBooleanHotkeyed DISABLE_DIMENSION_TELEPORT = new ConfigBooleanHotkeyed(
		"disableDimensionTeleport", false, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean DIMENSION_TELEPORT_PLAYERS_ONLY = new ConfigBoolean(
		"dimensionTeleportPlayersOnly", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 地狱门传送延迟：生存模式可秒传，创造模式可改为等待生存延迟。
	public static final ConfigBooleanHotkeyed NETHER_PORTAL_TELEPORT_DELAY = new ConfigBooleanHotkeyed(
		"netherPortalTeleportDelay", false, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean NETHER_PORTAL_SURVIVAL_INSTANT_TELEPORT = new ConfigBoolean(
		"netherPortalSurvivalInstantTeleport", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean NETHER_PORTAL_CREATIVE_DELAYED_TELEPORT = new ConfigBoolean(
		"netherPortalCreativeDelayedTeleport", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 自定义等待时间（tick）：0 = 使用上面的子开关与原版规则。
	public static final ConfigInteger NETHER_PORTAL_TELEPORT_DELAY_TICKS = new ConfigInteger(
		"netherPortalTeleportDelayTicks", TweakifyModConfig.DEFAULT_NETHER_PORTAL_DELAY_TICKS,
		TweakifyModConfig.MIN_NETHER_PORTAL_DELAY_TICKS,
		TweakifyModConfig.MAX_NETHER_PORTAL_DELAY_TICKS).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean DISABLE_NETHER_PORTAL_TELEPORT = new ConfigBoolean(
		"disableNetherPortalTeleport", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean DISABLE_END_PORTAL_TELEPORT = new ConfigBoolean(
		"disableEndPortalTeleport", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBooleanHotkeyed HIGHLIGHT_SINGLE_LAYER_BEDROCK = new ConfigBooleanHotkeyed(
		"highlightSingleLayerBedrock", false, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigInteger HIGHLIGHT_SINGLE_LAYER_BEDROCK_MAX_MARKERS = new ConfigInteger(
		"highlightSingleLayerBedrockMaxMarkers", TweakifyModConfig.DEFAULT_BEDROCK_MARKERS,
		TweakifyModConfig.MIN_BEDROCK_MARKERS,
		TweakifyModConfig.MAX_BEDROCK_MARKERS).apply(ConfigOptionKeys.GENERAL_KEY);

	// 基岩扫描半径（8 ~ 32 格）与标记颜色。
	public static final ConfigInteger HIGHLIGHT_SINGLE_LAYER_BEDROCK_SCAN_RADIUS = new ConfigInteger(
		"highlightSingleLayerBedrockScanRadius", TweakifyModConfig.DEFAULT_BEDROCK_SCAN_RADIUS,
		TweakifyModConfig.MIN_BEDROCK_SCAN_RADIUS,
		TweakifyModConfig.MAX_BEDROCK_SCAN_RADIUS).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigOptionList HIGHLIGHT_SINGLE_LAYER_BEDROCK_COLOR = new ConfigOptionList(
		"highlightSingleLayerBedrockColor", BedrockMarkerColor.GOLD).apply(ConfigOptionKeys.GENERAL_KEY);

	// 屏障可见：不手持屏障物品也能看到周围屏障，条目自带快捷键用于切换开关。
	public static final ConfigBooleanHotkeyed BARRIER_VISIBILITY = new ConfigBooleanHotkeyed(
		"barrierVisibility", false, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	// 单次扫描最多保留的屏障标记数量。
	public static final ConfigInteger BARRIER_VISIBILITY_MAX_MARKERS = new ConfigInteger(
		"barrierVisibilityMaxMarkers", TweakifyModConfig.DEFAULT_BARRIER_MARKERS,
		TweakifyModConfig.MIN_BARRIER_MARKERS,
		TweakifyModConfig.MAX_BARRIER_MARKERS).apply(ConfigOptionKeys.GENERAL_KEY);

	// 扫描半径（8 ~ 32 格）与同一方块两次标记粒子的间隔（20 ~ 200 tick）。
	public static final ConfigInteger BARRIER_VISIBILITY_SCAN_RADIUS = new ConfigInteger(
		"barrierVisibilityScanRadius", TweakifyModConfig.DEFAULT_BARRIER_SCAN_RADIUS,
		TweakifyModConfig.MIN_BARRIER_SCAN_RADIUS,
		TweakifyModConfig.MAX_BARRIER_SCAN_RADIUS).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigInteger BARRIER_VISIBILITY_PARTICLE_INTERVAL = new ConfigInteger(
		"barrierVisibilityParticleInterval", TweakifyModConfig.DEFAULT_BARRIER_PARTICLE_INTERVAL,
		TweakifyModConfig.MIN_BARRIER_PARTICLE_INTERVAL,
		TweakifyModConfig.MAX_BARRIER_PARTICLE_INTERVAL).apply(ConfigOptionKeys.GENERAL_KEY);

	// 装备耐久显示：耐久条上叠加耐久百分比 / 剩余耐久数值，显示内容由子项切换。
	public static final ConfigBooleanHotkeyed DURABILITY_OVERLAY = new ConfigBooleanHotkeyed(
		"durabilityOverlay", false, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigOptionList DURABILITY_DISPLAY_MODE = new ConfigOptionList(
		"durabilityDisplayMode", DurabilityDisplayMode.PERCENT).apply(ConfigOptionKeys.GENERAL_KEY);

	// 耐久文字配色：自动分档 / 固定白、黄、红。
	public static final ConfigOptionList DURABILITY_OVERLAY_COLOR_MODE = new ConfigOptionList(
		"durabilityOverlayColorMode", DurabilityColorMode.AUTO).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBooleanHotkeyed DISABLE_SHOVEL_SNOW_FULL_BREAK = new ConfigBooleanHotkeyed(
		"disableShovelSnowFullBreak", false, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	// 相机运动：参考 CameraOverhaul 的转向/横移/俯仰/静止晃动与屏幕震动。
	public static final ConfigBooleanHotkeyed CAMERA_MOTION = new ConfigBooleanHotkeyed(
		"cameraMotion", false, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigInteger CAMERA_MOTION_INTENSITY = new ConfigInteger(
		"cameraMotionIntensity", TweakifyModConfig.DEFAULT_CAMERA_MOTION_INTENSITY,
		TweakifyModConfig.MIN_CAMERA_MOTION_INTENSITY,
		TweakifyModConfig.MAX_CAMERA_MOTION_INTENSITY).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean CAMERA_MOTION_TURNING_ROLL = new ConfigBoolean(
		"cameraMotionTurningRoll", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean CAMERA_MOTION_STRAFING_ROLL = new ConfigBoolean(
		"cameraMotionStrafingRoll", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean CAMERA_MOTION_FORWARD_PITCH = new ConfigBoolean(
		"cameraMotionForwardPitch", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean CAMERA_MOTION_VERTICAL_PITCH = new ConfigBoolean(
		"cameraMotionVerticalPitch", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean CAMERA_MOTION_IDLE_SWAY = new ConfigBoolean(
		"cameraMotionIdleSway", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean CAMERA_MOTION_SCREEN_SHAKE = new ConfigBoolean(
		"cameraMotionScreenShake", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean CAMERA_MOTION_MOUSE_SMOOTHING = new ConfigBoolean(
		"cameraMotionMouseSmoothing", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean CAMERA_MOTION_FIRST_PERSON = new ConfigBoolean(
		"cameraMotionFirstPerson", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean CAMERA_MOTION_THIRD_PERSON = new ConfigBoolean(
		"cameraMotionThirdPerson", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// 物品分组：同类物品折叠成组图标，点击展开/收起。
	public static final ConfigBooleanHotkeyed ITEM_GROUPS = new ConfigBooleanHotkeyed(
		"itemGroups", false, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigOptionList ITEM_GROUPS_SORT = new ConfigOptionList(
		"itemGroupsSort", ItemGroupSort.DEFAULT).apply(ConfigOptionKeys.GENERAL_KEY);

	// 展开时图标轮播组内物品（对应参考模组的 showItemsInGroup）。
	public static final ConfigBoolean ITEM_GROUPS_SHOW_ITEMS = new ConfigBoolean(
		"itemGroupsShowItems", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 是否启用内置默认组；关闭后只用自定义组（对应参考模组的 Add all groups）。
	public static final ConfigBoolean ITEM_GROUPS_ADD_ALL = new ConfigBoolean(
		"itemGroupsAddAll", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// 自定义组：每行 tabId|组名|包含|排除|等价。
	public static final ConfigStringList CUSTOM_ITEM_GROUPS = new ConfigStringList(
		"customItemGroups", ImmutableList.of()).apply(ConfigOptionKeys.LISTS_KEY);

	// 定位栏优化：把定位栏圆点换成本服玩家头像（参考 locator-heads）。
	public static final ConfigBooleanHotkeyed LOCATOR_BAR_HEADS = new ConfigBooleanHotkeyed(
		"locatorBarHeads", false, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	// 经验条与定位栏同时显示（参考 alwaysShowXP）。
	public static final ConfigBoolean LOCATOR_BAR_ALWAYS_SHOW_XP = new ConfigBoolean(
		"locatorBarAlwaysShowXp", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigInteger LOCATOR_BAR_HEAD_SCALE = new ConfigInteger(
		"locatorBarHeadScale", TweakifyModConfig.DEFAULT_LOCATOR_HEAD_SIZE,
		TweakifyModConfig.MIN_LOCATOR_HEAD_SIZE,
		TweakifyModConfig.MAX_LOCATOR_HEAD_SIZE).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean LOCATOR_BAR_HEAD_HAT_LAYER = new ConfigBoolean(
		"locatorBarHeadHatLayer", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// 描边：样式（关/固定色/队伍色）、粗细与固定颜色。
	public static final ConfigOptionList LOCATOR_BAR_HEAD_BORDER = new ConfigOptionList(
		"locatorBarHeadBorder", LocatorHeadBorder.OFF).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigOptionList LOCATOR_BAR_HEAD_BORDER_THICKNESS = new ConfigOptionList(
		"locatorBarHeadBorderThickness", LocatorHeadBorderThickness.NORMAL).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigOptionList LOCATOR_BAR_HEAD_BORDER_COLOR = new ConfigOptionList(
		"locatorBarHeadBorderColor", LocatorHeadColor.WHITE).apply(ConfigOptionKeys.GENERAL_KEY);

	// 玩家名字时机与玩家过滤。
	public static final ConfigOptionList LOCATOR_BAR_HEAD_NAMES = new ConfigOptionList(
		"locatorBarHeadNames", LocatorHeadNames.NEVER).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigOptionList LOCATOR_BAR_HEAD_FILTER = new ConfigOptionList(
		"locatorBarHeadFilter", LocatorHeadFilter.ALL).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigStringList LOCATOR_BAR_HEAD_INCLUDED_PLAYERS = new ConfigStringList(
		"locatorBarHeadIncludedPlayers", ImmutableList.of()).apply(ConfigOptionKeys.LISTS_KEY);

	public static final ConfigStringList LOCATOR_BAR_HEAD_EXCLUDED_PLAYERS = new ConfigStringList(
		"locatorBarHeadExcludedPlayers", ImmutableList.of()).apply(ConfigOptionKeys.LISTS_KEY);

	public static final ConfigBoolean LOCATOR_BAR_HEAD_NEARBY_ONLY = new ConfigBoolean(
		"locatorBarHeadNearbyOnly", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 指南针覆盖层：开关、颜色、文字阴影与坐标标记。
	public static final ConfigBoolean LOCATOR_BAR_COMPASS = new ConfigBoolean(
		"locatorBarCompass", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigOptionList LOCATOR_BAR_COMPASS_COLOR = new ConfigOptionList(
		"locatorBarCompassColor", LocatorHeadColor.WHITE).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean LOCATOR_BAR_COMPASS_SHADOW = new ConfigBoolean(
		"locatorBarCompassShadow", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean LOCATOR_BAR_COMPASS_COORDINATES = new ConfigBoolean(
		"locatorBarCompassCoordinates", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 鞘翅烟花限制：滑翔中使用烟花后，等加速结束才能再次右键使用。
	public static final ConfigBooleanHotkeyed ELYTRA_FIREWORK_COOLDOWN = new ConfigBooleanHotkeyed(
		"elytraFireworkCooldown", false, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigInteger ELYTRA_FIREWORK_COOLDOWN_TICKS = new ConfigInteger(
		"elytraFireworkCooldownTicks", TweakifyModConfig.MIN_ELYTRA_FIREWORK_TICKS,
		TweakifyModConfig.MIN_ELYTRA_FIREWORK_TICKS, TweakifyModConfig.MAX_ELYTRA_FIREWORK_TICKS).apply(ConfigOptionKeys.GENERAL_KEY);

	// 等待烟花加速结束时的最长锁定时间（tick）。
	public static final ConfigInteger ELYTRA_FIREWORK_COOLDOWN_MAX_TICKS = new ConfigInteger(
		"elytraFireworkCooldownMaxTicks", TweakifyModConfig.DEFAULT_ELYTRA_FIREWORK_MAX_TICKS,
		TweakifyModConfig.MIN_ELYTRA_FIREWORK_MAX_TICKS,
		TweakifyModConfig.MAX_ELYTRA_FIREWORK_MAX_TICKS).apply(ConfigOptionKeys.GENERAL_KEY);

	// 关闭指定生物生成：列表为空时不生效。
	public static final ConfigBooleanHotkeyed DISABLE_SPECIFIED_MOB_SPAWN = new ConfigBooleanHotkeyed(
		"disableSpecifiedMobSpawn", false, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean DISABLED_SPAWN_ALLOW_PLAYER_SPAWN = new ConfigBoolean(
		"disabledSpawnAllowPlayerSpawn", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// 和平模式怪物：列表为空时不生效。
	public static final ConfigBooleanHotkeyed PEACEFUL_MODE_MONSTERS = new ConfigBooleanHotkeyed(
		"peacefulModeMonsters", false, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	// 刷怪蛋投掷：像鸡蛋/雪球一样投掷刷怪蛋，命中后生成生物。
	public static final ConfigBooleanHotkeyed THROW_SPAWN_EGG = new ConfigBooleanHotkeyed(
		"throwSpawnEgg", false, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	// 投掷速度百分比（50 ~ 200）与潜行时保持原版放置。
	public static final ConfigInteger THROW_SPAWN_EGG_VELOCITY = new ConfigInteger(
		"throwSpawnEggVelocity", TweakifyModConfig.DEFAULT_SPAWN_EGG_VELOCITY,
		TweakifyModConfig.MIN_SPAWN_EGG_VELOCITY,
		TweakifyModConfig.MAX_SPAWN_EGG_VELOCITY).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean THROW_SPAWN_EGG_SNEAK_BYPASS = new ConfigBoolean(
		"throwSpawnEggSneakBypass", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean PEACEFUL_MODE_MONSTER_SPAWN = new ConfigBoolean(
		"peacefulModeMonsterSpawn", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean PEACEFUL_MODE_MONSTER_NO_ATTACK = new ConfigBoolean(
		"peacefulModeMonsterNoAttack", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// 聊天框文字框选：聊天界面拖动选择文字并复制。
	public static final ConfigBooleanHotkeyed CHAT_TEXT_SELECTION = new ConfigBooleanHotkeyed(
		"chatTextSelection", false, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	// 生物攻击范围显示：以生物为中心画半透明蓝色圆圈。
	public static final ConfigBooleanHotkeyed ATTACK_RANGE_DISPLAY = new ConfigBooleanHotkeyed(
		"attackRangeDisplay", false, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	// 攻击范围显示方式：平面圆环 / 半透明球体。
	public static final ConfigOptionList ATTACK_RANGE_DISPLAY_MODE = new ConfigOptionList(
		"attackRangeDisplayMode", AttackRangeDisplayMode.FLAT).apply(ConfigOptionKeys.GENERAL_KEY);

	// 攻击范围渲染方式：蓝色线框 / 半透明实心。
	public static final ConfigOptionList ATTACK_RANGE_RENDER_MODE = new ConfigOptionList(
		"attackRangeRenderMode", AttackRangeRenderMode.WIREFRAME).apply(ConfigOptionKeys.GENERAL_KEY);

	// 攻击范围颜色调色板。
	public static final ConfigOptionList ATTACK_RANGE_PALETTE = new ConfigOptionList(
		"attackRangePalette", AttackRangePalette.BLUE).apply(ConfigOptionKeys.GENERAL_KEY);

	// 是否显示本地玩家自己的攻击范围。
	public static final ConfigBoolean ATTACK_RANGE_INCLUDE_SELF = new ConfigBoolean(
		"attackRangeIncludeSelf", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 只显示敌对生物；关闭时保持生物 + 玩家的原逻辑。
	public static final ConfigBoolean ATTACK_RANGE_HOSTILE_ONLY = new ConfigBoolean(
		"attackRangeHostileOnly", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 是否连隐身实体也显示。
	public static final ConfigBoolean ATTACK_RANGE_INCLUDE_INVISIBLE = new ConfigBoolean(
		"attackRangeIncludeInvisible", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 攻击范围的收集距离与单次最多目标数。
	public static final ConfigInteger ATTACK_RANGE_MAX_DISTANCE = new ConfigInteger(
		"attackRangeMaxDistance", TweakifyModConfig.DEFAULT_ATTACK_RANGE_DISTANCE,
		TweakifyModConfig.MIN_ATTACK_RANGE_DISTANCE,
		TweakifyModConfig.MAX_ATTACK_RANGE_DISTANCE).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigInteger ATTACK_RANGE_MAX_TARGETS = new ConfigInteger(
		"attackRangeMaxTargets", TweakifyModConfig.DEFAULT_ATTACK_RANGE_TARGETS,
		TweakifyModConfig.MIN_ATTACK_RANGE_TARGETS,
		TweakifyModConfig.MAX_ATTACK_RANGE_TARGETS).apply(ConfigOptionKeys.GENERAL_KEY);

	// 存档游玩时长：在存档列表右上角显示累计游玩时长，格式由子项切换。
	public static final ConfigBooleanHotkeyed SAVE_PLAY_TIME = new ConfigBooleanHotkeyed(
		"savePlayTime", false, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigOptionList SAVE_PLAY_TIME_FORMAT = new ConfigOptionList(
		"savePlayTimeFormat", SavePlayTimeFormat.HOURS).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigInteger SAVE_PLAY_TIME_DECIMALS = new ConfigInteger(
		"savePlayTimeDecimals", TweakifyModConfig.DEFAULT_SAVE_PLAY_TIME_DECIMALS,
		TweakifyModConfig.MIN_SAVE_PLAY_TIME_DECIMALS,
		TweakifyModConfig.MAX_SAVE_PLAY_TIME_DECIMALS).apply(ConfigOptionKeys.GENERAL_KEY);

	// 自定义方块轮廓：总开关与色调/线宽子项。
	public static final ConfigBooleanHotkeyed CUSTOM_BLOCK_OUTLINE = new ConfigBooleanHotkeyed(
		"customBlockOutline", false, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigDouble BLOCK_OUTLINE_WIDTH = new BlockOutlineWidthConfig(
		TweakifyModConfig.BLOCK_OUTLINE_WIDTH_FIELD).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigInteger BLOCK_OUTLINE_ANIMATION_SPEED = new ConfigInteger(
		"blockOutlineAnimationSpeed", TweakifyModConfig.DEFAULT_BLOCK_OUTLINE_ANIMATION_SPEED,
		TweakifyModConfig.MIN_BLOCK_OUTLINE_ANIMATION_SPEED,
		TweakifyModConfig.MAX_BLOCK_OUTLINE_ANIMATION_SPEED).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigOptionList BLOCK_OUTLINE_PALETTE = new ConfigOptionList(
		"blockOutlinePalette", BlockOutlinePalette.WHITE).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigOptionList BLOCK_OUTLINE_COLOR_MODE = new ConfigOptionList(
		"blockOutlineColorMode", BlockOutlineColorMode.PALETTE).apply(ConfigOptionKeys.GENERAL_KEY);

	// 更好的放置覆盖：可直接替换花、高草等植物。
	public static final ConfigBooleanHotkeyed BETTER_BLOCK_PLACEMENT = new ConfigBooleanHotkeyed(
		"betterBlockPlacement", false, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	// 更好的骨粉催熟：骨粉可催熟甘蔗与一格高的花。
	public static final ConfigBooleanHotkeyed BETTER_BONE_MEAL = new ConfigBooleanHotkeyed(
		"betterBoneMeal", false, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean BONE_MEAL_SUGAR_CANE = new ConfigBoolean(
		"boneMealSugarCane", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean BONE_MEAL_FLOWERS = new ConfigBoolean(
		"boneMealFlowers", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigInteger BONE_MEAL_FLOWER_COUNT = new ConfigInteger(
		"boneMealFlowerCount", TweakifyModConfig.DEFAULT_BONE_MEAL_FLOWER_COUNT,
		TweakifyModConfig.MIN_BONE_MEAL_FLOWER_COUNT,
		TweakifyModConfig.MAX_BONE_MEAL_FLOWER_COUNT).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigInteger BONE_MEAL_FLOWER_RADIUS = new ConfigInteger(
		"boneMealFlowerRadius", TweakifyModConfig.DEFAULT_BONE_MEAL_FLOWER_RADIUS,
		TweakifyModConfig.MIN_BONE_MEAL_FLOWER_RADIUS,
		TweakifyModConfig.MAX_BONE_MEAL_FLOWER_RADIUS).apply(ConfigOptionKeys.GENERAL_KEY);

	// 放置覆盖范围。
	public static final ConfigOptionList BETTER_BLOCK_PLACEMENT_MODE = new ConfigOptionList(
		"betterBlockPlacementMode", BlockPlacementMode.FLOWERS).apply(ConfigOptionKeys.GENERAL_KEY);

	// 潜行时保持原版放置，不覆盖植物。
	public static final ConfigBoolean BETTER_BLOCK_PLACEMENT_SNEAK_BYPASS = new ConfigBoolean(
		"betterBlockPlacementSneakBypass", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// 耐久文字子项。
	public static final ConfigBoolean DURABILITY_OVERLAY_HOTBAR_ONLY = new ConfigBoolean(
		"durabilityOverlayHotbarOnly", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigInteger DURABILITY_OVERLAY_LOW_THRESHOLD = new ConfigInteger(
		"durabilityOverlayLowThreshold", TweakifyModConfig.MIN_DURABILITY_THRESHOLD,
		TweakifyModConfig.MIN_DURABILITY_THRESHOLD, TweakifyModConfig.MAX_DURABILITY_THRESHOLD).apply(ConfigOptionKeys.GENERAL_KEY);

	// 药水效果时间显示：在 HUD 效果图标下方叠加剩余时间。
	public static final ConfigBooleanHotkeyed STATUS_EFFECT_DURATION_DISPLAY = new ConfigBooleanHotkeyed(
		"statusEffectDurationDisplay", false, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	// 时间格式固定为秒（不使用分:秒 / h / d 缩写）。
	public static final ConfigBoolean STATUS_EFFECT_DURATION_ALWAYS_SECONDS = new ConfigBoolean(
		"statusEffectDurationAlwaysSeconds", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 低时长过滤阈值（秒，0 = 不过滤）与文字配色。
	public static final ConfigInteger STATUS_EFFECT_DURATION_LOW_THRESHOLD = new ConfigInteger(
		"statusEffectDurationLowThreshold", TweakifyModConfig.DEFAULT_STATUS_DURATION_THRESHOLD,
		TweakifyModConfig.MIN_STATUS_DURATION_THRESHOLD,
		TweakifyModConfig.MAX_STATUS_DURATION_THRESHOLD).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigOptionList STATUS_EFFECT_DURATION_COLOR_MODE = new ConfigOptionList(
		"statusEffectDurationColorMode", StatusDurationColorMode.AUTO).apply(ConfigOptionKeys.GENERAL_KEY);

	// 右上角药水效果显示的整体缩放百分比（50 ~ 200），使用滑条调节。
	public static final ConfigInteger STATUS_EFFECT_DURATION_SCALE = new ConfigInteger(
		"statusEffectDurationScale", TweakifyModConfig.DEFAULT_STATUS_EFFECT_SCALE,
		TweakifyModConfig.MIN_STATUS_EFFECT_SCALE, TweakifyModConfig.MAX_STATUS_EFFECT_SCALE, true).apply(ConfigOptionKeys.GENERAL_KEY);

	// 复制 ID 子项。
	public static final ConfigOptionList COPY_ITEM_ID_FORMAT = new ConfigOptionList(
		"copyItemIdFormat", CopyIdFormat.PLAIN).apply(ConfigOptionKeys.GENERAL_KEY);

	// 是否保留命名空间前缀：关闭后 minecraft:stone 复制为 stone。
	public static final ConfigBoolean COPY_ITEM_ID_NAMESPACE = new ConfigBoolean(
		"copyItemIdNamespace", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean COPY_ITEM_ID_CLOSE_CONTAINER = new ConfigBoolean(
		"copyItemIdCloseContainer", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean COPY_ITEM_ID_CHAT_MESSAGE = new ConfigBoolean(
		"copyItemIdChatMessage", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// 末地折跃门传送。
	public static final ConfigBoolean DISABLE_END_GATEWAY_TELEPORT = new ConfigBoolean(
		"disableEndGatewayTeleport", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// 铲雪子项。
	public static final ConfigBoolean SHOVEL_SNOW_DROP_SNOWBALL = new ConfigBoolean(
		"shovelSnowDropSnowball", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean SHOVEL_SNOW_CONSUME_DURABILITY = new ConfigBoolean(
		"shovelSnowConsumeDurability", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// 聊天框选子项。
	public static final ConfigOptionList CHAT_SELECTION_COLOR = new ConfigOptionList(
		"chatSelectionColor", ChatSelectionColor.BLUE).apply(ConfigOptionKeys.GENERAL_KEY);

	// 框选背景透明度百分比。
	public static final ConfigInteger CHAT_SELECTION_OPACITY = new ConfigInteger(
		"chatSelectionOpacity", TweakifyModConfig.DEFAULT_CHAT_SELECTION_OPACITY,
		TweakifyModConfig.MIN_CHAT_SELECTION_OPACITY,
		TweakifyModConfig.MAX_CHAT_SELECTION_OPACITY).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean CHAT_SELECTION_AUTO_COPY = new ConfigBoolean(
		"chatSelectionAutoCopy", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 剑穿草打生物：持剑时跳过花草直接选中生物。
	public static final ConfigBooleanHotkeyed SWORD_CUT_THROUGH_PLANTS = new ConfigBooleanHotkeyed(
		"swordCutThroughPlants", false, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigOptionList CUT_THROUGH_ITEMS = new ConfigOptionList(
		"cutThroughItems", CutThroughItemMode.SWORD).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigOptionList CUT_THROUGH_PLANTS = new ConfigOptionList(
		"cutThroughPlants", CutThroughPlantMode.ALL).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean CUT_THROUGH_SNEAK_BYPASS = new ConfigBoolean(
		"cutThroughSneakBypass", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// 中键获得液体桶：准星指向水/岩浆时中键得到对应液体桶（仅创造模式）。
	public static final ConfigBooleanHotkeyed PICK_FLUID_BUCKET = new ConfigBooleanHotkeyed(
		"pickFluidBucket", false, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	// 液体范围：水与岩浆 / 仅水 / 仅岩浆。
	public static final ConfigOptionList PICK_FLUID_BUCKET_FLUIDS = new ConfigOptionList(
		"pickFluidBucketFluids", FluidPickScope.WATER_AND_LAVA).apply(ConfigOptionKeys.GENERAL_KEY);

	// 只取源头液体。
	public static final ConfigBoolean PICK_FLUID_BUCKET_SOURCE_ONLY = new ConfigBoolean(
		"pickFluidBucketSourceOnly", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 生存模式中键取物：背包里有对应物品就切换到主手。
	public static final ConfigBooleanHotkeyed SURVIVAL_PICK_BLOCK = new ConfigBooleanHotkeyed(
		"survivalPickBlock", false, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	// 生存取物是否也处理液体分支（水/岩浆 → 已有的桶）。
	public static final ConfigBoolean SURVIVAL_PICK_BLOCK_INCLUDE_FLUIDS = new ConfigBoolean(
		"survivalPickBlockIncludeFluids", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean SURVIVAL_PICK_BLOCK_HOTBAR_ONLY = new ConfigBoolean(
		"survivalPickBlockHotbarOnly", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 便捷焚烧：右键点击岩浆桶或打火石焚烧光标上的物品。
	public static final ConfigBooleanHotkeyed QUICK_BURN_ITEM = new ConfigBooleanHotkeyed(
		"quickBurnItem", false, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	// 需要潜行才焚烧，避免整理背包时误触。
	public static final ConfigBoolean QUICK_BURN_ITEM_REQUIRE_SNEAK = new ConfigBoolean(
		"quickBurnItemRequireSneak", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 悬停在可焚烧组合上时显示提示。
	public static final ConfigBoolean QUICK_BURN_ITEM_SHOW_HINT = new ConfigBoolean(
		"quickBurnItemShowHint", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// 可用作点燃物的物品 ID 列表。
	public static final ConfigStringList QUICK_BURN_ITEM_IGNITERS = new ItemIdConfigStringList(
		"quickBurnItemIgniters", ImmutableList.copyOf(TweakifyModConfig.DEFAULT_QUICK_BURN_IGNITERS))
		.apply(ConfigOptionKeys.LISTS_KEY);

	// 如履平地：自动跨上 1 格高的方块。
	public static final ConfigBooleanHotkeyed AUTO_STEP_UP = new ConfigBooleanHotkeyed(
		"autoStepUp", false, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	// 跨步高度：1 格 / 1.5 格 / 2 格。
	public static final ConfigOptionList AUTO_STEP_UP_HEIGHT = new ConfigOptionList(
		"autoStepUpHeight", AutoStepUpHeight.ONE_BLOCK).apply(ConfigOptionKeys.GENERAL_KEY);

	// 潜行时保持原版步高。
	public static final ConfigBoolean AUTO_STEP_UP_SNEAK_BYPASS = new ConfigBoolean(
		"autoStepUpSneakBypass", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 更好的攀爬：攀爬方块相关的移动优化。
	public static final ConfigBooleanHotkeyed BETTER_CLIMBING = new ConfigBooleanHotkeyed(
		"betterClimbing", false, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean CLIMBING_FAST_DESCEND = new ConfigBoolean(
		"climbingFastDescend", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean CLIMBING_FASTER_ASCEND = new ConfigBoolean(
		"climbingFasterAscend", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean CLIMBING_JUMP_FROM_GROUND = new ConfigBoolean(
		"climbingJumpFromGround", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean CLIMBING_CANCEL_COLLISION = new ConfigBoolean(
		"climbingCancelCollision", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// 主动爬行：总开关 + 按住键（子项）进入爬行姿态。
	public static final ConfigBooleanHotkeyed FORCE_CRAWL = new ConfigBooleanHotkeyed(
		"forceCrawl", false, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	// allowEmpty = false：未绑定按键时 isKeybindHeld() 不会返回 true，避免默认全员爬行。
	public static final ConfigHotkey FORCE_CRAWL_KEY = new ConfigHotkey(
		"forceCrawlKey", "", KeybindSettings.PRESS_ALLOWEXTRA).apply(ConfigOptionKeys.GENERAL_KEY);

	// 只在地面时强制爬行。
	public static final ConfigBoolean FORCE_CRAWL_ONLY_ON_GROUND = new ConfigBoolean(
		"forceCrawlOnlyOnGround", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 防猪灵装备切换：附近有猪灵时把背包里的金制装备换到身上，猪灵离开后换回。
	public static final ConfigBooleanHotkeyed PIGLIN_GOLD_ARMOR_SWAP = new ConfigBooleanHotkeyed(
		"piglinGoldArmorSwap", false, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	// 替换部位：自动（有哪个金装备就穿一件）或指定单部位。
	public static final ConfigOptionList PIGLIN_GOLD_ARMOR_SWAP_MODE = new ConfigOptionList(
		"piglinGoldArmorSwapMode", PiglinArmorSwapMode.AUTO).apply(ConfigOptionKeys.GENERAL_KEY);

	// 探测猪灵的半径（格）。
	public static final ConfigInteger PIGLIN_GOLD_ARMOR_SWAP_RANGE = new ConfigInteger(
		"piglinGoldArmorSwapRange", TweakifyModConfig.DEFAULT_PIGLIN_SWAP_RANGE,
		TweakifyModConfig.MIN_PIGLIN_SWAP_RANGE,
		TweakifyModConfig.MAX_PIGLIN_SWAP_RANGE).apply(ConfigOptionKeys.GENERAL_KEY);

	// 聊天框内容记忆：关闭聊天框后保留未发送的输入内容。
	public static final ConfigBooleanHotkeyed CHAT_INPUT_MEMORY = new ConfigBooleanHotkeyed(
		"chatInputMemory", false, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	// 以 / 开头的命令草稿不记忆。
	public static final ConfigBoolean CHAT_INPUT_MEMORY_IGNORE_COMMANDS = new ConfigBoolean(
		"chatInputMemoryIgnoreCommands", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 草稿最大保留长度（字符）。
	public static final ConfigInteger CHAT_INPUT_MEMORY_MAX_LENGTH = new ConfigInteger(
		"chatInputMemoryMaxLength", TweakifyModConfig.DEFAULT_CHAT_INPUT_MEMORY_LENGTH,
		TweakifyModConfig.MIN_CHAT_INPUT_MEMORY_LENGTH,
		TweakifyModConfig.MAX_CHAT_INPUT_MEMORY_LENGTH).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBooleanHotkeyed ENTITY_HIGHLIGHT = new ConfigBooleanHotkeyed(
		"entityHighlight", false, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.HIGHLIGHT_KEY);

	// 描边样式（常亮/闪烁）、闪烁周期与描边颜色。
	public static final ConfigOptionList ENTITY_HIGHLIGHT_STYLE = new ConfigOptionList(
		"entityHighlightStyle", EntityHighlightStyle.OUTLINE).apply(ConfigOptionKeys.HIGHLIGHT_KEY);

	public static final ConfigInteger ENTITY_HIGHLIGHT_BLINK_PERIOD = new ConfigInteger(
		"entityHighlightBlinkPeriod", TweakifyModConfig.DEFAULT_ENTITY_HIGHLIGHT_BLINK_PERIOD,
		TweakifyModConfig.MIN_ENTITY_HIGHLIGHT_BLINK_PERIOD,
		TweakifyModConfig.MAX_ENTITY_HIGHLIGHT_BLINK_PERIOD).apply(ConfigOptionKeys.HIGHLIGHT_KEY);

	public static final ConfigOptionList ENTITY_HIGHLIGHT_COLOR = new ConfigOptionList(
		"entityHighlightColor", EntityHighlightColor.VANILLA).apply(ConfigOptionKeys.HIGHLIGHT_KEY);

	public static final ConfigBoolean HIGHLIGHT_PRESET_1 = new ConfigBoolean(
		"highlightPreset1", false).apply(ConfigOptionKeys.HIGHLIGHT_KEY);

	public static final ConfigBoolean HIGHLIGHT_PRESET_2 = new ConfigBoolean(
		"highlightPreset2", false).apply(ConfigOptionKeys.HIGHLIGHT_KEY);

	public static final ConfigBoolean HIGHLIGHT_PRESET_3 = new ConfigBoolean(
		"highlightPreset3", false).apply(ConfigOptionKeys.HIGHLIGHT_KEY);

	public static final ConfigStringList HIGHLIGHT_PRESET_1_ENTITIES = new EntityIdConfigStringList(
		"highlightPreset1Entities", ImmutableList.of()).apply(ConfigOptionKeys.HIGHLIGHT_KEY);

	public static final ConfigStringList HIGHLIGHT_PRESET_2_ENTITIES = new EntityIdConfigStringList(
		"highlightPreset2Entities", ImmutableList.of()).apply(ConfigOptionKeys.HIGHLIGHT_KEY);

	// 爆炸范围与横扫范围：复用攻击范围的显示/渲染模式，颜色固定红色。
	public static final ConfigBooleanHotkeyed EXPLOSION_RANGE_DISPLAY = new ConfigBooleanHotkeyed(
		"explosionRangeDisplay", false, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	// 爆炸范围子开关：分别控制 TNT（含 TNT 方块）、TNT 矿车、苦力怕与末地水晶。
	public static final ConfigBoolean EXPLOSION_RANGE_SHOW_TNT = new ConfigBoolean(
		"explosionRangeShowTnt", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean EXPLOSION_RANGE_SHOW_TNT_MINECARTS = new ConfigBoolean(
		"explosionRangeShowTntMinecarts", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean EXPLOSION_RANGE_SHOW_CREEPERS = new ConfigBoolean(
		"explosionRangeShowCreepers", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean EXPLOSION_RANGE_SHOW_END_CRYSTALS = new ConfigBoolean(
		"explosionRangeShowEndCrystals", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// 更好的 F2 截图：F9 为子项，输出格式覆盖 PNG/JPG/TGA/BMP。
	public static final ConfigBoolean BETTER_SCREENSHOT = new ConfigBoolean(
		"betterScreenshot", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean BETTER_SCREENSHOT_F9 = new ConfigBoolean(
		"betterScreenshotF9", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigOptionList BETTER_SCREENSHOT_FORMAT = new ConfigOptionList(
		"betterScreenshotFormat", ScreenshotFormat.PNG).apply(ConfigOptionKeys.GENERAL_KEY);

	// 截图后是否在聊天栏显示可点击的截图卡片。
	public static final ConfigBoolean BETTER_SCREENSHOT_CHAT_CARD = new ConfigBoolean(
		"betterScreenshotChatCard", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBooleanHotkeyed SWEEPING_VIEW = new ConfigBooleanHotkeyed(
		"sweepingView", false, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	// sweeping-view 子设置：目标框线框 / 面框 / 球体与各自颜色。
	public static final ConfigBoolean SWEEPING_VIEW_WIREFRAME = new ConfigBoolean(
		"sweepingViewWireframe", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigOptionList SWEEPING_VIEW_WIREFRAME_COLOR = new ConfigOptionList(
		"sweepingViewWireframeColor", AttackRangePalette.RED).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean SWEEPING_VIEW_FACE = new ConfigBoolean(
		"sweepingViewFace", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean SWEEPING_VIEW_FACE_SOLID = new ConfigBoolean(
		"sweepingViewFaceSolid", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigOptionList SWEEPING_VIEW_FACE_COLOR = new ConfigOptionList(
		"sweepingViewFaceColor", AttackRangePalette.RED).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean SWEEPING_VIEW_SPHERE = new ConfigBoolean(
		"sweepingViewSphere", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigOptionList SWEEPING_VIEW_SPHERE_COLOR = new ConfigOptionList(
		"sweepingViewSphereColor", AttackRangePalette.RED).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean SWEEPING_VIEW_VEHICLES = new ConfigBoolean(
		"sweepingViewVehicles", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigStringList HIGHLIGHT_PRESET_3_ENTITIES = new EntityIdConfigStringList(
		"highlightPreset3Entities", ImmutableList.of()).apply(ConfigOptionKeys.HIGHLIGHT_KEY);

	// 附魔台附魔信息：悬停附魔选项时显示实际将获得的附魔。
	public static final ConfigBooleanHotkeyed ENCHANTING_TABLE_INFO = new ConfigBooleanHotkeyed(
		"enchantingTableInfo", false, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	// 附魔台信息子项：显示经验消耗、显示最高等级与仅潜行时显示。
	public static final ConfigBoolean ENCHANTING_TABLE_INFO_SHOW_COST = new ConfigBoolean(
		"enchantingTableInfoShowCost", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean ENCHANTING_TABLE_INFO_SHOW_MAX_LEVEL = new ConfigBoolean(
		"enchantingTableInfoShowMaxLevel", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean ENCHANTING_TABLE_INFO_REQUIRE_SNEAK = new ConfigBoolean(
		"enchantingTableInfoRequireSneak", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 手持物品信息（参考 held-item-info）：名称下方显示附魔、容器内容、告示牌等。
	public static final ConfigBooleanHotkeyed HELD_ITEM_INFO = new ConfigBooleanHotkeyed(
		"heldItemInfo", false, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	// 内容开关：与参考实现的选项一一对应。
	public static final ConfigBoolean HELD_ITEM_INFO_SHOW_NAME = new ConfigBoolean(
		"heldItemInfoShowName", true).apply(ConfigOptionKeys.GENERAL_KEY);
	public static final ConfigBoolean HELD_ITEM_INFO_SHOW_ENCHANTMENTS = new ConfigBoolean(
		"heldItemInfoShowEnchantments", true).apply(ConfigOptionKeys.GENERAL_KEY);
	public static final ConfigBoolean HELD_ITEM_INFO_SHOW_POTION_EFFECTS = new ConfigBoolean(
		"heldItemInfoShowPotionEffects", true).apply(ConfigOptionKeys.GENERAL_KEY);
	public static final ConfigBoolean HELD_ITEM_INFO_SHOW_CONTAINER_CONTENT = new ConfigBoolean(
		"heldItemInfoShowContainerContent", true).apply(ConfigOptionKeys.GENERAL_KEY);
	public static final ConfigBoolean HELD_ITEM_INFO_MERGE_SIMILAR_CONTAINER_ITEMS = new ConfigBoolean(
		"heldItemInfoMergeSimilarContainerItems", true).apply(ConfigOptionKeys.GENERAL_KEY);
	public static final ConfigBoolean HELD_ITEM_INFO_SHOW_FIREWORK_ATTRIBUTES = new ConfigBoolean(
		"heldItemInfoShowFireworkAttributes", true).apply(ConfigOptionKeys.GENERAL_KEY);
	public static final ConfigBoolean HELD_ITEM_INFO_SHOW_COMMAND_BLOCK_INFO = new ConfigBoolean(
		"heldItemInfoShowCommandBlockInfo", true).apply(ConfigOptionKeys.GENERAL_KEY);
	public static final ConfigBoolean HELD_ITEM_INFO_SHOW_BEEHIVE_CONTENT = new ConfigBoolean(
		"heldItemInfoShowBeehiveContent", true).apply(ConfigOptionKeys.GENERAL_KEY);
	public static final ConfigBoolean HELD_ITEM_INFO_SHOW_SPAWNER_ENTITY = new ConfigBoolean(
		"heldItemInfoShowSpawnerEntity", true).apply(ConfigOptionKeys.GENERAL_KEY);
	public static final ConfigBoolean HELD_ITEM_INFO_SHOW_CROSSBOW_PROJECTILES = new ConfigBoolean(
		"heldItemInfoShowCrossbowProjectiles", true).apply(ConfigOptionKeys.GENERAL_KEY);
	public static final ConfigBoolean HELD_ITEM_INFO_SHOW_LORE = new ConfigBoolean(
		"heldItemInfoShowLore", true).apply(ConfigOptionKeys.GENERAL_KEY);
	public static final ConfigBoolean HELD_ITEM_INFO_SHOW_UNBREAKABLE = new ConfigBoolean(
		"heldItemInfoShowUnbreakable", true).apply(ConfigOptionKeys.GENERAL_KEY);
	public static final ConfigBoolean HELD_ITEM_INFO_SHOW_SIGN_TEXT = new ConfigBoolean(
		"heldItemInfoShowSignText", true).apply(ConfigOptionKeys.GENERAL_KEY);
	public static final ConfigBoolean HELD_ITEM_INFO_SHOW_MUSIC_DISC_DESCRIPTION = new ConfigBoolean(
		"heldItemInfoShowMusicDiscDescription", true).apply(ConfigOptionKeys.GENERAL_KEY);
	public static final ConfigBoolean HELD_ITEM_INFO_SHOW_PAINTING_DESCRIPTION = new ConfigBoolean(
		"heldItemInfoShowPaintingDescription", true).apply(ConfigOptionKeys.GENERAL_KEY);
	public static final ConfigBoolean HELD_ITEM_INFO_SHOW_GOAT_HORN_INSTRUMENT = new ConfigBoolean(
		"heldItemInfoShowGoatHornInstrument", true).apply(ConfigOptionKeys.GENERAL_KEY);
	public static final ConfigBoolean HELD_ITEM_INFO_SHOW_BOOK_META = new ConfigBoolean(
		"heldItemInfoShowBookMeta", true).apply(ConfigOptionKeys.GENERAL_KEY);
	public static final ConfigBoolean HELD_ITEM_INFO_SHOW_FILLED_MAP_ID = new ConfigBoolean(
		"heldItemInfoShowFilledMapId", false).apply(ConfigOptionKeys.GENERAL_KEY);
	public static final ConfigBoolean HELD_ITEM_INFO_SHOW_ENTITY_BUCKET_CONTENT = new ConfigBoolean(
		"heldItemInfoShowEntityBucketContent", true).apply(ConfigOptionKeys.GENERAL_KEY);
	public static final ConfigBoolean HELD_ITEM_INFO_SHOW_BLOCK_STATE = new ConfigBoolean(
		"heldItemInfoShowBlockState", true).apply(ConfigOptionKeys.GENERAL_KEY);
	public static final ConfigBoolean HELD_ITEM_INFO_SHOW_HIDDEN_LINES_COUNT = new ConfigBoolean(
		"heldItemInfoShowHiddenLinesCount", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// 行数 / 行宽限制。
	public static final ConfigInteger HELD_ITEM_INFO_MAX_LINES = new ConfigInteger(
		"heldItemInfoMaxLines", TweakifyModConfig.DEFAULT_HELD_ITEM_INFO_MAX_LINES,
		TweakifyModConfig.MIN_HELD_ITEM_INFO_LINES,
		TweakifyModConfig.MAX_HELD_ITEM_INFO_LINES).apply(ConfigOptionKeys.GENERAL_KEY);
	public static final ConfigInteger HELD_ITEM_INFO_MAX_LINE_LENGTH = new ConfigInteger(
		"heldItemInfoMaxLineLength", TweakifyModConfig.DEFAULT_HELD_ITEM_INFO_MAX_LINE_LENGTH,
		TweakifyModConfig.MIN_HELD_ITEM_INFO_LINE_LENGTH,
		TweakifyModConfig.MAX_HELD_ITEM_INFO_LINE_LENGTH).apply(ConfigOptionKeys.GENERAL_KEY);
	public static final ConfigInteger HELD_ITEM_INFO_MAX_COMMAND_LINES = new ConfigInteger(
		"heldItemInfoMaxCommandLines", TweakifyModConfig.DEFAULT_HELD_ITEM_INFO_MAX_COMMAND_LINES,
		TweakifyModConfig.MIN_HELD_ITEM_INFO_LINES,
		TweakifyModConfig.MAX_HELD_ITEM_INFO_LINES).apply(ConfigOptionKeys.GENERAL_KEY);
	public static final ConfigInteger HELD_ITEM_INFO_MAX_LORE_LINES = new ConfigInteger(
		"heldItemInfoMaxLoreLines", TweakifyModConfig.DEFAULT_HELD_ITEM_INFO_MAX_LORE_LINES,
		TweakifyModConfig.MIN_HELD_ITEM_INFO_LINES,
		TweakifyModConfig.MAX_HELD_ITEM_INFO_LINES).apply(ConfigOptionKeys.GENERAL_KEY);

	// 是否只显示“在物品栏提示框里可见”的数据组件。
	public static final ConfigBoolean HELD_ITEM_INFO_RESPECT_HIDE_FLAGS = new ConfigBoolean(
		"heldItemInfoRespectHideFlags", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// 排版：行高、额外行偏移、名称间距与整体垂直偏移。
	public static final ConfigInteger HELD_ITEM_INFO_LINE_HEIGHT = new ConfigInteger(
		"heldItemInfoLineHeight", TweakifyModConfig.DEFAULT_HELD_ITEM_INFO_LINE_HEIGHT,
		TweakifyModConfig.MIN_HELD_ITEM_INFO_LINE_HEIGHT,
		TweakifyModConfig.MAX_HELD_ITEM_INFO_LINE_HEIGHT).apply(ConfigOptionKeys.GENERAL_KEY);
	public static final ConfigDouble HELD_ITEM_INFO_OFFSET_PER_EXTRA_LINE = new ConfigDouble(
		"heldItemInfoOffsetPerExtraLine", TweakifyModConfig.DEFAULT_HELD_ITEM_INFO_OFFSET_PER_LINE,
		TweakifyModConfig.MIN_HELD_ITEM_INFO_OFFSET_PER_LINE,
		TweakifyModConfig.MAX_HELD_ITEM_INFO_OFFSET_PER_LINE).apply(ConfigOptionKeys.GENERAL_KEY);
	public static final ConfigInteger HELD_ITEM_INFO_ITEM_NAME_SPACING = new ConfigInteger(
		"heldItemInfoItemNameSpacing", TweakifyModConfig.DEFAULT_HELD_ITEM_INFO_NAME_SPACING,
		TweakifyModConfig.MIN_HELD_ITEM_INFO_NAME_SPACING,
		TweakifyModConfig.MAX_HELD_ITEM_INFO_NAME_SPACING).apply(ConfigOptionKeys.GENERAL_KEY);
	public static final ConfigInteger HELD_ITEM_INFO_VERTICAL_OFFSET = new ConfigInteger(
		"heldItemInfoVerticalOffset", TweakifyModConfig.DEFAULT_HELD_ITEM_INFO_VERTICAL_OFFSET,
		TweakifyModConfig.MIN_HELD_ITEM_INFO_VERTICAL_OFFSET,
		TweakifyModConfig.MAX_HELD_ITEM_INFO_VERTICAL_OFFSET).apply(ConfigOptionKeys.GENERAL_KEY);

	// 淡出时长（秒）：单行基础时长与每多一行的追加时长。
	public static final ConfigDouble HELD_ITEM_INFO_BASE_FADE_DURATION = new ConfigDouble(
		"heldItemInfoBaseFadeDuration", TweakifyModConfig.DEFAULT_HELD_ITEM_INFO_BASE_FADE,
		TweakifyModConfig.MIN_HELD_ITEM_INFO_FADE_DURATION,
		TweakifyModConfig.MAX_HELD_ITEM_INFO_FADE_DURATION).apply(ConfigOptionKeys.GENERAL_KEY);
	public static final ConfigDouble HELD_ITEM_INFO_FADE_DURATION_PER_EXTRA_LINE = new ConfigDouble(
		"heldItemInfoFadeDurationPerExtraLine", TweakifyModConfig.DEFAULT_HELD_ITEM_INFO_FADE_PER_LINE,
		TweakifyModConfig.MIN_HELD_ITEM_INFO_FADE_DURATION,
		TweakifyModConfig.MAX_HELD_ITEM_INFO_FADE_PER_LINE).apply(ConfigOptionKeys.GENERAL_KEY);

	// 与血量条/动作栏消息错开。
	public static final ConfigBoolean HELD_ITEM_INFO_PREVENT_OVERLAP = new ConfigBoolean(
		"heldItemInfoPreventOverlap", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// 背景可见性。
	public static final ConfigOptionList HELD_ITEM_INFO_BACKGROUND = new ConfigOptionList(
		"heldItemInfoBackground", HeldItemInfoBackground.VANILLA).apply(ConfigOptionKeys.GENERAL_KEY);

	// 附魔过滤：只保留（或只排除）名单内的附魔。
	public static final ConfigBoolean HELD_ITEM_INFO_SHOW_ONLY_FILTERED_ENCHANTMENTS = new ConfigBoolean(
		"heldItemInfoShowOnlyFilteredEnchantments", false).apply(ConfigOptionKeys.GENERAL_KEY);
	public static final ConfigStringList HELD_ITEM_INFO_FILTERED_ENCHANTMENTS = new ConfigStringList(
		"heldItemInfoFilteredEnchantments", ImmutableList.of()).apply(ConfigOptionKeys.LISTS_KEY);

	// 背包快捷右键：右键点击背包里的方块物品直接打开对应界面。
	public static final ConfigBooleanHotkeyed INVENTORY_QUICK_RIGHT_CLICK = new ConfigBooleanHotkeyed(
		"inventoryQuickRightClick", false, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean QUICK_RIGHT_CLICK_FUNCTIONAL_BLOCKS = new ConfigBoolean(
		"quickRightClickFunctionalBlocks", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean QUICK_RIGHT_CLICK_SHULKER_BOXES = new ConfigBoolean(
		"quickRightClickShulkerBoxes", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean QUICK_RIGHT_CLICK_ENDER_CHEST = new ConfigBoolean(
		"quickRightClickEnderChest", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// 需要潜行才打开，避免整理背包时误触。
	public static final ConfigBoolean QUICK_RIGHT_CLICK_REQUIRE_SNEAK = new ConfigBoolean(
		"quickRightClickRequireSneak", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigStringList NETHER_PORTAL_TELEPORT_BLACKLIST = new EntityIdConfigStringList(
		"netherPortalTeleportBlacklist", ImmutableList.of()).apply(ConfigOptionKeys.LISTS_KEY);

	public static final ConfigStringList END_PORTAL_TELEPORT_BLACKLIST = new EntityIdConfigStringList(
		"endPortalTeleportBlacklist", ImmutableList.of()).apply(ConfigOptionKeys.LISTS_KEY);

	public static final ConfigStringList END_GATEWAY_TELEPORT_BLACKLIST = new EntityIdConfigStringList(
		"endGatewayTeleportBlacklist", ImmutableList.of()).apply(ConfigOptionKeys.LISTS_KEY);

	public static final ConfigStringList ATTACK_RANGE_ENTITIES = new EntityIdConfigStringList(
		"attackRangeEntities", ImmutableList.of()).apply(ConfigOptionKeys.LISTS_KEY);

	public static final ConfigStringList DISABLED_SPAWN_ENTITIES = new EntityIdConfigStringList(
		"disabledSpawnEntities", ImmutableList.of()).apply(ConfigOptionKeys.LISTS_KEY);

	public static final ConfigStringList PEACEFUL_MODE_MONSTER_ENTITIES = new EntityIdConfigStringList(
		"peacefulModeMonsterEntities", ImmutableList.of()).apply(ConfigOptionKeys.LISTS_KEY);

	public static final ConfigStringList RESTRICTED_OFFHAND_ITEMS = new ItemIdConfigStringList(
		"restrictedOffhandItems",
		ImmutableList.copyOf(TweakifyModConfig.defaultLightSourceItemIds())).apply(ConfigOptionKeys.LISTS_KEY);

	public static final ConfigStringList MAIN_HAND_USE_COOLDOWN_ITEMS = new ItemIdConfigStringList(
		"mainHandUseCooldownItems",
		ImmutableList.copyOf(TweakifyModConfig.defaultInteractableItemIds())).apply(ConfigOptionKeys.LISTS_KEY);

	public static final ConfigStringList OFFHAND_USE_COOLDOWN_ITEMS = new ItemIdConfigStringList(
		"offhandUseCooldownItems",
		ImmutableList.copyOf(TweakifyModConfig.defaultInteractableItemIds())).apply(ConfigOptionKeys.LISTS_KEY);
}
