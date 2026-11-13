package com.gtzc.tweakify.client;

import com.google.common.collect.ImmutableList;
import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.ConfigCatalog;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.AsyncFileWriter;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.config.IConfigDouble;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.IConfigInteger;
import fi.dy.masa.malilib.config.IConfigOptionList;
import fi.dy.masa.malilib.config.IConfigStringList;
import fi.dy.masa.malilib.config.options.ConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import fi.dy.masa.malilib.config.options.ConfigDouble;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import fi.dy.masa.malilib.config.options.ConfigStringList;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
import fi.dy.masa.malilib.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

// 配置项字段通过 OPTIONS_BY_NAME 反射按名字索引，配置界面、快捷键与命令表都按名字派生；
// IDE 无法追踪这种“按字符串使用”，会把大量公开配置声明误报为未使用。
// 这些字段不能删除（漏一项会让启动期双向覆盖校验直接失败）。
// 注意：类级注解也会抑制方法体内的未使用提示；当前私有成员与局部变量均已核对有调用，
// 后续新增逻辑请自行确认没有引入真正的死代码。
@SuppressWarnings("unused")
public final class TweakifyModConfigOptions implements IConfigHandler {
	private static final String GENERAL_KEY = TweakifyMod.MOD_ID + ".config.general";
	private static final String LISTS_KEY = TweakifyMod.MOD_ID + ".config.lists";
	private static final String HIGHLIGHT_KEY = TweakifyMod.MOD_ID + ".config.highlight";

	// 仅由“测试”标签追加显示的界面操作项：打开配置档案编辑器。
	// 使用 ConfigBoolean 以复用 MaLiLib 的配置行布局；条目控件会移除其原生开关/重置按钮，只保留“打开”。
	public static final ConfigBoolean PROFILE_EDITOR = new ConfigBoolean("profileEditor", false).apply(GENERAL_KEY);

	// 开关项的切换快捷键：默认无快捷键（允许为空），允许多余按键便于边走边按。
	private static final KeybindSettings TOGGLE_HOTKEY_SETTINGS = KeybindSettings.PRESS_ALLOWEXTRA_EMPTY;

	// 打开配置界面的快捷键：任意界面可用，允许清空。
	private static final KeybindSettings OPEN_CONFIG_GUI_SETTINGS =
		KeybindSettings.create(KeybindSettings.Context.ANY, KeyAction.PRESS, true, true, false, true, true);

	// 复制物体 ID 的快捷键：需要在容器界面（背包/箱子）里也能触发，因此同样使用 ANY 上下文。
	private static final KeybindSettings COPY_ID_HOTKEY_SETTINGS =
		KeybindSettings.create(KeybindSettings.Context.ANY, KeyAction.PRESS, true, true, false, true, true);

