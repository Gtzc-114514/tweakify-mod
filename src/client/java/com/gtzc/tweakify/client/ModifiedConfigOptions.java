package com.gtzc.tweakify.client;

import com.google.common.collect.ImmutableList;
import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import fi.dy.masa.malilib.config.options.ConfigStringList;

/**
 * 「修改」标签的配置项声明。
 *
 * <p>字段通过 {@link TweakifyModConfigOptions} 反射按名字收集，界面顺序由
 * {@code ConfigCatalog} 决定，本文件的声明顺序不影响界面与命令顺序。
 * 新增配置项时在这里加字段，并在 {@code ConfigCatalog} 登记分类。
 */
@SuppressWarnings("unused")
public final class ModifiedConfigOptions {
	private ModifiedConfigOptions() {
	}

	// 掉落物消失时间（tick）：默认 6000 = 原版 5 分钟，滑块范围 3 秒 ~ 30 分钟。
	public static final ConfigInteger ITEM_DESPAWN_TIME = new ConfigInteger(
		"itemDespawnTime", TweakifyModConfig.DEFAULT_ITEM_DESPAWN_TICKS,
		TweakifyModConfig.MIN_ITEM_DESPAWN_TICKS,
		TweakifyModConfig.MAX_ITEM_DESPAWN_TICKS).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean DISABLE_USE_ITEM_SLOWDOWN = new ConfigBoolean(
		"disableUseItemSlowdown", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 保留原版使用减速的百分比（0 = 完全取消，100 = 原版相同）。
	public static final ConfigInteger USE_ITEM_SLOWDOWN_KEEP_PERCENT = new ConfigInteger(
		"useItemSlowdownKeepPercent", TweakifyModConfig.DEFAULT_USE_ITEM_SLOWDOWN_KEEP_PERCENT,
		TweakifyModConfig.MIN_USE_ITEM_SLOWDOWN_KEEP_PERCENT,
		TweakifyModConfig.MAX_USE_ITEM_SLOWDOWN_KEEP_PERCENT).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigStringList USE_ITEM_SLOWDOWN_EXCEPTIONS = new ItemIdConfigStringList(
		"useItemSlowdownExceptions", ImmutableList.of()).apply(ConfigOptionKeys.LISTS_KEY);

	public static final ConfigBoolean DISABLE_HURT_SHAKE = new ConfigBoolean(
		"disableHurtShake", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 关闭受伤抖动时保留死亡镜头倾斜。
	public static final ConfigBoolean HURT_SHAKE_KEEP_DEATH_TILT = new ConfigBoolean(
		"hurtShakeKeepDeathTilt", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean HURT_SHAKE_PLAYERS_ONLY = new ConfigBoolean(
		"hurtShakePlayersOnly", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean DISABLE_HURT_TINT = new ConfigBoolean(
		"disableHurtTint", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 关闭红色覆盖时保留受伤闪白。
	public static final ConfigBoolean HURT_TINT_KEEP_WHITE_FLASH = new ConfigBoolean(
		"hurtTintKeepWhiteFlash", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean HURT_TINT_PLAYERS_ONLY = new ConfigBoolean(
		"hurtTintPlayersOnly", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean DISABLE_UNDERWATER_MINING_PENALTY = new ConfigBoolean(
		"disableUnderwaterMiningPenalty", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 保留的水下挖掘惩罚百分比（0 = 完全去掉，100 = 原版）。
	public static final ConfigInteger UNDERWATER_MINING_PENALTY_KEEP = new ConfigInteger(
		"underwaterMiningPenaltyKeep", TweakifyModConfig.DEFAULT_MINING_PENALTY_KEEP_PERCENT,
		TweakifyModConfig.MIN_MINING_PENALTY_KEEP_PERCENT,
		TweakifyModConfig.MAX_MINING_PENALTY_KEEP_PERCENT).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean UNDERWATER_MINING_PENALTY_SWIMMING_ONLY = new ConfigBoolean(
		"underwaterMiningPenaltySwimmingOnly", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean DISABLE_AIR_MINING_PENALTY = new ConfigBoolean(
		"disableAirMiningPenalty", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 保留的空中挖掘惩罚百分比（0 = 完全去掉，100 = 原版）。
	public static final ConfigInteger AIR_MINING_PENALTY_KEEP = new ConfigInteger(
		"airMiningPenaltyKeep", TweakifyModConfig.DEFAULT_MINING_PENALTY_KEEP_PERCENT,
		TweakifyModConfig.MIN_MINING_PENALTY_KEEP_PERCENT,
		TweakifyModConfig.MAX_MINING_PENALTY_KEEP_PERCENT).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean AIR_MINING_PENALTY_ONLY_WHEN_FALLING = new ConfigBoolean(
		"airMiningPenaltyOnlyWhenFalling", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean DISABLE_CURSE_ENCHANTMENT_RESTRICTION = new ConfigBoolean(
		"disableCurseEnchantmentRestriction", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 诅咒移除范围：全部 / 仅绑定诅咒 / 仅消失诅咒。
	public static final ConfigOptionList CURSE_REMOVAL_SCOPE = new ConfigOptionList(
		"curseRemovalScope", CurseRemovalScope.ALL).apply(ConfigOptionKeys.GENERAL_KEY);

	// 禁用附魔过于昂贵：铁砧花费达到 40 级时仍可取出结果。
	public static final ConfigBoolean DISABLE_TOO_EXPENSIVE = new ConfigBoolean(
		"disableTooExpensive", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 可承受的最大经验花费（0 = 不限制）与超出时仅提示不阻止。
	public static final ConfigInteger TOO_EXPENSIVE_MAX_LEVEL_COST = new ConfigInteger(
		"tooExpensiveMaxLevelCost", TweakifyModConfig.DEFAULT_TOO_EXPENSIVE_MAX_LEVEL_COST,
		TweakifyModConfig.MIN_TOO_EXPENSIVE_MAX_LEVEL_COST,
		TweakifyModConfig.MAX_TOO_EXPENSIVE_MAX_LEVEL_COST).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean TOO_EXPENSIVE_WARN_ONLY = new ConfigBoolean(
		"tooExpensiveWarnOnly", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 移除附魔冲突：铁砧合并时忽略附魔互斥限制。
	public static final ConfigBoolean REMOVE_ENCHANTMENT_CONFLICTS = new ConfigBoolean(
		"removeEnchantmentConflicts", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 仅解除同一互斥组（同类附魔）内部的冲突。
	public static final ConfigBoolean REMOVE_ENCHANTMENT_CONFLICTS_SAME_GROUP_ONLY = new ConfigBoolean(
		"removeEnchantmentConflictsSameGroupOnly", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean REMOVE_ENCHANTMENT_CONFLICTS_REQUIRE_SNEAK = new ConfigBoolean(
		"removeEnchantmentConflictsRequireSneak", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 附魔描述：在物品提示中为每个附魔追加一行描述。
	public static final ConfigBoolean ENCHANTMENT_DESCRIPTIONS = new ConfigBoolean(
		"enchantmentDescriptions", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 只在按住潜行键时追加附魔描述。
	public static final ConfigBoolean ENCHANTMENT_DESCRIPTIONS_SNEAK_ONLY = new ConfigBoolean(
		"enchantmentDescriptionsSneakOnly", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean ENCHANTMENT_DESCRIPTIONS_INCLUDE_CURSES = new ConfigBoolean(
		"enchantmentDescriptionsIncludeCurses", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// 第三人称自身名字：第三/第二人称显示玩家自己的名字。
	public static final ConfigBoolean THIRD_PERSON_PLAYER_NAME = new ConfigBoolean(
		"thirdPersonPlayerName", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 只在潜行时显示自己的名字。
	public static final ConfigBoolean THIRD_PERSON_PLAYER_NAME_SNEAK_ONLY = new ConfigBoolean(
		"thirdPersonPlayerNameSneakOnly", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean THIRD_PERSON_PLAYER_NAME_IGNORE_HUD_HIDDEN = new ConfigBoolean(
		"thirdPersonPlayerNameIgnoreHudHidden", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 雪傀儡不受群系影响：温暖/干燥群系不再因群系属性融化；雨/水仍会伤害，仅单人/局域网生效。
	public static final ConfigBoolean SNOW_GOLEM_BIOME_IMMUNITY = new ConfigBoolean(
		"snowGolemBiomeImmunity", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 只在主世界生效。
	public static final ConfigBoolean SNOW_GOLEM_BIOME_IMMUNITY_OVERWORLD_ONLY = new ConfigBoolean(
		"snowGolemBiomeImmunityOverworldOnly", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean SNOW_GOLEM_BIOME_IMMUNITY_ONLY_WHEN_RAINING = new ConfigBoolean(
		"snowGolemBiomeImmunityOnlyWhenRaining", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 超平坦无史莱姆：超平坦世界中不再生成史莱姆。
	public static final ConfigBoolean DISABLE_FLAT_WORLD_SLIME_SPAWN = new ConfigBoolean(
		"disableFlatWorldSlimeSpawn", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean FLAT_WORLD_SLIME_SPAWN_ALL_WORLDS = new ConfigBoolean(
		"flatWorldSlimeSpawnAllWorlds", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigInteger FLAT_WORLD_SLIME_SPAWN_BELOW_Y = new ConfigInteger(
		"flatWorldSlimeSpawnBelowY", TweakifyModConfig.DEFAULT_FLAT_SLIME_Y,
		TweakifyModConfig.MIN_FLAT_SLIME_Y,
		TweakifyModConfig.MAX_FLAT_SLIME_Y).apply(ConfigOptionKeys.GENERAL_KEY);

	// 更好的隐身：隐身后去掉药水粒子，装备与第三人称手持物品也隐藏。
	public static final ConfigBoolean BETTER_INVISIBILITY = new ConfigBoolean(
		"betterInvisibility", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 锻造模板不消耗：锻造台取出成品时保留模板。
	public static final ConfigBoolean DISABLE_SMITHING_TEMPLATE_CONSUMPTION = new ConfigBoolean(
		"disableSmithingTemplateConsumption", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 锻造模板保留概率（0 ~ 100）。
	public static final ConfigInteger SMITHING_TEMPLATE_KEEP_CHANCE = new ConfigInteger(
		"smithingTemplateKeepChance", TweakifyModConfig.DEFAULT_SMITHING_TEMPLATE_KEEP_CHANCE,
		TweakifyModConfig.MIN_SMITHING_TEMPLATE_KEEP_CHANCE,
		TweakifyModConfig.MAX_SMITHING_TEMPLATE_KEEP_CHANCE).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean SMITHING_TEMPLATE_SURVIVAL_ONLY = new ConfigBoolean(
		"smithingTemplateSurvivalOnly", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 夜视淡出：夜视结束前平滑淡出。
	public static final ConfigBoolean NIGHT_VISION_FADE_OUT = new ConfigBoolean(
		"nightVisionFadeOut", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigOptionList NIGHT_VISION_FADE_OUT_CURVE = new ConfigOptionList(
		"nightVisionFadeOutCurve", NightVisionCurve.SMOOTH).apply(ConfigOptionKeys.GENERAL_KEY);

	// 附魔等级使用阿拉伯数字。
	public static final ConfigBoolean ENCHANTMENT_LEVEL_ARABIC = new ConfigBoolean(
		"enchantmentLevelArabic", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 数字样式：plain（锋利 3）/ parentheses（锋利 (3)）。
	public static final ConfigOptionList ENCHANTMENT_LEVEL_ARABIC_STYLE = new ConfigOptionList(
		"enchantmentLevelArabicStyle", EnchantmentLevelStyle.PLAIN).apply(ConfigOptionKeys.GENERAL_KEY);

	// 只在附魔等级不低于该值时使用阿拉伯数字。
	public static final ConfigInteger ENCHANTMENT_LEVEL_ARABIC_MIN_LEVEL = new ConfigInteger(
		"enchantmentLevelArabicMinLevel", TweakifyModConfig.DEFAULT_ENCHANTMENT_ARABIC_LEVEL,
		TweakifyModConfig.MIN_ENCHANTMENT_ARABIC_LEVEL,
		TweakifyModConfig.MAX_ENCHANTMENT_ARABIC_LEVEL).apply(ConfigOptionKeys.GENERAL_KEY);

	// 疣块分解：合成格里单独放下界疣块时产出 9 个下界疣。
	public static final ConfigBoolean WART_BLOCK_DECOMPOSITION = new ConfigBoolean(
		"wartBlockDecomposition", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 疣块分解产出数量（1 ~ 9）。
	public static final ConfigInteger WART_BLOCK_DECOMPOSITION_OUTPUT = new ConfigInteger(
		"wartBlockDecompositionOutput", TweakifyModConfig.DEFAULT_WART_DECOMPOSITION_OUTPUT,
		TweakifyModConfig.MIN_WART_DECOMPOSITION_OUTPUT,
		TweakifyModConfig.MAX_WART_DECOMPOSITION_OUTPUT).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean WART_BLOCK_DECOMPOSITION_REQUIRE_SNEAK = new ConfigBoolean(
		"wartBlockDecompositionRequireSneak", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 鞘翅水中飞行：滑翔时不再被流体移动逻辑接管。
	public static final ConfigBoolean ELYTRA_WATER_FLIGHT = new ConfigBoolean(
		"elytraWaterFlight", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean ELYTRA_WATER_FLIGHT_ONLY_WATER = new ConfigBoolean(
		"elytraWaterFlightOnlyWater", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean ELYTRA_WATER_FLIGHT_ONLY_SUBMERGED = new ConfigBoolean(
		"elytraWaterFlightOnlySubmerged", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 禁用地狱门特效：不再显示紫色覆盖层、镜头旋转与传送门音效。
	public static final ConfigBoolean DISABLE_NETHER_PORTAL_EFFECT = new ConfigBoolean(
		"disableNetherPortalEffect", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 禁用床太远：离床太远时也能直接入睡。
	public static final ConfigBoolean DISABLE_BED_TOO_FAR = new ConfigBoolean(
		"disableBedTooFar", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 允许入睡的最大距离（格，0 = 不限制）。
	public static final ConfigInteger BED_TOO_FAR_MAX_DISTANCE = new ConfigInteger(
		"bedTooFarMaxDistance", TweakifyModConfig.DEFAULT_BED_TOO_FAR_DISTANCE,
		TweakifyModConfig.MIN_BED_TOO_FAR_DISTANCE,
		TweakifyModConfig.MAX_BED_TOO_FAR_DISTANCE).apply(ConfigOptionKeys.GENERAL_KEY);

	// 铁傀儡进矿车：空矿车碰到铁傀儡时让铁傀儡坐进矿车。
	public static final ConfigBoolean IRON_GOLEM_RIDE_MINECART = new ConfigBoolean(
		"ironGolemRideMinecart", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 只在玩家附近自动上车（0 ~ 64 格，0 = 不限制）。
	public static final ConfigInteger IRON_GOLEM_RIDE_MINECART_MAX_DISTANCE = new ConfigInteger(
		"ironGolemRideMinecartMaxDistance", TweakifyModConfig.DEFAULT_IRON_GOLEM_PICKUP_DISTANCE,
		TweakifyModConfig.MIN_IRON_GOLEM_PICKUP_DISTANCE,
		TweakifyModConfig.MAX_IRON_GOLEM_PICKUP_DISTANCE).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean IRON_GOLEM_RIDE_MINECART_EMPTY_HAND_ONLY = new ConfigBoolean(
		"ironGolemRideMinecartEmptyHandOnly", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 穿牌打开容器：准星对准告示牌时直接打开它背后的容器。
	public static final ConfigBoolean SIGN_OPEN_CONTAINER = new ConfigBoolean(
		"signOpenContainer", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 潜行时保持原版告示牌交互。
	public static final ConfigBoolean SIGN_OPEN_CONTAINER_SNEAK_BYPASS = new ConfigBoolean(
		"signOpenContainerSneakBypass", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean SIGN_OPEN_CONTAINER_SKIP_WAXED = new ConfigBoolean(
		"signOpenContainerSkipWaxed", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 不死图腾背包生效：图腾在快捷栏或背包即可触发，不再要求主手/副手。
	public static final ConfigBoolean TOTEM_FROM_INVENTORY = new ConfigBoolean(
		"totemFromInventory", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 背包查找是否包含快捷栏。
	public static final ConfigBoolean TOTEM_INVENTORY_INCLUDE_HOTBAR = new ConfigBoolean(
		"totemInventoryIncludeHotbar", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// 优先从快捷栏开始查找。
	public static final ConfigBoolean TOTEM_INVENTORY_HOTBAR_FIRST = new ConfigBoolean(
		"totemInventoryHotbarFirst", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// 禁用蛛网减速：蜘蛛网不再粘住并限制移动。
	public static final ConfigBoolean DISABLE_COBWEB_SLOWDOWN = new ConfigBoolean(
		"disableCobwebSlowdown", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean COBWEB_SLOWDOWN_ALL_ENTITIES = new ConfigBoolean(
		"cobwebSlowdownAllEntities", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean COBWEB_SLOWDOWN_KEEP_JUMP_PENALTY = new ConfigBoolean(
		"cobwebSlowdownKeepJumpPenalty", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 禁用液体推动：水流与岩浆流不再施加水平推力。
	public static final ConfigBoolean DISABLE_FLUID_PUSH = new ConfigBoolean(
		"disableFluidPush", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean DISABLE_WATER_PUSH = new ConfigBoolean(
		"disableWaterPush", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean DISABLE_LAVA_PUSH = new ConfigBoolean(
		"disableLavaPush", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// 禁用粘液块减速：站在粘液块上不再被乘算降速。
	public static final ConfigBoolean DISABLE_SLIME_BLOCK_SLOWDOWN = new ConfigBoolean(
		"disableSlimeBlockSlowdown", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean DISABLE_SLIME_BLOCK_BOUNCE = new ConfigBoolean(
		"disableSlimeBlockBounce", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean DISABLE_SLIME_BLOCK_SLOWDOWN_PLAYERS_ONLY = new ConfigBoolean(
		"disableSlimeBlockSlowdownPlayersOnly", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 禁用蜂蜜块减速：蜂蜜块不再降低移动速度。
	public static final ConfigBoolean DISABLE_HONEY_BLOCK_SLOWDOWN = new ConfigBoolean(
		"disableHoneyBlockSlowdown", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean DISABLE_HONEY_BLOCK_STICK = new ConfigBoolean(
		"disableHoneyBlockStick", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// 连接方块轮廓：准星对准门/床/双箱等连接方块时显示整体轮廓。
	public static final ConfigBoolean CONNECTED_BLOCK_OUTLINE = new ConfigBoolean(
		"connectedBlockOutline", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 门使用 2 格视觉高度（关闭时门保持原版轮廓）。
	public static final ConfigBoolean CONNECTED_BLOCK_OUTLINE_DOOR_FULL_HEIGHT = new ConfigBoolean(
		"connectedBlockOutlineDoorFullHeight", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean CONNECTED_BLOCK_OUTLINE_DOORS = new ConfigBoolean(
		"connectedBlockOutlineDoors", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean CONNECTED_BLOCK_OUTLINE_CHESTS = new ConfigBoolean(
		"connectedBlockOutlineChests", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean CONNECTED_BLOCK_OUTLINE_BEDS = new ConfigBoolean(
		"connectedBlockOutlineBeds", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean CONNECTED_BLOCK_OUTLINE_PLANTS = new ConfigBoolean(
		"connectedBlockOutlinePlants", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// 更好的隐身子项。
	public static final ConfigBoolean BETTER_INVISIBILITY_EFFECT_PARTICLES = new ConfigBoolean(
		"betterInvisibilityEffectParticles", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean BETTER_INVISIBILITY_LAYERS = new ConfigBoolean(
		"betterInvisibilityLayers", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean BETTER_INVISIBILITY_LLAMA_CARPET = new ConfigBoolean(
		"betterInvisibilityLlamaCarpet", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// 地狱门特效子项。
	public static final ConfigBoolean DISABLE_NETHER_PORTAL_OVERLAY = new ConfigBoolean(
		"disableNetherPortalOverlay", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean DISABLE_NETHER_PORTAL_CAMERA = new ConfigBoolean(
		"disableNetherPortalCamera", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean DISABLE_NETHER_PORTAL_SOUND = new ConfigBoolean(
		"disableNetherPortalSound", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// 掉落物子项。
	public static final ConfigBoolean ITEM_DESPAWN_PLAYER_DROPPED_ONLY = new ConfigBoolean(
		"itemDespawnPlayerDroppedOnly", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigInteger ITEM_DESPAWN_TIME_XP_ORBS = new ConfigInteger(
		"itemDespawnTimeXpOrbs", TweakifyModConfig.DEFAULT_ITEM_DESPAWN_TICKS,
		TweakifyModConfig.MIN_ITEM_DESPAWN_TICKS, TweakifyModConfig.MAX_ITEM_DESPAWN_TICKS).apply(ConfigOptionKeys.GENERAL_KEY);

	// 夜视淡出时长。
	public static final ConfigInteger NIGHT_VISION_FADE_OUT_DURATION = new ConfigInteger(
		"nightVisionFadeOutDuration", TweakifyModConfig.DEFAULT_NIGHT_VISION_FADE_TICKS,
		TweakifyModConfig.MIN_NIGHT_VISION_FADE_TICKS, TweakifyModConfig.MAX_NIGHT_VISION_FADE_TICKS).apply(ConfigOptionKeys.GENERAL_KEY);

	// 屏幕覆盖层总开关与子项。
	public static final ConfigBooleanHotkeyed DISABLE_OVERLAY_EFFECTS = new ConfigBooleanHotkeyed(
		"disableOverlayEffects", false, "", ConfigOptionKeys.TOGGLE_HOTKEY_SETTINGS).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean DISABLE_POWDER_SNOW_OVERLAY = new ConfigBoolean(
		"disablePowderSnowOverlay", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean DISABLE_LAVA_OVERLAY = new ConfigBoolean(
		"disableLavaOverlay", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean DISABLE_WATER_OVERLAY = new ConfigBoolean(
		"disableWaterOverlay", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean DISABLE_WATER_FOG = new ConfigBoolean(
		"disableWaterFog", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean DISABLE_LAVA_FOG = new ConfigBoolean(
		"disableLavaFog", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean DISABLE_POWDER_SNOW_FOG = new ConfigBoolean(
		"disablePowderSnowFog", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// 移动与挖掘。
	public static final ConfigBoolean DISABLE_SPRINT_FOV = new ConfigBoolean(
		"disableSprintFov", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 保留原版疾跑视野强度的百分比（0 = 完全禁用）。
	public static final ConfigInteger SPRINT_FOV_INTENSITY = new ConfigInteger(
		"sprintFovIntensity", TweakifyModConfig.DEFAULT_SPRINT_FOV_INTENSITY,
		TweakifyModConfig.MIN_SPRINT_FOV_INTENSITY,
		TweakifyModConfig.MAX_SPRINT_FOV_INTENSITY).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean SPRINT_FOV_ONLY_FIRST_PERSON = new ConfigBoolean(
		"sprintFovOnlyFirstPerson", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean KEEP_MINING_PROGRESS_ON_ITEM_SWITCH = new ConfigBoolean(
		"keepMiningProgressOnItemSwitch", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 保留挖掘进度的最大距离（格，0 = 不限制）。
	public static final ConfigInteger KEEP_MINING_PROGRESS_MAX_DISTANCE = new ConfigInteger(
		"keepMiningProgressMaxDistance", TweakifyModConfig.DEFAULT_KEEP_MINING_PROGRESS_DISTANCE,
		TweakifyModConfig.MIN_KEEP_MINING_PROGRESS_DISTANCE,
		TweakifyModConfig.MAX_KEEP_MINING_PROGRESS_DISTANCE).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean KEEP_MINING_PROGRESS_SAME_TOOL_ONLY = new ConfigBoolean(
		"keepMiningProgressSameToolOnly", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 不使用最后一根火把：只剩一根时不允许放置。
	public static final ConfigBoolean KEEP_LAST_TORCH = new ConfigBoolean(
		"keepLastTorch", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 剩余不超过该数量时阻止放置。
	public static final ConfigInteger KEEP_LAST_TORCH_MIN_COUNT = new ConfigInteger(
		"keepLastTorchMinCount", TweakifyModConfig.DEFAULT_KEEP_LAST_TORCH_COUNT,
		TweakifyModConfig.MIN_KEEP_LAST_TORCH_COUNT,
		TweakifyModConfig.MAX_KEEP_LAST_TORCH_COUNT).apply(ConfigOptionKeys.GENERAL_KEY);

	// 主手最后一根火把的判定忽略副手。
	public static final ConfigBoolean KEEP_LAST_TORCH_IGNORE_OFFHAND = new ConfigBoolean(
		"keepLastTorchIgnoreOffhand", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 经过可攀爬方块不减速：蹭到梯子/藤蔓边缘时不再夹紧水平速度。
	public static final ConfigBoolean CLIMBABLE_PASS_NO_SLOWDOWN = new ConfigBoolean(
		"climbablePassNoSlowdown", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 只在潜行时生效。
	public static final ConfigBoolean CLIMBABLE_PASS_NO_SLOWDOWN_SNEAK_ONLY = new ConfigBoolean(
		"climbablePassNoSlowdownSneakOnly", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 生效的可攀爬方块类型。
	public static final ConfigBoolean CLIMBABLE_PASS_LADDERS = new ConfigBoolean(
		"climbablePassLadders", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean CLIMBABLE_PASS_VINES = new ConfigBoolean(
		"climbablePassVines", true).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean CLIMBABLE_PASS_TRAPDOORS = new ConfigBoolean(
		"climbablePassTrapdoors", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// 可见交易显示：在村民交易界面显示未来等级（锁定）的交易。
	public static final ConfigBoolean VISIBLE_TRADERS = new ConfigBoolean(
		"visibleTraders", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 只显示未解锁（锁定）的交易。
	public static final ConfigBoolean VISIBLE_TRADERS_ONLY_LOCKED = new ConfigBoolean(
		"visibleTradersOnlyLocked", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 最多显示的未来等级数。
	public static final ConfigInteger VISIBLE_TRADERS_FUTURE_LEVELS = new ConfigInteger(
		"visibleTradersFutureLevels", TweakifyModConfig.DEFAULT_VISIBLE_TRADER_FUTURE_LEVELS,
		TweakifyModConfig.MIN_VISIBLE_TRADER_FUTURE_LEVELS,
		TweakifyModConfig.MAX_VISIBLE_TRADER_FUTURE_LEVELS).apply(ConfigOptionKeys.GENERAL_KEY);

	// 单次最多下发的锁定交易条数（0 = 不限制）。
	public static final ConfigInteger VISIBLE_TRADERS_MAX_LOCKED_OFFERS = new ConfigInteger(
		"visibleTradersMaxLockedOffers", TweakifyModConfig.DEFAULT_VISIBLE_TRADER_LOCKED_OFFERS,
		TweakifyModConfig.MIN_VISIBLE_TRADER_LOCKED_OFFERS,
		TweakifyModConfig.MAX_VISIBLE_TRADER_LOCKED_OFFERS).apply(ConfigOptionKeys.GENERAL_KEY);

	// 强制关闭加载屏幕：加载世界时立即关闭加载界面。
	public static final ConfigBoolean FORCE_CLOSE_LOADING_SCREEN = new ConfigBoolean(
		"forceCloseLoadingScreen", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean FORCE_CLOSE_LOADING_SCREEN_INSTANT = new ConfigBoolean(
		"forceCloseLoadingScreenInstant", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean FORCE_CLOSE_LOADING_SCREEN_KEEP_FIRST = new ConfigBoolean(
		"forceCloseLoadingScreenKeepFirst", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean FORCE_CLOSE_LOADING_SCREEN_SKIP_RESOURCE_PACK_FADE = new ConfigBoolean(
		"forceCloseLoadingScreenSkipResourcePackFade", true).apply(ConfigOptionKeys.GENERAL_KEY);

	// 关闭资源包兼容性提示：不显示不兼容标记，也不弹确认框。
	public static final ConfigBoolean DISABLE_RESOURCE_PACK_COMPATIBILITY_CHECK = new ConfigBoolean(
		"disableResourcePackCompatibilityCheck", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 同时忽略数据包的兼容性提示。
	public static final ConfigBoolean RESOURCE_PACK_COMPATIBILITY_ALSO_DATA_PACKS = new ConfigBoolean(
		"resourcePackCompatibilityAlsoDataPacks", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean RESOURCE_PACK_COMPATIBILITY_WARN_ONLY = new ConfigBoolean(
		"resourcePackCompatibilityWarnOnly", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean DISABLE_FARMLAND_TRAMPLING = new ConfigBoolean(
		"disableFarmlandTrampling", false).apply(ConfigOptionKeys.GENERAL_KEY);

	public static final ConfigBoolean FARMLAND_TRAMPLING_PLAYER_ONLY = new ConfigBoolean(
		"farmlandTramplingPlayerOnly", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 盔甲架手臂：所有盔甲架默认显示手臂。
	public static final ConfigBoolean ARMOR_STAND_ARMS = new ConfigBoolean(
		"armorStandArms", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 只给手中拿着物品的盔甲架显示手臂。
	public static final ConfigBoolean ARMOR_STAND_ARMS_ONLY_WITH_ITEMS = new ConfigBoolean(
		"armorStandArmsOnlyWithItems", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 只对小型盔甲架显示手臂。
	public static final ConfigBoolean ARMOR_STAND_ARMS_ONLY_SMALL = new ConfigBoolean(
		"armorStandArmsOnlySmall", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 隐藏物品展示框：装有物品时只渲染物品，不渲染外框。
	public static final ConfigBoolean HIDE_ITEM_FRAMES = new ConfigBoolean(
		"hideItemFrames", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 只隐藏装有地图的展示框。
	public static final ConfigBoolean HIDE_ITEM_FRAMES_ONLY_MAPS = new ConfigBoolean(
		"hideItemFramesOnlyMaps", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 连空展示框也隐藏外框。
	public static final ConfigBoolean HIDE_ITEM_FRAMES_HIDE_EMPTY = new ConfigBoolean(
		"hideItemFramesHideEmpty", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 取消速度上限：关闭服务端的 moved too quickly 速度检查。
	public static final ConfigBoolean REMOVE_SPEED_LIMIT = new ConfigBoolean(
		"removeSpeedLimit", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 只对骑乘中的玩家取消速度上限。
	public static final ConfigBoolean REMOVE_SPEED_LIMIT_ONLY_VEHICLES = new ConfigBoolean(
		"removeSpeedLimitOnlyVehicles", false).apply(ConfigOptionKeys.GENERAL_KEY);

	// 只对正在飞行/滑翔的玩家取消速度上限。
	public static final ConfigBoolean REMOVE_SPEED_LIMIT_ONLY_FLYING = new ConfigBoolean(
		"removeSpeedLimitOnlyFlying", false).apply(ConfigOptionKeys.GENERAL_KEY);
}