	// 打开配置界面的快捷键，默认 G + C，置顶显示。
	public static final ConfigOptionList LANGUAGE = new ConfigOptionList(
		"language", ModLanguage.ZH_CN).apply(GENERAL_KEY);
	public static final ConfigBoolean FOLLOW_SYSTEM_LANGUAGE = new ConfigBoolean(
		"followSystemLanguage", true).apply(GENERAL_KEY);
	// 标签记忆：打开配置界面时停在上次选择的分类标签。
	public static final ConfigBoolean REMEMBER_SELECTED_TAB = new ConfigBoolean(
		"rememberSelectedTab", false).apply(GENERAL_KEY);
	public static final ConfigHotkey OPEN_CONFIG_GUI = new ConfigHotkey(
		"openConfigGui", "G,C", OPEN_CONFIG_GUI_SETTINGS).apply(GENERAL_KEY);
	// 掉落物消失时间（tick）：默认 6000 = 原版 5 分钟，滑块范围 3 秒 ~ 30 分钟。
	public static final ConfigInteger ITEM_DESPAWN_TIME = new ConfigInteger(
		"itemDespawnTime", TweakifyModConfig.DEFAULT_ITEM_DESPAWN_TICKS,
		TweakifyModConfig.MIN_ITEM_DESPAWN_TICKS,
		TweakifyModConfig.MAX_ITEM_DESPAWN_TICKS).apply(GENERAL_KEY);
	// 复制物体 ID：开关默认关闭，条目自带的快捷键用于切换开关；
	// 真正执行复制的快捷键是它下面的子项 copyTargetId。
	public static final ConfigBooleanHotkeyed COPY_ITEM_ID = new ConfigBooleanHotkeyed(
		"copyItemId", false, "", TOGGLE_HOTKEY_SETTINGS).apply(GENERAL_KEY);
	public static final ConfigHotkey COPY_TARGET_ID_HOTKEY = new ConfigHotkey(
		"copyTargetId", "", COPY_ID_HOTKEY_SETTINGS).apply(GENERAL_KEY);
	public static final ConfigBoolean BLOCK_OFFHAND_FALLBACK = new ConfigBoolean(
		"blockOffhandFallback", true).apply(GENERAL_KEY);
	public static final ConfigBoolean INTERCEPT_BLOCK_INTERACTION = new ConfigBoolean(
		"interceptBlockInteraction", false).apply(GENERAL_KEY);
	public static final ConfigBoolean INTERCEPT_ENTITY_INTERACTION = new ConfigBoolean(
		"interceptEntityInteraction", false).apply(GENERAL_KEY);
	public static final ConfigBooleanHotkeyed MINING_COOLDOWN = new ConfigBooleanHotkeyed(
		"miningCooldown", false, "", TOGGLE_HOTKEY_SETTINGS).apply(GENERAL_KEY);
	public static final ConfigBoolean MINING_COOLDOWN_ATTACKS = new ConfigBoolean(
		"miningCooldownAttacks", false).apply(GENERAL_KEY);
	public static final ConfigBooleanHotkeyed USE_COOLDOWN = new ConfigBooleanHotkeyed(
		"useCooldown", true, "", TOGGLE_HOTKEY_SETTINGS).apply(GENERAL_KEY);
	public static final ConfigBoolean MAIN_HAND_USE_COOLDOWN = new ConfigBoolean(
		"mainHandUseCooldown", false).apply(GENERAL_KEY);
	public static final ConfigBoolean OFFHAND_USE_COOLDOWN = new ConfigBoolean(
		"offhandUseCooldown", false).apply(GENERAL_KEY);
	public static final ConfigBoolean DISABLE_USE_ITEM_SLOWDOWN = new ConfigBoolean(
		"disableUseItemSlowdown", false).apply(GENERAL_KEY);
	public static final ConfigStringList USE_ITEM_SLOWDOWN_EXCEPTIONS = new ItemIdConfigStringList(
		"useItemSlowdownExceptions", ImmutableList.of()).apply(LISTS_KEY);
	public static final ConfigBoolean DISABLE_HURT_SHAKE = new ConfigBoolean(
		"disableHurtShake", false).apply(GENERAL_KEY);
	public static final ConfigBooleanHotkeyed DISABLE_FLIGHT_DRIFT = new ConfigBooleanHotkeyed(
		"disableFlightDrift", false, "", TOGGLE_HOTKEY_SETTINGS).apply(GENERAL_KEY);
	public static final ConfigBoolean FLIGHT_DRIFT_VERTICAL = new ConfigBoolean(
		"flightDriftVertical", false).apply(GENERAL_KEY);
	public static final ConfigBoolean DISABLE_HURT_TINT = new ConfigBoolean(
		"disableHurtTint", false).apply(GENERAL_KEY);
	public static final ConfigBooleanHotkeyed DISABLE_DIMENSION_TELEPORT = new ConfigBooleanHotkeyed(
		"disableDimensionTeleport", false, "", TOGGLE_HOTKEY_SETTINGS).apply(GENERAL_KEY);
	public static final ConfigBoolean DIMENSION_TELEPORT_PLAYERS_ONLY = new ConfigBoolean(
		"dimensionTeleportPlayersOnly", false).apply(GENERAL_KEY);
	// 地狱门传送延迟：生存模式可秒传，创造模式可改为等待生存延迟。
	public static final ConfigBooleanHotkeyed NETHER_PORTAL_TELEPORT_DELAY = new ConfigBooleanHotkeyed(
		"netherPortalTeleportDelay", false, "", TOGGLE_HOTKEY_SETTINGS).apply(GENERAL_KEY);
	public static final ConfigBoolean NETHER_PORTAL_SURVIVAL_INSTANT_TELEPORT = new ConfigBoolean(
		"netherPortalSurvivalInstantTeleport", false).apply(GENERAL_KEY);
	public static final ConfigBoolean NETHER_PORTAL_CREATIVE_DELAYED_TELEPORT = new ConfigBoolean(
		"netherPortalCreativeDelayedTeleport", false).apply(GENERAL_KEY);
	public static final ConfigBoolean DISABLE_NETHER_PORTAL_TELEPORT = new ConfigBoolean(
		"disableNetherPortalTeleport", true).apply(GENERAL_KEY);
	public static final ConfigBoolean DISABLE_END_PORTAL_TELEPORT = new ConfigBoolean(
		"disableEndPortalTeleport", true).apply(GENERAL_KEY);
	public static final ConfigBoolean DISABLE_UNDERWATER_MINING_PENALTY = new ConfigBoolean(
		"disableUnderwaterMiningPenalty", false).apply(GENERAL_KEY);
	public static final ConfigBoolean DISABLE_AIR_MINING_PENALTY = new ConfigBoolean(
		"disableAirMiningPenalty", false).apply(GENERAL_KEY);
	public static final ConfigBooleanHotkeyed HIGHLIGHT_SINGLE_LAYER_BEDROCK = new ConfigBooleanHotkeyed(
		"highlightSingleLayerBedrock", false, "", TOGGLE_HOTKEY_SETTINGS).apply(GENERAL_KEY);
	public static final ConfigInteger HIGHLIGHT_SINGLE_LAYER_BEDROCK_MAX_MARKERS = new ConfigInteger(
		"highlightSingleLayerBedrockMaxMarkers", TweakifyModConfig.DEFAULT_BEDROCK_MARKERS,
		TweakifyModConfig.MIN_BEDROCK_MARKERS,
		TweakifyModConfig.MAX_BEDROCK_MARKERS).apply(GENERAL_KEY);
	// 屏障可见：不手持屏障物品也能看到周围屏障，条目自带快捷键用于切换开关。
	public static final ConfigBooleanHotkeyed BARRIER_VISIBILITY = new ConfigBooleanHotkeyed(
		"barrierVisibility", false, "", TOGGLE_HOTKEY_SETTINGS).apply(GENERAL_KEY);
	// 装备耐久显示：耐久条上叠加耐久百分比 / 剩余耐久数值，显示内容由子项切换。
	public static final ConfigBooleanHotkeyed DURABILITY_OVERLAY = new ConfigBooleanHotkeyed(
		"durabilityOverlay", false, "", TOGGLE_HOTKEY_SETTINGS).apply(GENERAL_KEY);
	public static final ConfigOptionList DURABILITY_DISPLAY_MODE = new ConfigOptionList(
		"durabilityDisplayMode", DurabilityDisplayMode.PERCENT).apply(GENERAL_KEY);
	public static final ConfigBoolean DISABLE_CURSE_ENCHANTMENT_RESTRICTION = new ConfigBoolean(
		"disableCurseEnchantmentRestriction", false).apply(GENERAL_KEY);
	// 禁用附魔过于昂贵：铁砧花费达到 40 级时仍可取出结果。
	public static final ConfigBoolean DISABLE_TOO_EXPENSIVE = new ConfigBoolean(
		"disableTooExpensive", false).apply(GENERAL_KEY);
	// 移除附魔冲突：铁砧合并时忽略附魔互斥限制。
	public static final ConfigBoolean REMOVE_ENCHANTMENT_CONFLICTS = new ConfigBoolean(
		"removeEnchantmentConflicts", false).apply(GENERAL_KEY);
	// 附魔描述：在物品提示中为每个附魔追加一行描述。
	public static final ConfigBoolean ENCHANTMENT_DESCRIPTIONS = new ConfigBoolean(
		"enchantmentDescriptions", false).apply(GENERAL_KEY);
	public static final ConfigBooleanHotkeyed DISABLE_SHOVEL_SNOW_FULL_BREAK = new ConfigBooleanHotkeyed(
		"disableShovelSnowFullBreak", false, "", TOGGLE_HOTKEY_SETTINGS).apply(GENERAL_KEY);
	// 草径卡船修复：土径对船使用满方块碰撞。
	public static final ConfigBoolean FIX_BOAT_STUCK_ON_DIRT_PATH = new ConfigBoolean(
		"fixBoatStuckOnDirtPath", false).apply(GENERAL_KEY);
	// 鹦鹉肩膀跳舞：唱片机播放音乐时，肩膀上的鹦鹉也跳舞。
	public static final ConfigBoolean PARROT_DANCE_ON_SHOULDER = new ConfigBoolean(
		"parrotDanceOnShoulder", false).apply(GENERAL_KEY);
	// 更好的粗体：粗体偏移从 1.0 改为 0.5。
	public static final ConfigBoolean BETTER_BOLD = new ConfigBoolean(
		"betterBold", false).apply(GENERAL_KEY);
	// MiniHUD 掉帧优化：放宽 HUD 行刷新间隔，减少重复重算。
	public static final ConfigBoolean MINI_HUD_FPS_OPTIMIZATION = new ConfigBoolean(
		"miniHudFpsOptimization", false).apply(GENERAL_KEY);
	// 第三人称自身名字：第三/第二人称显示玩家自己的名字。
	public static final ConfigBoolean THIRD_PERSON_PLAYER_NAME = new ConfigBoolean(
		"thirdPersonPlayerName", false).apply(GENERAL_KEY);
	// 雪傀儡不受群系影响：温暖/干燥群系不再因群系属性融化；雨/水仍会伤害，仅单人/局域网生效。
	public static final ConfigBoolean SNOW_GOLEM_BIOME_IMMUNITY = new ConfigBoolean(
		"snowGolemBiomeImmunity", false).apply(GENERAL_KEY);
	// 相机运动：参考 CameraOverhaul 的转向/横移/俯仰/静止晃动与屏幕震动。
	public static final ConfigBooleanHotkeyed CAMERA_MOTION = new ConfigBooleanHotkeyed(
		"cameraMotion", false, "", TOGGLE_HOTKEY_SETTINGS).apply(GENERAL_KEY);
	public static final ConfigInteger CAMERA_MOTION_INTENSITY = new ConfigInteger(
		"cameraMotionIntensity", TweakifyModConfig.DEFAULT_CAMERA_MOTION_INTENSITY,
		TweakifyModConfig.MIN_CAMERA_MOTION_INTENSITY,
		TweakifyModConfig.MAX_CAMERA_MOTION_INTENSITY).apply(GENERAL_KEY);
	public static final ConfigBoolean CAMERA_MOTION_TURNING_ROLL = new ConfigBoolean(
		"cameraMotionTurningRoll", true).apply(GENERAL_KEY);
	public static final ConfigBoolean CAMERA_MOTION_STRAFING_ROLL = new ConfigBoolean(
		"cameraMotionStrafingRoll", true).apply(GENERAL_KEY);
	public static final ConfigBoolean CAMERA_MOTION_FORWARD_PITCH = new ConfigBoolean(
		"cameraMotionForwardPitch", true).apply(GENERAL_KEY);
	public static final ConfigBoolean CAMERA_MOTION_VERTICAL_PITCH = new ConfigBoolean(
		"cameraMotionVerticalPitch", true).apply(GENERAL_KEY);
	public static final ConfigBoolean CAMERA_MOTION_IDLE_SWAY = new ConfigBoolean(
		"cameraMotionIdleSway", true).apply(GENERAL_KEY);
	public static final ConfigBoolean CAMERA_MOTION_SCREEN_SHAKE = new ConfigBoolean(
		"cameraMotionScreenShake", true).apply(GENERAL_KEY);
	public static final ConfigBoolean CAMERA_MOTION_MOUSE_SMOOTHING = new ConfigBoolean(
		"cameraMotionMouseSmoothing", false).apply(GENERAL_KEY);
	public static final ConfigBoolean CAMERA_MOTION_FIRST_PERSON = new ConfigBoolean(
		"cameraMotionFirstPerson", false).apply(GENERAL_KEY);
	public static final ConfigBoolean CAMERA_MOTION_THIRD_PERSON = new ConfigBoolean(
		"cameraMotionThirdPerson", true).apply(GENERAL_KEY);
	// 物品分组：同类物品折叠成组图标，点击展开/收起。
	public static final ConfigBooleanHotkeyed ITEM_GROUPS = new ConfigBooleanHotkeyed(
		"itemGroups", false, "", TOGGLE_HOTKEY_SETTINGS).apply(GENERAL_KEY);
	public static final ConfigOptionList ITEM_GROUPS_SORT = new ConfigOptionList(
		"itemGroupsSort", ItemGroupSort.DEFAULT).apply(GENERAL_KEY);
	// 鞘翅烟花限制：滑翔中使用烟花后，等加速结束才能再次右键使用。
	public static final ConfigBooleanHotkeyed ELYTRA_FIREWORK_COOLDOWN = new ConfigBooleanHotkeyed(
		"elytraFireworkCooldown", false, "", TOGGLE_HOTKEY_SETTINGS).apply(GENERAL_KEY);
	public static final ConfigInteger ELYTRA_FIREWORK_COOLDOWN_TICKS = new ConfigInteger(
		"elytraFireworkCooldownTicks", TweakifyModConfig.MIN_ELYTRA_FIREWORK_TICKS,
		TweakifyModConfig.MIN_ELYTRA_FIREWORK_TICKS, TweakifyModConfig.MAX_ELYTRA_FIREWORK_TICKS).apply(GENERAL_KEY);
	// 超平坦无史莱姆：超平坦世界中不再生成史莱姆。
	public static final ConfigBoolean DISABLE_FLAT_WORLD_SLIME_SPAWN = new ConfigBoolean(
		"disableFlatWorldSlimeSpawn", false).apply(GENERAL_KEY);
	public static final ConfigBoolean FLAT_WORLD_SLIME_SPAWN_ALL_WORLDS = new ConfigBoolean(
		"flatWorldSlimeSpawnAllWorlds", false).apply(GENERAL_KEY);
	// 关闭指定生物生成：列表为空时不生效。
	public static final ConfigBooleanHotkeyed DISABLE_SPECIFIED_MOB_SPAWN = new ConfigBooleanHotkeyed(
		"disableSpecifiedMobSpawn", false, "", TOGGLE_HOTKEY_SETTINGS).apply(GENERAL_KEY);
	public static final ConfigBoolean DISABLED_SPAWN_ALLOW_PLAYER_SPAWN = new ConfigBoolean(
		"disabledSpawnAllowPlayerSpawn", true).apply(GENERAL_KEY);
	// 和平模式怪物：列表为空时不生效。
	public static final ConfigBooleanHotkeyed PEACEFUL_MODE_MONSTERS = new ConfigBooleanHotkeyed(
		"peacefulModeMonsters", false, "", TOGGLE_HOTKEY_SETTINGS).apply(GENERAL_KEY);
	// 刷怪蛋投掷：像鸡蛋/雪球一样投掷刷怪蛋，命中后生成生物。
	public static final ConfigBooleanHotkeyed THROW_SPAWN_EGG = new ConfigBooleanHotkeyed(
		"throwSpawnEgg", false, "", TOGGLE_HOTKEY_SETTINGS).apply(GENERAL_KEY);
	public static final ConfigBoolean PEACEFUL_MODE_MONSTER_SPAWN = new ConfigBoolean(
		"peacefulModeMonsterSpawn", true).apply(GENERAL_KEY);
	public static final ConfigBoolean PEACEFUL_MODE_MONSTER_NO_ATTACK = new ConfigBoolean(
		"peacefulModeMonsterNoAttack", true).apply(GENERAL_KEY);
	// 更好的隐身：隐身后去掉药水粒子，装备与第三人称手持物品也隐藏。
	public static final ConfigBoolean BETTER_INVISIBILITY = new ConfigBoolean(
		"betterInvisibility", false).apply(GENERAL_KEY);
	// 锻造模板不消耗：锻造台取出成品时保留模板。
	public static final ConfigBoolean DISABLE_SMITHING_TEMPLATE_CONSUMPTION = new ConfigBoolean(
		"disableSmithingTemplateConsumption", false).apply(GENERAL_KEY);
	// 聊天框文字框选：聊天界面拖动选择文字并复制。
	public static final ConfigBooleanHotkeyed CHAT_TEXT_SELECTION = new ConfigBooleanHotkeyed(
		"chatTextSelection", false, "", TOGGLE_HOTKEY_SETTINGS).apply(GENERAL_KEY);
	// 生物攻击范围显示：以生物为中心画半透明蓝色圆圈。
	public static final ConfigBooleanHotkeyed ATTACK_RANGE_DISPLAY = new ConfigBooleanHotkeyed(
		"attackRangeDisplay", false, "", TOGGLE_HOTKEY_SETTINGS).apply(GENERAL_KEY);
	// 攻击范围显示方式：平面圆环 / 半透明球体。
	public static final ConfigOptionList ATTACK_RANGE_DISPLAY_MODE = new ConfigOptionList(
		"attackRangeDisplayMode", AttackRangeDisplayMode.FLAT).apply(GENERAL_KEY);
	// 攻击范围渲染方式：蓝色线框 / 半透明实心。
	public static final ConfigOptionList ATTACK_RANGE_RENDER_MODE = new ConfigOptionList(
		"attackRangeRenderMode", AttackRangeRenderMode.WIREFRAME).apply(GENERAL_KEY);
	// 是否显示本地玩家自己的攻击范围。
	public static final ConfigBoolean ATTACK_RANGE_INCLUDE_SELF = new ConfigBoolean(
		"attackRangeIncludeSelf", false).apply(GENERAL_KEY);
	// 攻击范围的收集距离与单次最多目标数。
	public static final ConfigInteger ATTACK_RANGE_MAX_DISTANCE = new ConfigInteger(
		"attackRangeMaxDistance", TweakifyModConfig.DEFAULT_ATTACK_RANGE_DISTANCE,
		TweakifyModConfig.MIN_ATTACK_RANGE_DISTANCE,
		TweakifyModConfig.MAX_ATTACK_RANGE_DISTANCE).apply(GENERAL_KEY);
	public static final ConfigInteger ATTACK_RANGE_MAX_TARGETS = new ConfigInteger(
		"attackRangeMaxTargets", TweakifyModConfig.DEFAULT_ATTACK_RANGE_TARGETS,
		TweakifyModConfig.MIN_ATTACK_RANGE_TARGETS,
		TweakifyModConfig.MAX_ATTACK_RANGE_TARGETS).apply(GENERAL_KEY);
	// 夜视淡出：夜视结束前平滑淡出。
	public static final ConfigBoolean NIGHT_VISION_FADE_OUT = new ConfigBoolean(
		"nightVisionFadeOut", false).apply(GENERAL_KEY);
	public static final ConfigOptionList NIGHT_VISION_FADE_OUT_CURVE = new ConfigOptionList(
		"nightVisionFadeOutCurve", NightVisionCurve.SMOOTH).apply(GENERAL_KEY);
	// 附魔等级使用阿拉伯数字。
	public static final ConfigBoolean ENCHANTMENT_LEVEL_ARABIC = new ConfigBoolean(
		"enchantmentLevelArabic", false).apply(GENERAL_KEY);
	// 疣块分解：合成格里单独放下界疣块时产出 9 个下界疣。
	public static final ConfigBoolean WART_BLOCK_DECOMPOSITION = new ConfigBoolean(
		"wartBlockDecomposition", false).apply(GENERAL_KEY);
	// 鞘翅水中飞行：滑翔时不再被流体移动逻辑接管。
	public static final ConfigBoolean ELYTRA_WATER_FLIGHT = new ConfigBoolean(
		"elytraWaterFlight", false).apply(GENERAL_KEY);
	public static final ConfigBoolean ELYTRA_WATER_FLIGHT_ONLY_WATER = new ConfigBoolean(
		"elytraWaterFlightOnlyWater", false).apply(GENERAL_KEY);
	// 禁用地狱门特效：不再显示紫色覆盖层、镜头旋转与传送门音效。
	public static final ConfigBoolean DISABLE_NETHER_PORTAL_EFFECT = new ConfigBoolean(
		"disableNetherPortalEffect", false).apply(GENERAL_KEY);
	// 禁用床太远：离床太远时也能直接入睡。
	public static final ConfigBoolean DISABLE_BED_TOO_FAR = new ConfigBoolean(
		"disableBedTooFar", false).apply(GENERAL_KEY);
	// 铁傀儡进矿车：空矿车碰到铁傀儡时让铁傀儡坐进矿车。
	public static final ConfigBoolean IRON_GOLEM_RIDE_MINECART = new ConfigBoolean(
		"ironGolemRideMinecart", false).apply(GENERAL_KEY);
	// 连接方块轮廓：准星对准门/床/双箱等连接方块时显示整体轮廓。
	public static final ConfigBoolean CONNECTED_BLOCK_OUTLINE = new ConfigBoolean(
		"connectedBlockOutline", false).apply(GENERAL_KEY);
	// 自定义方块轮廓：总开关与色调/线宽子项。
	public static final ConfigBooleanHotkeyed CUSTOM_BLOCK_OUTLINE = new ConfigBooleanHotkeyed(
		"customBlockOutline", false, "", TOGGLE_HOTKEY_SETTINGS).apply(GENERAL_KEY);
	public static final ConfigDouble BLOCK_OUTLINE_WIDTH = new BlockOutlineWidthConfig(
		TweakifyModConfig.BLOCK_OUTLINE_WIDTH_FIELD).apply(GENERAL_KEY);
	public static final ConfigInteger BLOCK_OUTLINE_ANIMATION_SPEED = new ConfigInteger(
		"blockOutlineAnimationSpeed", TweakifyModConfig.DEFAULT_BLOCK_OUTLINE_ANIMATION_SPEED,
		TweakifyModConfig.MIN_BLOCK_OUTLINE_ANIMATION_SPEED,
		TweakifyModConfig.MAX_BLOCK_OUTLINE_ANIMATION_SPEED).apply(GENERAL_KEY);
	public static final ConfigOptionList BLOCK_OUTLINE_PALETTE = new ConfigOptionList(
		"blockOutlinePalette", BlockOutlinePalette.WHITE).apply(GENERAL_KEY);
	public static final ConfigOptionList BLOCK_OUTLINE_COLOR_MODE = new ConfigOptionList(
		"blockOutlineColorMode", BlockOutlineColorMode.PALETTE).apply(GENERAL_KEY);
	// 更好的放置覆盖：可直接替换花、高草等植物。
	public static final ConfigBooleanHotkeyed BETTER_BLOCK_PLACEMENT = new ConfigBooleanHotkeyed(
		"betterBlockPlacement", false, "", TOGGLE_HOTKEY_SETTINGS).apply(GENERAL_KEY);
	// 更好的骨粉催熟：骨粉可催熟甘蔗与一格高的花。
	public static final ConfigBooleanHotkeyed BETTER_BONE_MEAL = new ConfigBooleanHotkeyed(
		"betterBoneMeal", false, "", TOGGLE_HOTKEY_SETTINGS).apply(GENERAL_KEY);
	public static final ConfigBoolean BONE_MEAL_SUGAR_CANE = new ConfigBoolean(
		"boneMealSugarCane", true).apply(GENERAL_KEY);
	public static final ConfigBoolean BONE_MEAL_FLOWERS = new ConfigBoolean(
		"boneMealFlowers", true).apply(GENERAL_KEY);
	// 更好的隐身子项。
	public static final ConfigBoolean BETTER_INVISIBILITY_EFFECT_PARTICLES = new ConfigBoolean(
		"betterInvisibilityEffectParticles", true).apply(GENERAL_KEY);
	public static final ConfigBoolean BETTER_INVISIBILITY_LAYERS = new ConfigBoolean(
		"betterInvisibilityLayers", true).apply(GENERAL_KEY);
	public static final ConfigBoolean BETTER_INVISIBILITY_LLAMA_CARPET = new ConfigBoolean(
		"betterInvisibilityLlamaCarpet", true).apply(GENERAL_KEY);
	// 地狱门特效子项。
	public static final ConfigBoolean DISABLE_NETHER_PORTAL_OVERLAY = new ConfigBoolean(
		"disableNetherPortalOverlay", true).apply(GENERAL_KEY);
	public static final ConfigBoolean DISABLE_NETHER_PORTAL_CAMERA = new ConfigBoolean(
		"disableNetherPortalCamera", true).apply(GENERAL_KEY);
	public static final ConfigBoolean DISABLE_NETHER_PORTAL_SOUND = new ConfigBoolean(
		"disableNetherPortalSound", true).apply(GENERAL_KEY);
	// 放置覆盖范围。
	public static final ConfigOptionList BETTER_BLOCK_PLACEMENT_MODE = new ConfigOptionList(
		"betterBlockPlacementMode", BlockPlacementMode.FLOWERS).apply(GENERAL_KEY);
	// 耐久文字子项。
	public static final ConfigBoolean DURABILITY_OVERLAY_HOTBAR_ONLY = new ConfigBoolean(
		"durabilityOverlayHotbarOnly", false).apply(GENERAL_KEY);
	public static final ConfigInteger DURABILITY_OVERLAY_LOW_THRESHOLD = new ConfigInteger(
		"durabilityOverlayLowThreshold", TweakifyModConfig.MIN_DURABILITY_THRESHOLD,
		TweakifyModConfig.MIN_DURABILITY_THRESHOLD, TweakifyModConfig.MAX_DURABILITY_THRESHOLD).apply(GENERAL_KEY);
	// 药水效果时间显示：在 HUD 效果图标下方叠加剩余时间。
	public static final ConfigBooleanHotkeyed STATUS_EFFECT_DURATION_DISPLAY = new ConfigBooleanHotkeyed(
		"statusEffectDurationDisplay", false, "", TOGGLE_HOTKEY_SETTINGS).apply(GENERAL_KEY);
	// 复制 ID 子项。
	public static final ConfigOptionList COPY_ITEM_ID_FORMAT = new ConfigOptionList(
		"copyItemIdFormat", CopyIdFormat.PLAIN).apply(GENERAL_KEY);
	public static final ConfigBoolean COPY_ITEM_ID_CLOSE_CONTAINER = new ConfigBoolean(
		"copyItemIdCloseContainer", false).apply(GENERAL_KEY);
	public static final ConfigBoolean COPY_ITEM_ID_CHAT_MESSAGE = new ConfigBoolean(
		"copyItemIdChatMessage", true).apply(GENERAL_KEY);
	// 末地折跃门传送。
	public static final ConfigBoolean DISABLE_END_GATEWAY_TELEPORT = new ConfigBoolean(
		"disableEndGatewayTeleport", true).apply(GENERAL_KEY);
	// 掉落物子项。
	public static final ConfigBoolean ITEM_DESPAWN_PLAYER_DROPPED_ONLY = new ConfigBoolean(
		"itemDespawnPlayerDroppedOnly", false).apply(GENERAL_KEY);
	public static final ConfigInteger ITEM_DESPAWN_TIME_XP_ORBS = new ConfigInteger(
		"itemDespawnTimeXpOrbs", TweakifyModConfig.DEFAULT_ITEM_DESPAWN_TICKS,
		TweakifyModConfig.MIN_ITEM_DESPAWN_TICKS, TweakifyModConfig.MAX_ITEM_DESPAWN_TICKS).apply(GENERAL_KEY);
	// 铲雪子项。
	public static final ConfigBoolean SHOVEL_SNOW_DROP_SNOWBALL = new ConfigBoolean(
		"shovelSnowDropSnowball", true).apply(GENERAL_KEY);
	public static final ConfigBoolean SHOVEL_SNOW_CONSUME_DURABILITY = new ConfigBoolean(
		"shovelSnowConsumeDurability", true).apply(GENERAL_KEY);
	// 夜视淡出时长。
	public static final ConfigInteger NIGHT_VISION_FADE_OUT_DURATION = new ConfigInteger(
		"nightVisionFadeOutDuration", TweakifyModConfig.DEFAULT_NIGHT_VISION_FADE_TICKS,
		TweakifyModConfig.MIN_NIGHT_VISION_FADE_TICKS, TweakifyModConfig.MAX_NIGHT_VISION_FADE_TICKS).apply(GENERAL_KEY);
	// 聊天框选子项。
	public static final ConfigOptionList CHAT_SELECTION_COLOR = new ConfigOptionList(
		"chatSelectionColor", ChatSelectionColor.BLUE).apply(GENERAL_KEY);
	public static final ConfigBoolean CHAT_SELECTION_AUTO_COPY = new ConfigBoolean(
		"chatSelectionAutoCopy", false).apply(GENERAL_KEY);
	// 屏幕覆盖层总开关与子项。
	public static final ConfigBooleanHotkeyed DISABLE_OVERLAY_EFFECTS = new ConfigBooleanHotkeyed(
		"disableOverlayEffects", false, "", TOGGLE_HOTKEY_SETTINGS).apply(GENERAL_KEY);
	public static final ConfigBoolean DISABLE_POWDER_SNOW_OVERLAY = new ConfigBoolean(
		"disablePowderSnowOverlay", true).apply(GENERAL_KEY);
	public static final ConfigBoolean DISABLE_LAVA_OVERLAY = new ConfigBoolean(
		"disableLavaOverlay", true).apply(GENERAL_KEY);
	public static final ConfigBoolean DISABLE_WATER_OVERLAY = new ConfigBoolean(
		"disableWaterOverlay", true).apply(GENERAL_KEY);
	public static final ConfigBoolean DISABLE_WATER_FOG = new ConfigBoolean(
		"disableWaterFog", true).apply(GENERAL_KEY);
	public static final ConfigBoolean DISABLE_LAVA_FOG = new ConfigBoolean(
		"disableLavaFog", true).apply(GENERAL_KEY);
	public static final ConfigBoolean DISABLE_POWDER_SNOW_FOG = new ConfigBoolean(
		"disablePowderSnowFog", true).apply(GENERAL_KEY);
	// 移动与挖掘。
	public static final ConfigBoolean DISABLE_SPRINT_FOV = new ConfigBoolean(
		"disableSprintFov", false).apply(GENERAL_KEY);
	public static final ConfigBoolean KEEP_MINING_PROGRESS_ON_ITEM_SWITCH = new ConfigBoolean(
		"keepMiningProgressOnItemSwitch", false).apply(GENERAL_KEY);
	// 剑穿草打生物：持剑时跳过花草直接选中生物。
	public static final ConfigBooleanHotkeyed SWORD_CUT_THROUGH_PLANTS = new ConfigBooleanHotkeyed(
		"swordCutThroughPlants", false, "", TOGGLE_HOTKEY_SETTINGS).apply(GENERAL_KEY);
	public static final ConfigOptionList CUT_THROUGH_ITEMS = new ConfigOptionList(
		"cutThroughItems", CutThroughItemMode.SWORD).apply(GENERAL_KEY);
	public static final ConfigOptionList CUT_THROUGH_PLANTS = new ConfigOptionList(
		"cutThroughPlants", CutThroughPlantMode.ALL).apply(GENERAL_KEY);
	public static final ConfigBoolean CUT_THROUGH_SNEAK_BYPASS = new ConfigBoolean(
		"cutThroughSneakBypass", true).apply(GENERAL_KEY);
	// 中键获得液体桶：准星指向水/岩浆时中键得到对应液体桶（仅创造模式）。
	public static final ConfigBooleanHotkeyed PICK_FLUID_BUCKET = new ConfigBooleanHotkeyed(
		"pickFluidBucket", false, "", TOGGLE_HOTKEY_SETTINGS).apply(GENERAL_KEY);
	// 生存模式中键取物：背包里有对应物品就切换到主手。
	public static final ConfigBooleanHotkeyed SURVIVAL_PICK_BLOCK = new ConfigBooleanHotkeyed(
		"survivalPickBlock", false, "", TOGGLE_HOTKEY_SETTINGS).apply(GENERAL_KEY);
	// 便捷焚烧：右键点击岩浆桶或打火石焚烧光标上的物品。
	public static final ConfigBooleanHotkeyed QUICK_BURN_ITEM = new ConfigBooleanHotkeyed(
		"quickBurnItem", false, "", TOGGLE_HOTKEY_SETTINGS).apply(GENERAL_KEY);
	// 不使用最后一根火把：只剩一根时不允许放置。
	public static final ConfigBoolean KEEP_LAST_TORCH = new ConfigBoolean(
		"keepLastTorch", false).apply(GENERAL_KEY);
	// 如履平地：自动跨上 1 格高的方块。
	public static final ConfigBooleanHotkeyed AUTO_STEP_UP = new ConfigBooleanHotkeyed(
		"autoStepUp", false, "", TOGGLE_HOTKEY_SETTINGS).apply(GENERAL_KEY);
	// 更好的攀爬：攀爬方块相关的移动优化。
	public static final ConfigBooleanHotkeyed BETTER_CLIMBING = new ConfigBooleanHotkeyed(
		"betterClimbing", false, "", TOGGLE_HOTKEY_SETTINGS).apply(GENERAL_KEY);
	public static final ConfigBoolean CLIMBING_FAST_DESCEND = new ConfigBoolean(
		"climbingFastDescend", true).apply(GENERAL_KEY);
	public static final ConfigBoolean CLIMBING_FASTER_ASCEND = new ConfigBoolean(
		"climbingFasterAscend", true).apply(GENERAL_KEY);
	public static final ConfigBoolean CLIMBING_JUMP_FROM_GROUND = new ConfigBoolean(
		"climbingJumpFromGround", true).apply(GENERAL_KEY);
	public static final ConfigBoolean CLIMBING_CANCEL_COLLISION = new ConfigBoolean(
		"climbingCancelCollision", true).apply(GENERAL_KEY);
	// 主动爬行：总开关 + 按住键（子项）进入爬行姿态。
	public static final ConfigBooleanHotkeyed FORCE_CRAWL = new ConfigBooleanHotkeyed(
		"forceCrawl", false, "", TOGGLE_HOTKEY_SETTINGS).apply(GENERAL_KEY);
	// allowEmpty = false：未绑定按键时 isKeybindHeld() 不会返回 true，避免默认全员爬行。
	public static final ConfigHotkey FORCE_CRAWL_KEY = new ConfigHotkey(
		"forceCrawlKey", "", KeybindSettings.PRESS_ALLOWEXTRA).apply(GENERAL_KEY);
	// 经过可攀爬方块不减速：蹭到梯子/藤蔓边缘时不再夹紧水平速度。
	public static final ConfigBoolean CLIMBABLE_PASS_NO_SLOWDOWN = new ConfigBoolean(
		"climbablePassNoSlowdown", false).apply(GENERAL_KEY);
	// 可见交易显示：村民/流浪商人头顶显示交易结果物品。
	public static final ConfigBoolean VISIBLE_TRADERS = new ConfigBoolean(
		"visibleTraders", false).apply(GENERAL_KEY);
	// 防猪灵装备切换：附近有猪灵时把背包里的金制装备换到身上，猪灵离开后换回。
	public static final ConfigBooleanHotkeyed PIGLIN_GOLD_ARMOR_SWAP = new ConfigBooleanHotkeyed(
		"piglinGoldArmorSwap", false, "", TOGGLE_HOTKEY_SETTINGS).apply(GENERAL_KEY);
	// 替换部位：自动（有哪个金装备就穿一件）或指定单部位。
	public static final ConfigOptionList PIGLIN_GOLD_ARMOR_SWAP_MODE = new ConfigOptionList(
		"piglinGoldArmorSwapMode", PiglinArmorSwapMode.AUTO).apply(GENERAL_KEY);
	// 聊天框内容记忆：关闭聊天框后保留未发送的输入内容。
	public static final ConfigBooleanHotkeyed CHAT_INPUT_MEMORY = new ConfigBooleanHotkeyed(
		"chatInputMemory", false, "", TOGGLE_HOTKEY_SETTINGS).apply(GENERAL_KEY);
	// 强制关闭加载屏幕：加载世界时立即关闭加载界面。
	public static final ConfigBoolean FORCE_CLOSE_LOADING_SCREEN = new ConfigBoolean(
		"forceCloseLoadingScreen", false).apply(GENERAL_KEY);
	public static final ConfigBoolean FORCE_CLOSE_LOADING_SCREEN_INSTANT = new ConfigBoolean(
		"forceCloseLoadingScreenInstant", false).apply(GENERAL_KEY);
	public static final ConfigBoolean FORCE_CLOSE_LOADING_SCREEN_KEEP_FIRST = new ConfigBoolean(
		"forceCloseLoadingScreenKeepFirst", false).apply(GENERAL_KEY);
	public static final ConfigBoolean FORCE_CLOSE_LOADING_SCREEN_SKIP_RESOURCE_PACK_FADE = new ConfigBoolean(
		"forceCloseLoadingScreenSkipResourcePackFade", true).apply(GENERAL_KEY);
	// 关闭资源包兼容性提示：不显示不兼容标记，也不弹确认框。
	public static final ConfigBoolean DISABLE_RESOURCE_PACK_COMPATIBILITY_CHECK = new ConfigBoolean(
		"disableResourcePackCompatibilityCheck", false).apply(GENERAL_KEY);
	public static final ConfigBoolean DISABLE_FARMLAND_TRAMPLING = new ConfigBoolean(
		"disableFarmlandTrampling", false).apply(GENERAL_KEY);
	public static final ConfigBoolean FARMLAND_TRAMPLING_PLAYER_ONLY = new ConfigBoolean(
		"farmlandTramplingPlayerOnly", false).apply(GENERAL_KEY);
	// 滚动列表记忆：关闭界面时记住滚动位置，下次打开回到该位置。
	public static final ConfigBoolean SCROLL_LIST_MEMORY = new ConfigBoolean(
		"scrollListMemory", true).apply(GENERAL_KEY);
	public static final ConfigBoolean SCROLL_LIST_MEMORY_PERSIST = new ConfigBoolean(
		"scrollListMemoryPersist", false).apply(GENERAL_KEY);
	public static final ConfigBoolean MALILIB_SCROLL_MEMORY = new ConfigBoolean(
		"malilibScrollMemory", true).apply(GENERAL_KEY);
	public static final ConfigBoolean MOD_MENU_SCROLL_MEMORY = new ConfigBoolean(
		"modMenuScrollMemory", true).apply(GENERAL_KEY);
	public static final ConfigBooleanHotkeyed ENTITY_HIGHLIGHT = new ConfigBooleanHotkeyed(
		"entityHighlight", false, "", TOGGLE_HOTKEY_SETTINGS).apply(HIGHLIGHT_KEY);
	public static final ConfigBoolean HIGHLIGHT_PRESET_1 = new ConfigBoolean(
		"highlightPreset1", false).apply(HIGHLIGHT_KEY);
	public static final ConfigBoolean HIGHLIGHT_PRESET_2 = new ConfigBoolean(
		"highlightPreset2", false).apply(HIGHLIGHT_KEY);
	public static final ConfigBoolean HIGHLIGHT_PRESET_3 = new ConfigBoolean(
		"highlightPreset3", false).apply(HIGHLIGHT_KEY);
	public static final ConfigStringList HIGHLIGHT_PRESET_1_ENTITIES = new EntityIdConfigStringList(
		"highlightPreset1Entities", ImmutableList.of()).apply(HIGHLIGHT_KEY);
	public static final ConfigStringList HIGHLIGHT_PRESET_2_ENTITIES = new EntityIdConfigStringList(
		"highlightPreset2Entities", ImmutableList.of()).apply(HIGHLIGHT_KEY);
	public static final ConfigStringList HIGHLIGHT_PRESET_3_ENTITIES = new EntityIdConfigStringList(
		"highlightPreset3Entities", ImmutableList.of()).apply(HIGHLIGHT_KEY);
	public static final ConfigStringList NETHER_PORTAL_TELEPORT_BLACKLIST = new EntityIdConfigStringList(
		"netherPortalTeleportBlacklist", ImmutableList.of()).apply(LISTS_KEY);
	public static final ConfigStringList END_PORTAL_TELEPORT_BLACKLIST = new EntityIdConfigStringList(
		"endPortalTeleportBlacklist", ImmutableList.of()).apply(LISTS_KEY);
	public static final ConfigStringList END_GATEWAY_TELEPORT_BLACKLIST = new EntityIdConfigStringList(
		"endGatewayTeleportBlacklist", ImmutableList.of()).apply(LISTS_KEY);
	public static final ConfigStringList ATTACK_RANGE_ENTITIES = new EntityIdConfigStringList(
		"attackRangeEntities", ImmutableList.of()).apply(LISTS_KEY);
	public static final ConfigStringList DISABLED_SPAWN_ENTITIES = new EntityIdConfigStringList(
		"disabledSpawnEntities", ImmutableList.of()).apply(GENERAL_KEY);
	public static final ConfigStringList PEACEFUL_MODE_MONSTER_ENTITIES = new EntityIdConfigStringList(
		"peacefulModeMonsterEntities", ImmutableList.of()).apply(GENERAL_KEY);
	public static final ConfigStringList RESTRICTED_OFFHAND_ITEMS = new ItemIdConfigStringList(
		"restrictedOffhandItems",
		ImmutableList.copyOf(TweakifyModConfig.defaultLightSourceItemIds())).apply(LISTS_KEY);
	public static final ConfigStringList MAIN_HAND_USE_COOLDOWN_ITEMS = new ItemIdConfigStringList(
		"mainHandUseCooldownItems",
		ImmutableList.copyOf(TweakifyModConfig.defaultInteractableItemIds())).apply(LISTS_KEY);
	public static final ConfigStringList OFFHAND_USE_COOLDOWN_ITEMS = new ItemIdConfigStringList(
		"offhandUseCooldownItems",
		ImmutableList.copyOf(TweakifyModConfig.defaultInteractableItemIds())).apply(LISTS_KEY);

	// 双向同步进行中时为 true：屏蔽值变更回调，避免递归与重复写盘。
	private static boolean syncing;

	// 三个预设互斥：开启一个时关闭其他两个，被关闭项的回调不再重复保存。
	private static boolean enforcingPresetExclusivity;

	private static final ImmutableList<ConfigBoolean> HIGHLIGHT_PRESETS = ImmutableList.of(
		HIGHLIGHT_PRESET_1, HIGHLIGHT_PRESET_2, HIGHLIGHT_PRESET_3);

	private static void onHighlightPresetChanged(ConfigBoolean changed) {
		if (enforcingPresetExclusivity) {
			return;
		}
		if (changed.getBooleanValue()) {
			enforcingPresetExclusivity = true;
			try {
				for (ConfigBoolean preset : HIGHLIGHT_PRESETS) {
					if (preset != changed) {
						preset.setBooleanValue(false);
					}
				}
			} finally {
				enforcingPresetExclusivity = false;
			}
		}
		syncAndSave();
	}

	// 配置项实例按名字索引：反射收集本类的静态 IConfigBase 字段。
	// 新增配置项只需声明一个字段 + 在 ConfigCatalog 登记一行，不必再同步维护多份清单。
	private static final Map<String, IConfigBase> OPTIONS_BY_NAME = indexOptions();

	private static Map<String, IConfigBase> indexOptions() {
		Map<String, IConfigBase> options = new LinkedHashMap<>();
		for (Field field : TweakifyModConfigOptions.class.getDeclaredFields()) {
			if (!Modifier.isStatic(field.getModifiers())
				|| !IConfigBase.class.isAssignableFrom(field.getType())) {
				continue;
			}
			// 仅界面使用的操作项不参与配置字段/目录校验。
			if (field.getName().equals("PROFILE_EDITOR")) {
				continue;
			}
			try {
				if (field.get(null) instanceof IConfigBase option) {
					options.put(option.getName(), option);
				}
			} catch (IllegalAccessException e) {
				throw new IllegalStateException("Cannot read option field " + field.getName(), e);
			}
		}
		return Collections.unmodifiableMap(options);
	}

	// 全部配置项，顺序与 ConfigCatalog 一致（界面树、快捷键与统计都按这个顺序）。
	public static final ImmutableList<IConfigBase> ALL_OPTIONS = buildAllOptions();

	private static ImmutableList<IConfigBase> buildAllOptions() {
		ImmutableList.Builder<IConfigBase> builder = ImmutableList.builder();
		for (String name : ConfigCatalog.optionNames()) {
			IConfigBase option = OPTIONS_BY_NAME.get(name);
			if (option == null) {
				throw new IllegalStateException("Missing MaLiLib option for config entry: " + name);
			}
			builder.add(option);
		}
		return builder.build();
	}

	// MaLiLib 的正统持久化机制：配置值一旦变化（setModified -> onValueChanged）
	// 就同步到模组配置并延迟写盘，不依赖编辑对话框关闭时的修改判定。
	// 统一按 ALL_OPTIONS 注册，新增配置项（含快捷键项）不必再手工补一行回调。
	static {
		for (IConfigBase option : ALL_OPTIONS) {
			registerValueChangeCallback(option);
		}
		registerPresetCallback(HIGHLIGHT_PRESET_1);
		registerPresetCallback(HIGHLIGHT_PRESET_2);
		registerPresetCallback(HIGHLIGHT_PRESET_3);
	}

	// 按名字取配置项实例：配置树由 ConfigCatalog 的登记顺序构建，不再手写一份结构。
	public static IConfigBase optionOf(String name) {
		return OPTIONS_BY_NAME.get(name);
	}

	// ConfigBase.setValueChangeCallback 的泛型参数是各配置项自身，遍历注册时需要擦除类型。
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void registerValueChangeCallback(IConfigBase option) {
		if (option instanceof ConfigBase base) {
			base.setValueChangeCallback(ignored -> syncAndSave(option));
		}
	}

	// 三个预设互斥：开启一个时关闭其他两个，再统一同步并保存。
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void registerPresetCallback(ConfigBoolean preset) {
		((ConfigBase) preset).setValueChangeCallback(ignored -> onHighlightPresetChanged(preset));
	}

	// 高亮分组的 prettyName 使用 highlight 前缀：按目录中的分组结构判断，不再手写一份名单。
	private static String prettyNamePrefix(String optionName) {
		return ConfigCatalog.isInsideGroup("entityHighlight", optionName) ? HIGHLIGHT_KEY : GENERAL_KEY;
	}

	private static void requireField(String fieldName) {
		if (!TweakifyModConfig.hasField(fieldName)) {
			throw new IllegalStateException("Missing TweakifyModConfig field for option: " + fieldName);
		}
	}

	// 带快捷键的配置项，注册到 MaLiLib 快捷键系统（由 ALL_OPTIONS 派生，不再手工枚举）。
	public static final ImmutableList<IHotkey> HOTKEY_OPTIONS = collectHotkeys();

	private static ImmutableList<IHotkey> collectHotkeys() {
		ImmutableList.Builder<IHotkey> builder = ImmutableList.builder();
		for (IConfigBase option : ALL_OPTIONS) {
			if (option instanceof IHotkey hotkey) {
				builder.add(hotkey);
			}
		}
		return builder.build();
	}

	// 布尔开关配置项，供配置界面统计“总数/可用/已修改/已禁用”（由 ALL_OPTIONS 派生）。
	public static final ImmutableList<IConfigBoolean> TOGGLE_OPTIONS = collectToggles();

	private static ImmutableList<IConfigBoolean> collectToggles() {
		ImmutableList.Builder<IConfigBoolean> builder = ImmutableList.builder();
		for (IConfigBase option : ALL_OPTIONS) {
			if (option instanceof IConfigBoolean toggle) {
				builder.add(toggle);
			}
		}
		return builder.build();
	}

	// 版本号只用于迁移，不对外提供界面选项。
	private static final String CONFIG_VERSION_FIELD = "configVersion";

	// 带值的配置项名即 TweakifyModConfig 字段名，启动时校验一次，避免改名后同步静默失效；
	// 纯快捷键项（如 openConfigGui）只校验对应的 hotkey 字段。
	// 同时校验目录与字段双向覆盖：漏登记或漏声明都在启动时直接失败，而不是静默丢失一项。
	// 放在最后：上面的派生列表已全部初始化，静态块执行顺序不会拿到 null。
	static {
		for (IConfigBase option : ALL_OPTIONS) {
			if (option instanceof IConfigBoolean || option instanceof IConfigStringList
				|| option instanceof IConfigOptionList || option instanceof IConfigInteger
				|| option instanceof IConfigDouble) {
				requireField(option.getName());
			}
			if (option instanceof IHotkey) {
				requireField(hotkeyFieldName(option.getName()));
			}
		}

		if (OPTIONS_BY_NAME.size() != ALL_OPTIONS.size()) {
			Set<String> orphans = new LinkedHashSet<>(OPTIONS_BY_NAME.keySet());
			ALL_OPTIONS.forEach(option -> orphans.remove(option.getName()));
			throw new IllegalStateException("Options not registered in ConfigCatalog: " + orphans);
		}

		Set<String> coveredFields = new HashSet<>(OPTIONS_BY_NAME.keySet());
		for (IHotkey hotkey : HOTKEY_OPTIONS) {
			coveredFields.add(hotkeyFieldName(hotkey.getName()));
		}
		coveredFields.add(CONFIG_VERSION_FIELD);
		for (String fieldName : TweakifyModConfig.configFieldNames()) {
			if (!coveredFields.contains(fieldName)) {
				throw new IllegalStateException("Config field without option: " + fieldName);
			}
		}
	}

	// 配置项名与 TweakifyModConfig 字段名一一对应，同步时按名字读写；
	// 新增开关只需添加字段与配置项，不必再分别维护两段镜像代码。
	public static void syncFromConfig() {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null) {
			return;
		}

		// 同步期间屏蔽值变更回调，避免每个赋值都触发一次写盘。
		boolean previousSyncing = syncing;
		syncing = true;
		try {
			for (IConfigBase option : ALL_OPTIONS) {
				applyToOption(config, option);
			}
			refreshPrettyNames();
		} finally {
			syncing = previousSyncing;
		}
		resetChatMemoryIfDisabled();
	}

	// 把配置对象的值写进 MaLiLib 选项（含可选快捷键）。
	private static void applyToOption(TweakifyModConfig config, IConfigBase option) {
		String name = option.getName();
		if (option instanceof IConfigOptionList optionList) {
			optionList.setOptionListValue(
				optionList.getDefaultOptionListValue().fromString(config.getString(name)));
		} else if (option instanceof IConfigStringList list) {
			list.setStrings(config.getStringList(name));
		} else if (option instanceof IConfigInteger integer) {
			integer.setIntegerValue(config.getInteger(name));
		} else if (option instanceof IConfigDouble doubleOption) {
			doubleOption.setDoubleValue(config.getDouble(name));
		} else if (option instanceof IConfigBoolean toggle) {
			toggle.setBooleanValue(config.getBoolean(name));
		}
		if (option instanceof IHotkey hotkey) {
			String bound = config.getString(hotkeyFieldName(name));
			// 配置写成 null 时（手工编辑、迁移工具）MaLiLib 会在 split 处抛 NPE；未绑定用空字符串表示。
			hotkey.getKeybind().setValueFromString(bound == null ? "" : bound);
		}
	}

	// 把 MaLiLib 选项的值写回配置对象（赋值触发的回调由 syncing 拦截）。
	private static void applyToConfig(TweakifyModConfig config, IConfigBase option) {
		String name = option.getName();
		if (option instanceof IConfigOptionList optionList) {
			config.setString(name, optionList.getOptionListValue().getStringValue());
		} else if (option instanceof IConfigStringList list) {
			config.setStringList(name, list.getStrings());
		} else if (option instanceof IConfigInteger integer) {
			config.setInteger(name, integer.getIntegerValue());
		} else if (option instanceof IConfigDouble doubleOption) {
			config.setDouble(name, doubleOption.getDoubleValue());
		} else if (option instanceof IConfigBoolean toggle) {
			config.setBoolean(name, toggle.getBooleanValue());
		}
		if (option instanceof IHotkey hotkey) {
			config.setString(hotkeyFieldName(name), hotkey.getKeybind().getStringValue());
		}
	}

	// 快捷键字段统一命名为 hotkey + 配置项名首字母大写。
	private static String hotkeyFieldName(String optionName) {
		return TweakifyModConfig.HOTKEY_FIELD_PREFIX
			+ Character.toUpperCase(optionName.charAt(0)) + optionName.substring(1);
	}

	// 快捷键切换提示使用 prettyName，按当前语言重新翻译一次。
	private static void refreshPrettyNames() {
		for (IHotkey hotkey : HOTKEY_OPTIONS) {
			hotkey.setPrettyName(StringUtils.translate(
				prettyNamePrefix(hotkey.getName()) + ".prettyName." + hotkey.getName()));
		}
	}

	@Override
	public void load() {
		syncFromConfig();
	}

	@Override
	public void save() {
		// 有尚未写盘的修改时先提交；随后把完整快照排到同一串行队列。
		if (savePending) {
			flushPendingSaveNow();
		}
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null) {
			return;
		}
		long covered;
		AsyncFileWriter.WriteHandle handle;
		synchronized (config) {
			covered = WRITE_SEQUENCE.get();
			handle = config.prepareSaveHandle();
			if (handle != null) {
				// 完整快照已包含当前节流修改，不用再单独写一遍。
				savePending = false;
				pendingSnapshot = null;
			}
		}
		if (handle == null) {
			TweakifyMod.LOGGER.error("Failed to submit Tweakify Mod configuration save");
			// 写盘队列已关闭：直接同步写当前内存，避免修改只留在内存里。
			Boolean sync = config.writeSynchronously();
			if (!Boolean.TRUE.equals(sync)) {
				TweakifyMod.LOGGER.error("Failed to save Tweakify Mod configuration synchronously");
			}
			return;
		}
		long previousSynced = lastSyncedSequence.get();
		long advancedSynced = lastSyncedSequence.accumulateAndGet(covered, Math::max);
		// 完整快照已提交：先乐观标记覆盖本次提交前的所有异步写。
		boolean callbackQueued = AsyncFileWriter.execute(() -> {
			Boolean written = AsyncFileWriter.await(handle, SAVE_CALLBACK_WAIT_MILLIS, true);
			if (Boolean.TRUE.equals(written)) {
				discardDeferredUpTo(advancedSynced);
				return;
			}
			TweakifyMod.LOGGER.error("Failed to save Tweakify Mod configuration");
			handleSaveFailure(config, advancedSynced, previousSynced);
		});
		if (!callbackQueued) {
			// 执行器已关闭，失败回调不会执行：先确认整份写是否已经成功，
			// 避免把可能已落盘的新值在内存中回滚（内存旧值/磁盘新值的反向不一致）。
			Boolean written = AsyncFileWriter.await(handle, SAVE_CALLBACK_WAIT_MILLIS, false);
			if (Boolean.TRUE.equals(written)) {
				discardDeferredUpTo(advancedSynced);
				return;
			}
			if (written == null) {
				TweakifyMod.LOGGER.warn("Unable to confirm the final config write before shutdown; keeping the in-memory state");
				return;
			}
			// 明确失败：同步兜底；成功则丢弃被覆盖的延迟结果，仍失败才回退并重放。
			// 锁超时（null）不触发回退：竞争者可能正在写当前内存快照，回退会造成反向漂移。
			Boolean sync = config.writeSynchronously();
			handleSaveFailure(sync, advancedSynced, previousSynced);
		}
	}

	// 整份保存失败后的收敛：提交兜底写并等它的真实结果（不是“已入队”）；
	// 执行器已关闭时同步写；两者都失败才回退乐观推进的序号。
	static void handleSaveFailure(TweakifyModConfig config, long advancedSequence, long previousSequence) {
		AsyncFileWriter.WriteHandle fallback = config.submitSaveAsyncHandle();
		if (fallback == null) {
			Boolean sync = config.writeSynchronously();
			handleSaveFailure(sync, advancedSequence, previousSequence);
			return;
		}
		// 回调排在兜底写之后，await 立即返回，不会自等待。
		boolean callbackQueued = AsyncFileWriter.execute(() -> {
			Boolean written = AsyncFileWriter.await(fallback, SAVE_CALLBACK_WAIT_MILLIS, false);
			handleSaveFailure(Boolean.TRUE.equals(written), advancedSequence, previousSequence);
		});
		if (!callbackQueued) {
			Boolean written = AsyncFileWriter.await(fallback, SAVE_CALLBACK_WAIT_MILLIS, false);
			handleSaveFailure(Boolean.TRUE.equals(written), advancedSequence, previousSequence);
		}
	}

	// 包私有：decision + CAS 回退 + 重放被跳过的失败，测试可直接驱动。
	static void handleSaveFailure(boolean fallbackWritten, long advancedSequence, long previousSequence) {
		handleSaveFailure(Boolean.valueOf(fallbackWritten), advancedSequence, previousSequence);
	}

	// Boolean：true=写成功，false=写失败，null=锁超时/无法确认（不回退，避免反向漂移）。
	static void handleSaveFailure(Boolean fallbackWritten, long advancedSequence, long previousSequence) {
		if (Boolean.TRUE.equals(fallbackWritten)) {
			discardDeferredUpTo(advancedSequence);
			return;
		}
		if (fallbackWritten == null) {
			TweakifyMod.LOGGER.warn("Timed out waiting for the config write lock; keeping the optimistic sequence");
			return;
		}
		if (rollbackSyncedSequence(advancedSequence, previousSequence)) {
			replayDeferredFailures(previousSequence, advancedSequence);
		}
	}

	// 只回退本次乐观推进；若已有更新的成功推进，CAS 失败即保留新值。
	static boolean rollbackSyncedSequence(long advancedSequence, long previousSequence) {
		return lastSyncedSequence.compareAndSet(advancedSequence, previousSequence);
	}

	// 包私有：测试直接检查序号推进/回退。
	static void setSyncedSequenceForTest(long value) {
		lastSyncedSequence.set(value);
	}

	static long syncedSequenceForTest() {
		return lastSyncedSequence.get();
	}

	@Override
	public void onConfigsChanged() {
		syncAndSave();
	}

	// 写盘节流：滑块拖动等高频修改会连续触发几十次回调，
	// 这里合并成最多每 MIN_SAVE_INTERVAL_MS 写一次；客户端 tick 与配置保存入口负责兜底刷出。
	private static final long MIN_SAVE_INTERVAL_MS = 250L;
	// 异步保存回调等待上限：回调排在本份写盘之后，正常应瞬时返回。
	private static final long SAVE_CALLBACK_WAIT_MILLIS = 1000L;
	private static boolean savePending;
	private static long lastSaveAtMillis;
	private static TweakifyModConfig.Snapshot pendingSnapshot;

	// 异步写盘：序列化留在客户端线程（有锁、亚毫秒级），只有磁盘 IO 交给后台线程。
	// 每次 flush 都在配置锁内完成「序列化 + 提交到全局串行队列」，因此队列顺序等于快照
	// 新旧顺序；即使磁盘慢、前面还有旧写未完成，也不会出现旧快照晚于新快照落盘。
	private static final Queue<SaveResult> SAVE_RESULTS = new ConcurrentLinkedQueue<>();
	// 被乐观推进跳过的失败结果：整份保存失败并回退序号后，按序号重放，重新参与回滚判定。
	private static final Queue<SaveResult> DEFERRED_RESULTS = new ConcurrentLinkedQueue<>();
	private static final int MAX_DEFERRED_RESULTS = 8;

	// 写盘序号：每次 flush 提交 +1。Options.save() 提交完整快照时会乐观推进 lastSyncedSequence
	//（覆盖本次提交前已入队的异步写），用于识别“结果入队时已被更晚的整份保存覆盖”的过期失败，
	// 避免旧失败结果反向回滚新值。整份保存失败且无任何兜底写时，会用 CAS 回退这次推进。
	private static final AtomicLong WRITE_SEQUENCE = new AtomicLong();
	private static final AtomicLong lastSyncedSequence = new AtomicLong();

	// 节流 flush 的在途上限：磁盘慢（网盘/杀软/机械盘）时，生产速度可能高于消费速度，
	// 超过上限就保留待写标记，等下一个节流窗口再提交，避免每 250ms 一份全量 JSON 无上限堆积。
	// 注意：显式保存（save/applyAndSave/退出兜底）不受此限制，必须入队或同步写。
	private static final int MAX_IN_FLIGHT_FLUSHES = 4;
	private static final AtomicInteger IN_FLIGHT_FLUSHES = new AtomicInteger();

	private record SaveResult(boolean success, TweakifyModConfig.Snapshot snapshot,
						 TweakifyModConfig.Snapshot afterSnapshot, long sequence) {
	}

	// 单配置项变化：只同步这一项，避免每次切换开关都清洗 8 个列表并重建缓存。
	static void syncAndSave(IConfigBase changed) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (syncing || config == null || changed == null) {
			return;
		}
		syncing = true;
		try {
			markPending(config);
			applyToConfig(config, changed);
			resetChatMemoryIfDisabled();
			savePending = true;
		} finally {
			syncing = false;
		}
		flushPendingSave();
	}

	// 全量同步：快捷键编辑结束、配置界面重建等没有明确变化项时使用（低频）。
	static void syncAndSave() {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (syncing || config == null) {
			return;
		}
		syncing = true;
		try {
			markPending(config);
			for (IConfigBase option : ALL_OPTIONS) {
				applyToConfig(config, option);
			}
			resetChatMemoryIfDisabled();
			savePending = true;
		} finally {
			syncing = false;
		}
		flushPendingSave();
	}

	// 关闭聊天输入记忆时立即清空草稿，再次开启后从空开始。
	private static void resetChatMemoryIfDisabled() {
		if (!CHAT_INPUT_MEMORY.getBooleanValue()) {
			ChatInputMemoryState.reset();
		}
	}

	// 本批修改开始前才拍快照：同一批高频变化（如拖动滑块）共用一份回滚快照。
	private static void markPending(TweakifyModConfig config) {
		if (!savePending) {
			pendingSnapshot = config.snapshot();
		}
	}

	// 由客户端 tick 调用：超过节流间隔才真正写盘，避免高频 IO。
	static void flushPendingSave() {
		if (savePending && System.currentTimeMillis() - lastSaveAtMillis >= MIN_SAVE_INTERVAL_MS) {
			flushPendingSaveNow();
		}
	}

	// 立即写盘：序列化在客户端线程完成，磁盘 IO 交给后台线程，避免拖动滑块时掉帧。
	// 「序列化 + 提交」必须在同一把配置锁内完成，保证提交顺序就是快照新旧顺序：
	// 否则另一个线程的同步保存可能先提交，而这里的旧快照稍后才入队，把新内容覆盖掉。
	static void flushPendingSaveNow() {
		if (!savePending) {
			return;
		}
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null) {
			savePending = false;
			pendingSnapshot = null;
			return;
		}
		TweakifyModConfig.Snapshot snapshot = pendingSnapshot;
		boolean serializationFailed = false;
		synchronized (config) {
			if (IN_FLIGHT_FLUSHES.get() >= MAX_IN_FLIGHT_FLUSHES) {
				// 队列积压：保留本批修改与回滚快照，等下一个节流窗口再提交。
				lastSaveAtMillis = System.currentTimeMillis();
				return;
			}
			String serialized = null;
			TweakifyModConfig.Snapshot after = null;
			try {
				serialized = config.toJson();
				after = config.snapshot();
			} catch (RuntimeException e) {
				TweakifyMod.LOGGER.error("Failed to serialize Tweakify Mod configuration", e);
				serializationFailed = true;
			}
			savePending = false;
			pendingSnapshot = null;
			lastSaveAtMillis = System.currentTimeMillis();
			if (!serializationFailed) {
				String json = serialized;
				TweakifyModConfig.Snapshot afterSnapshot = after;
				long sequence = WRITE_SEQUENCE.incrementAndGet();
				// 落盘序号在提交时分配：任务延迟执行时不能覆盖更晚提交的写。
				long writeSequence = config.allocateWriteSequence();
				IN_FLIGHT_FLUSHES.incrementAndGet();
				// 只入队、不等待：队列由后台线程按提交顺序串行执行。
				// 入队失败（执行器已关闭）时立即退回计数，避免永久占掉一个名额。
				if (!AsyncFileWriter.execute(() -> {
					try {
						writeConfig(json, snapshot, afterSnapshot, sequence, writeSequence);
					} finally {
						IN_FLIGHT_FLUSHES.decrementAndGet();
					}
				})) {
					IN_FLIGHT_FLUSHES.decrementAndGet();
				}
			}
		}
		if (serializationFailed) {
			// 序列化失败等同于写盘失败：在客户端线程回滚，不进入后台队列。
			config.restoreSnapshot(snapshot);
			syncFromConfig();
		}
	}

	// 后台写盘任务：只写已序列化好的文本，不访问配置字段，因此不会与配置锁互相阻塞。
	private static void writeConfig(String json, TweakifyModConfig.Snapshot snapshot,
								 TweakifyModConfig.Snapshot afterSnapshot, long sequence, long writeSequence) {
		boolean success;
		try {
			TweakifyModConfig config = TweakifyMod.CONFIG;
			success = config != null && config.writeJson(json, writeSequence);
		} catch (RuntimeException e) {
			// 非 IOException 的写盘异常（路径非法、安全策略等）也必须产出失败结果：
			// 否则 pollSaveResults 永远收不到这次结果，回滚与日志会一起静默丢失。
			TweakifyMod.LOGGER.error("Unexpected error while writing Tweakify Mod configuration", e);
			success = false;
		}
		SAVE_RESULTS.add(new SaveResult(success, snapshot, afterSnapshot, sequence));
	}

	// 客户端 tick 调用：把后台写盘结果取回客户端线程处理。
	static void pollSaveResults() {
		SaveResult result;
		while ((result = SAVE_RESULTS.poll()) != null) {
			if (result.success()) {
				continue;
			}
			TweakifyMod.LOGGER.error("Failed to save Tweakify Mod configuration");
			// 这份失败写已被更晚成功的整份同步写覆盖：转存到延迟队列，
			// 若该整份保存随后失败并回退序号，这些结果会被重新放回并回滚内存。
			if (result.sequence() <= lastSyncedSequence.get()) {
				deferFailure(result);
				continue;
			}
			// 之后已提交了新修改（可能尚未写盘）：不回滚，避免把更新的值一起冲掉。
			if (result.sequence() < WRITE_SEQUENCE.get()) {
				continue;
			}
			TweakifyModConfig config = TweakifyMod.CONFIG;
			if (config != null) {
				applyFailedWriteRollback(config, result.snapshot(), result.afterSnapshot());
			}
		}
	}

	// 异步写盘失败后的统一收敛：字段级回滚 + 刷新待写快照 + 兜底写当前内存。
	// 刷新 pendingSnapshot 很关键：否则下一批 flush 仍以“含回滚前值”的旧快照为 before，
	// 会把刚回滚的字段再次当成“本次修改”写回磁盘（静默复活）。
	static void applyFailedWriteRollback(TweakifyModConfig config, TweakifyModConfig.Snapshot before,
										 TweakifyModConfig.Snapshot after) {
		config.rollbackChangedFieldsIfUnchanged(before, after);
		syncFromConfig();
		if (savePending) {
			pendingSnapshot = config.snapshot();
		}
		config.submitSaveAsync();
	}

	// 包私有：测试模拟“一批修改尚未写盘”。
	static void setPendingForTest(TweakifyModConfig.Snapshot snapshot) {
		savePending = true;
		pendingSnapshot = snapshot;
	}

	// 包私有：测试确认 pending 快照已经与当前内存一致。
	static boolean pendingMatchesForTest(TweakifyModConfig config) {
		return pendingSnapshot != null && pendingSnapshot.hasSameValuesAs(config.snapshot());
	}

	// 包私有：测试注入一条写盘结果，验证序号守卫与回滚路径。
	static void enqueueSaveResultForTest(boolean success, TweakifyModConfig.Snapshot snapshot,
										 TweakifyModConfig.Snapshot afterSnapshot, long sequence) {
		SAVE_RESULTS.add(new SaveResult(success, snapshot, afterSnapshot, sequence));
	}

	// 整份保存期间被跳过的失败结果不能直接丢弃；有界保留，回退序号后重放。
	private static void deferFailure(SaveResult result) {
		DEFERRED_RESULTS.add(result);
		while (DEFERRED_RESULTS.size() > MAX_DEFERRED_RESULTS) {
			DEFERRED_RESULTS.poll();
			TweakifyMod.LOGGER.warn("Deferred save-result queue overflow; dropping the oldest failure");
		}
	}

	// 整份保存成功覆盖到 sequence：延迟结果已无回滚价值，丢弃。
	private static void discardDeferredUpTo(long sequence) {
		DEFERRED_RESULTS.removeIf(result -> result.sequence() <= sequence);
	}

	// 回退序号后重放本次推进覆盖的失败结果，让后续 poll 能重新回滚内存。
	private static void replayDeferredFailures(long previousSequence, long advancedSequence) {
		for (SaveResult result : DEFERRED_RESULTS) {
			if (result.sequence() > previousSequence && result.sequence() <= advancedSequence
				&& DEFERRED_RESULTS.remove(result)) {
				SAVE_RESULTS.add(result);
			}
		}
	}

	// 包私有：测试之间清理写盘节流与序号状态，避免跨测试互相影响。
	static void resetForTest() {
		savePending = false;
		pendingSnapshot = null;
		lastSaveAtMillis = 0L;
		WRITE_SEQUENCE.set(0L);
		lastSyncedSequence.set(0L);
		IN_FLIGHT_FLUSHES.set(0);
		SAVE_RESULTS.clear();
		DEFERRED_RESULTS.clear();
	}

	// 客户端退出：saveForShutdown 会把这次写盘排在同一串行队列并等待；超时也不取消任务。
	// 若等待仍失败，随后的 shutdown 可能 shutdownNow 丢弃排队任务，因此在关闭线程
	// 直接同步写一份最终快照，保证最后一次修改不丢。
	static void flushOnShutdown() {
		savePending = false;
		pendingSnapshot = null;
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null) {
			return;
		}
		if (config.saveForShutdown()) {
			return;
		}
		// 先停止写盘线程（等待/丢弃），再同步写，避免旧任务在同步写之后覆盖新内容。
		if (!AsyncFileWriter.shutdown(2000L)) {
			TweakifyMod.LOGGER.warn("File writer did not terminate before the synchronous fallback write");
		}
		Boolean sync = config.writeSynchronously();
		if (!Boolean.TRUE.equals(sync)) {
			TweakifyMod.LOGGER.error("Failed to write Tweakify Mod configuration synchronously on shutdown");
		}
	}
}
