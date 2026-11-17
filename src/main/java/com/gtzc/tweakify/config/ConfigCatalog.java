package com.gtzc.tweakify.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 配置项目录：分组结构、界面分类与命令开关的唯一登记处。
 *
 * <p>新增配置项时只需两步：在 {@link TweakifyModConfig} 加字段、在此表加一行（并在语言文件补
 * name/prettyName/comment，需要命令时再补 command.label）。配置界面的树、分类标签、
 * 命令注册与快捷键列表都由本表派生，不再各自维护一份清单。
 *
 * <p>表内顺序即界面顺序；父项必须排在子项之前。
 */
public final class ConfigCatalog {
	// 界面分类：未登记分类（含继承结果为 null）的配置项归入“通用”。
	public enum Tab {
		FUNCTION,
		MODIFIED,
		/** 修复类问题（草径卡船等）。 */
		FIX,
		/** 测试/开发辅助功能（档案编辑器等）。 */
		TESTING,
		/** 性能优化类（MiniHUD 掉帧优化等）。 */
		OPTIMIZATION,
		/** 同时归入“功能”与“通用”两个标签。 */
		FUNCTION_AND_GENERAL
	}

	/**
	 * 一个配置项：{@code parent} 为空表示根节点；{@code tab} 为 null 时继承父项；
	 * {@code command} 表示是否注册 /tweakify 命令（必须是布尔字段）。
	 */
	public record Entry(String name, String parent, Tab tab, boolean command) {
		public boolean isRoot() {
			return this.parent == null;
		}
	}

	private static final List<Entry> ENTRIES = List.copyOf(buildEntries());
	private static final Map<String, Entry> BY_NAME = indexByName();
	private static final Map<String, List<Entry>> CHILDREN = indexChildren();
	private static final List<Entry> COMMAND_ENTRIES = ENTRIES.stream()
		.filter(Entry::command)
		.toList();

	private ConfigCatalog() {
	}

	private static List<Entry> buildEntries() {
		List<Entry> entries = new ArrayList<>();
		// 界面语言
		entries.add(root("language"));
		entries.add(child("language", "followSystemLanguage"));
		entries.add(root("openConfigGui"));
		// 标签记忆（通用标签）
		entries.add(root("rememberSelectedTab"));
		entries.add(child("rememberSelectedTab", "rememberSelectedTabFallback"));
		// 掉落物
		entries.add(modified("itemDespawnTime"));
		entries.add(child("itemDespawnTime", "itemDespawnPlayerDroppedOnly"));
		entries.add(child("itemDespawnTime", "itemDespawnTimeXpOrbs"));
		// 复制物体 ID
		entries.add(function("copyItemId", true));
		entries.add(child("copyItemId", "copyTargetId"));
		entries.add(child("copyItemId", "copyItemIdFormat"));
		entries.add(child("copyItemId", "copyItemIdNamespace"));
		entries.add(child("copyItemId", "copyItemIdCloseContainer"));
		entries.add(child("copyItemId", "copyItemIdChatMessage"));
		// 交互冷却
		entries.add(function("miningCooldown", true));
		entries.add(child("miningCooldown", "miningCooldownAttacks"));
		entries.add(function("useCooldown", true));
		entries.add(child("useCooldown", "mainHandUseCooldown", true));
		entries.add(child("mainHandUseCooldown", "mainHandUseCooldownItems"));
		entries.add(child("useCooldown", "offhandUseCooldown", true));
		entries.add(child("offhandUseCooldown", "offhandUseCooldownItems"));
		entries.add(child("useCooldown", "blockOffhandFallback", true));
		entries.add(child("blockOffhandFallback", "interceptBlockInteraction", true));
		entries.add(child("blockOffhandFallback", "interceptEntityInteraction", true));
		entries.add(child("blockOffhandFallback", "restrictedOffhandItems"));
		// 移动与视角
		entries.add(modified("disableUseItemSlowdown", true));
		entries.add(child("disableUseItemSlowdown", "useItemSlowdownExceptions"));
		entries.add(child("disableUseItemSlowdown", "useItemSlowdownKeepPercent"));
		entries.add(modified("disableHurtShake", true));
		entries.add(child("disableHurtShake", "hurtShakeKeepDeathTilt"));
		entries.add(modified("disableHurtTint", true));
		entries.add(child("disableHurtTint", "hurtTintKeepWhiteFlash"));
		entries.add(function("disableFlightDrift", true));
		entries.add(child("disableFlightDrift", "flightDriftVertical", true));
		// 如履平地（功能标签，子项为跨步高度）
		entries.add(function("autoStepUp", true));
		entries.add(child("autoStepUp", "autoStepUpHeight"));
		// 更好的攀爬（功能标签，子开关）
		entries.add(function("betterClimbing", true));
		entries.add(child("betterClimbing", "climbingFastDescend"));
		entries.add(child("betterClimbing", "climbingFasterAscend"));
		entries.add(child("betterClimbing", "climbingJumpFromGround"));
		entries.add(child("betterClimbing", "climbingCancelCollision"));
		// 主动爬行（功能标签，子项为按住键）
		entries.add(function("forceCrawl", true));
		entries.add(child("forceCrawl", "forceCrawlKey"));
		// 经过可攀爬方块不减速（修改标签）
		entries.add(modified("climbablePassNoSlowdown", true));
		entries.add(child("climbablePassNoSlowdown", "climbablePassNoSlowdownSneakOnly"));
		// 维度传送
		entries.add(function("disableDimensionTeleport", true));
		entries.add(child("disableDimensionTeleport", "dimensionTeleportPlayersOnly"));
		entries.add(child("disableDimensionTeleport", "disableNetherPortalTeleport", true));
		entries.add(child("disableNetherPortalTeleport", "netherPortalTeleportBlacklist"));
		entries.add(child("disableDimensionTeleport", "disableEndPortalTeleport", true));
		entries.add(child("disableEndPortalTeleport", "endPortalTeleportBlacklist"));
		entries.add(child("disableDimensionTeleport", "disableEndGatewayTeleport", true));
		entries.add(child("disableEndGatewayTeleport", "endGatewayTeleportBlacklist"));
		// 地狱门传送延迟
		entries.add(function("netherPortalTeleportDelay", true));
		entries.add(child("netherPortalTeleportDelay", "netherPortalSurvivalInstantTeleport"));
		entries.add(child("netherPortalTeleportDelay", "netherPortalCreativeDelayedTeleport"));
		// 世界交互
		entries.add(modified("disableUnderwaterMiningPenalty", true));
		entries.add(child("disableUnderwaterMiningPenalty", "underwaterMiningPenaltyKeep"));
		entries.add(modified("disableAirMiningPenalty", true));
		entries.add(child("disableAirMiningPenalty", "airMiningPenaltyKeep"));
		// 剑穿草打生物（功能标签）
		entries.add(function("swordCutThroughPlants", true));
		entries.add(child("swordCutThroughPlants", "cutThroughItems"));
		entries.add(child("swordCutThroughPlants", "cutThroughPlants"));
		entries.add(child("swordCutThroughPlants", "cutThroughSneakBypass"));
		// 中键获得液体桶（功能标签，子项为液体范围）
		entries.add(function("pickFluidBucket", true));
		entries.add(child("pickFluidBucket", "pickFluidBucketFluids"));
		// 生存模式中键取物（功能标签，子项为液体分支）
		entries.add(function("survivalPickBlock", true));
		entries.add(child("survivalPickBlock", "survivalPickBlockIncludeFluids"));
		// 防猪灵装备切换（功能标签）
		entries.add(function("piglinGoldArmorSwap", true));
		entries.add(child("piglinGoldArmorSwap", "piglinGoldArmorSwapMode"));
		// 聊天框内容记忆（功能标签，子项为命令草稿放行）
		entries.add(function("chatInputMemory", true));
		entries.add(child("chatInputMemory", "chatInputMemoryIgnoreCommands"));
		entries.add(function("disableShovelSnowFullBreak", true));
		entries.add(child("disableShovelSnowFullBreak", "shovelSnowDropSnowball"));
		entries.add(child("disableShovelSnowFullBreak", "shovelSnowConsumeDurability"));
		// 不使用最后一根火把（修改标签）
		entries.add(modified("keepLastTorch", true));
		entries.add(child("keepLastTorch", "keepLastTorchMinCount"));
		// 便捷焚烧（功能标签，子项为潜行确认）
		entries.add(function("quickBurnItem", true));
		entries.add(child("quickBurnItem", "quickBurnItemRequireSneak"));
		// 草径卡船修复（修复标签）
		entries.add(fix("fixBoatStuckOnDirtPath", true));
		entries.add(child("fixBoatStuckOnDirtPath", "fixBoatStuckOnDirtPathPlayerOnly"));
		// 鹦鹉肩膀跳舞（修复标签）
		entries.add(fix("parrotDanceOnShoulder", true));
		entries.add(child("parrotDanceOnShoulder", "parrotDanceOnShoulderLocalPlayerOnly"));
		// 更好的粗体（修复标签）
		entries.add(fix("betterBold", true));
		entries.add(child("betterBold", "betterBoldOffsetPercent"));
		// 冰霜行者跳跃修复（修复标签）
		entries.add(fix("frostWalkerJumpFix", true));
		entries.add(child("frostWalkerJumpFix", "frostWalkerJumpFixJumpOnly"));
		// 物品栏鼠标按键（MC-577）
		entries.add(fix("fixMouseButtonInventory", true));
		// 聊天卡顿（MC-247973）
		entries.add(fix("chatLagFix", true));
		// 高等级经验条（MC-79545）
		entries.add(fix("experienceBarHighLevelFix", true));
		// 书本界面垂直居中（MC-61489）
		entries.add(fix("bookGuiVerticalCenterFix", true));
		// 药水效果分类（MC-84661 / MC-170462）
		entries.add(fix("potionTooltipCategoryFix", true));
		// 举盾受伤音效（MC-105068）
		entries.add(fix("shieldBlockHurtSoundFix", true));
		// 旁观者覆盖层（MC-217716）
		entries.add(fix("spectatorOverlayFix", true));
		// 冷却指示器闪烁（MC-268420）
		entries.add(fix("cooldownIndicatorFix", true));
		// 第三人称鱼线（MC-4490）
		entries.add(fix("fishingLineThirdPersonFix", true));
		// 重生快捷栏（MC-143474）
		entries.add(fix("respawnHotbarFix", true));
		// 潜行动画重复（MC-159163）
		entries.add(fix("sneakAnimationFix", true));
		// 骷髅瞄准（MC-121706）
		entries.add(fix("skeletonAimingFix", true));
		// MiniHUD 掉帧优化（优化标签）
		entries.add(optimization("miniHudFpsOptimization", true));
		entries.add(child("miniHudFpsOptimization", "miniHudRefreshIntervalMs"));
		// 第三人称自身名字（修改标签）
		entries.add(modified("thirdPersonPlayerName", true));
		entries.add(child("thirdPersonPlayerName", "thirdPersonPlayerNameSneakOnly"));
		// 雪傀儡不受群系影响（修改标签）
		entries.add(modified("snowGolemBiomeImmunity", true));
		entries.add(child("snowGolemBiomeImmunity", "snowGolemBiomeImmunityOverworldOnly"));
		// 相机运动（功能标签，参考 CameraOverhaul：转向/横移/俯仰/晃动/震动/平滑）
		entries.add(function("cameraMotion", true));
		entries.add(child("cameraMotion", "cameraMotionIntensity"));
		entries.add(child("cameraMotion", "cameraMotionTurningRoll"));
		entries.add(child("cameraMotion", "cameraMotionStrafingRoll"));
		entries.add(child("cameraMotion", "cameraMotionForwardPitch"));
		entries.add(child("cameraMotion", "cameraMotionVerticalPitch"));
		entries.add(child("cameraMotion", "cameraMotionIdleSway"));
		entries.add(child("cameraMotion", "cameraMotionScreenShake"));
		entries.add(child("cameraMotion", "cameraMotionMouseSmoothing"));
		entries.add(child("cameraMotion", "cameraMotionFirstPerson"));
		entries.add(child("cameraMotion", "cameraMotionThirdPerson"));
		// 物品分组（功能标签，参考 inventory-item-groups 的基岩版分组）
		entries.add(function("itemGroups", true));
		entries.add(child("itemGroups", "itemGroupsSort"));
		entries.add(child("itemGroups", "itemGroupsShowItems"));
		entries.add(child("itemGroups", "itemGroupsAddAll"));
		entries.add(child("itemGroups", "customItemGroups"));
		// 定位栏优化（功能标签，参考 locator-heads：玩家头像 + 描边 + 名字 + 过滤 + 指南针）
		entries.add(function("locatorBarHeads", true));
		entries.add(child("locatorBarHeads", "locatorBarAlwaysShowXp"));
		entries.add(child("locatorBarHeads", "locatorBarHeadScale"));
		entries.add(child("locatorBarHeads", "locatorBarHeadHatLayer"));
		entries.add(child("locatorBarHeads", "locatorBarHeadBorder"));
		entries.add(child("locatorBarHeads", "locatorBarHeadBorderThickness"));
		entries.add(child("locatorBarHeads", "locatorBarHeadBorderColor"));
		entries.add(child("locatorBarHeads", "locatorBarHeadNames"));
		entries.add(child("locatorBarHeads", "locatorBarHeadFilter"));
		entries.add(child("locatorBarHeads", "locatorBarHeadIncludedPlayers"));
		entries.add(child("locatorBarHeads", "locatorBarHeadExcludedPlayers"));
		entries.add(child("locatorBarHeads", "locatorBarHeadNearbyOnly"));
		entries.add(child("locatorBarHeads", "locatorBarCompass"));
		entries.add(child("locatorBarHeads", "locatorBarCompassColor"));
		entries.add(child("locatorBarHeads", "locatorBarCompassShadow"));
		entries.add(child("locatorBarHeads", "locatorBarCompassCoordinates"));
		// 强制关闭加载屏幕（修改标签）
		entries.add(modified("forceCloseLoadingScreen", true));
		entries.add(child("forceCloseLoadingScreen", "forceCloseLoadingScreenInstant"));
		entries.add(child("forceCloseLoadingScreen", "forceCloseLoadingScreenKeepFirst"));
		entries.add(child("forceCloseLoadingScreen", "forceCloseLoadingScreenSkipResourcePackFade"));
		entries.add(modified("disableResourcePackCompatibilityCheck", true));
		entries.add(child("disableResourcePackCompatibilityCheck", "resourcePackCompatibilityAlsoDataPacks"));
		entries.add(modified("disableCurseEnchantmentRestriction", true));
		entries.add(child("disableCurseEnchantmentRestriction", "curseRemovalScope"));
		entries.add(modified("disableTooExpensive", true));
		entries.add(child("disableTooExpensive", "tooExpensiveMaxLevelCost"));
		entries.add(child("disableTooExpensive", "tooExpensiveWarnOnly"));
		// 移除附魔冲突（修改标签）
		entries.add(modified("removeEnchantmentConflicts", true));
		entries.add(child("removeEnchantmentConflicts", "removeEnchantmentConflictsSameGroupOnly"));
		// 附魔描述（修改标签）
		entries.add(modified("enchantmentDescriptions", true));
		entries.add(child("enchantmentDescriptions", "enchantmentDescriptionsSneakOnly"));
		// 可见交易显示（修改标签）
		entries.add(modified("visibleTraders", true));
		entries.add(child("visibleTraders", "visibleTradersOnlyLocked"));
		// 实体高亮（prettyName 使用 highlight 前缀）
		entries.add(function("entityHighlight", true));
		entries.add(child("entityHighlight", "entityHighlightStyle"));
		entries.add(child("entityHighlight", "entityHighlightBlinkPeriod"));
		entries.add(child("entityHighlight", "entityHighlightColor"));
		entries.add(child("entityHighlight", "highlightPreset1", true));
		entries.add(child("highlightPreset1", "highlightPreset1Entities"));
		entries.add(child("entityHighlight", "highlightPreset2", true));
		entries.add(child("highlightPreset2", "highlightPreset2Entities"));
		entries.add(child("entityHighlight", "highlightPreset3", true));
		entries.add(child("highlightPreset3", "highlightPreset3Entities"));
		// 渲染
		entries.add(function("highlightSingleLayerBedrock", true));
		entries.add(child("highlightSingleLayerBedrock", "highlightSingleLayerBedrockMaxMarkers"));
		entries.add(child("highlightSingleLayerBedrock", "highlightSingleLayerBedrockScanRadius"));
		entries.add(child("highlightSingleLayerBedrock", "highlightSingleLayerBedrockColor"));
		entries.add(function("barrierVisibility", true));
		entries.add(child("barrierVisibility", "barrierVisibilityMaxMarkers"));
		entries.add(child("barrierVisibility", "barrierVisibilityScanRadius"));
		entries.add(child("barrierVisibility", "barrierVisibilityParticleInterval"));
		entries.add(function("durabilityOverlay", true));
		entries.add(child("durabilityOverlay", "durabilityDisplayMode"));
		entries.add(child("durabilityOverlay", "durabilityOverlayColorMode"));
		entries.add(child("durabilityOverlay", "durabilityOverlayHotbarOnly"));
		entries.add(child("durabilityOverlay", "durabilityOverlayLowThreshold"));
		entries.add(function("statusEffectDurationDisplay", true));
		entries.add(child("statusEffectDurationDisplay", "statusEffectDurationAlwaysSeconds"));
		entries.add(child("statusEffectDurationDisplay", "statusEffectDurationLowThreshold"));
		entries.add(child("statusEffectDurationDisplay", "statusEffectDurationColorMode"));
		entries.add(function("elytraFireworkCooldown", true));
		entries.add(child("elytraFireworkCooldown", "elytraFireworkCooldownTicks"));
		entries.add(modified("disableFlatWorldSlimeSpawn", true));
		entries.add(child("disableFlatWorldSlimeSpawn", "flatWorldSlimeSpawnAllWorlds"));
		// 关闭指定生物生成（功能标签）
		entries.add(function("disableSpecifiedMobSpawn", true));
		entries.add(child("disableSpecifiedMobSpawn", "disabledSpawnEntities"));
		entries.add(child("disableSpecifiedMobSpawn", "disabledSpawnAllowPlayerSpawn"));
		// 和平模式怪物（功能标签）
		entries.add(function("peacefulModeMonsters", true));
		entries.add(child("peacefulModeMonsters", "peacefulModeMonsterEntities"));
		entries.add(child("peacefulModeMonsters", "peacefulModeMonsterSpawn"));
		entries.add(child("peacefulModeMonsters", "peacefulModeMonsterNoAttack"));
		// 刷怪蛋投掷（功能标签，子项为速度与潜行放行）
		entries.add(function("throwSpawnEgg", true));
		entries.add(child("throwSpawnEgg", "throwSpawnEggVelocity"));
		entries.add(child("throwSpawnEgg", "throwSpawnEggSneakBypass"));
		entries.add(modified("betterInvisibility", true));
		entries.add(child("betterInvisibility", "betterInvisibilityEffectParticles"));
		entries.add(child("betterInvisibility", "betterInvisibilityLayers"));
		entries.add(child("betterInvisibility", "betterInvisibilityLlamaCarpet"));
		entries.add(modified("disableSmithingTemplateConsumption", true));
		entries.add(child("disableSmithingTemplateConsumption", "smithingTemplateKeepChance"));
		entries.add(function("chatTextSelection", true));
		entries.add(child("chatTextSelection", "chatSelectionColor"));
		entries.add(child("chatTextSelection", "chatSelectionOpacity"));
		entries.add(child("chatTextSelection", "chatSelectionAutoCopy"));
		entries.add(function("attackRangeDisplay", true));
		entries.add(child("attackRangeDisplay", "attackRangeDisplayMode"));
		entries.add(child("attackRangeDisplay", "attackRangeRenderMode"));
		entries.add(child("attackRangeDisplay", "attackRangePalette"));
		entries.add(child("attackRangeDisplay", "attackRangeIncludeSelf", true));
		entries.add(child("attackRangeDisplay", "attackRangeHostileOnly"));
		entries.add(child("attackRangeDisplay", "attackRangeIncludeInvisible"));
		entries.add(child("attackRangeDisplay", "attackRangeMaxDistance"));
		entries.add(child("attackRangeDisplay", "attackRangeMaxTargets"));
		entries.add(child("attackRangeDisplay", "attackRangeEntities"));
		entries.add(modified("nightVisionFadeOut", true));
		entries.add(child("nightVisionFadeOut", "nightVisionFadeOutDuration"));
		entries.add(child("nightVisionFadeOut", "nightVisionFadeOutCurve"));
		entries.add(modified("enchantmentLevelArabic", true));
		entries.add(child("enchantmentLevelArabic", "enchantmentLevelArabicStyle"));
		// 世界交互与移动
		entries.add(modified("wartBlockDecomposition", true));
		entries.add(child("wartBlockDecomposition", "wartBlockDecompositionOutput"));
		entries.add(modified("elytraWaterFlight", true));
		entries.add(child("elytraWaterFlight", "elytraWaterFlightOnlyWater"));
		entries.add(modified("disableNetherPortalEffect", true));
		entries.add(child("disableNetherPortalEffect", "disableNetherPortalOverlay"));
		entries.add(child("disableNetherPortalEffect", "disableNetherPortalCamera"));
		entries.add(child("disableNetherPortalEffect", "disableNetherPortalSound"));
		entries.add(modified("disableBedTooFar", true));
		entries.add(child("disableBedTooFar", "bedTooFarMaxDistance"));
		entries.add(modified("ironGolemRideMinecart", true));
		entries.add(child("ironGolemRideMinecart", "ironGolemRideMinecartMaxDistance"));
		// 穿牌打开容器（修改标签）
		entries.add(modified("signOpenContainer", true));
		entries.add(child("signOpenContainer", "signOpenContainerSneakBypass"));
		// 禁用蛛网减速（修改标签）
		entries.add(modified("disableCobwebSlowdown", true));
		entries.add(child("disableCobwebSlowdown", "cobwebSlowdownAllEntities"));
		// 禁用液体推动（修改标签）
		entries.add(modified("disableFluidPush", true));
		entries.add(child("disableFluidPush", "disableWaterPush"));
		entries.add(child("disableFluidPush", "disableLavaPush"));
		// 禁用粘液块减速（修改标签，子项为弹跳）
		entries.add(modified("disableSlimeBlockSlowdown", true));
		entries.add(child("disableSlimeBlockSlowdown", "disableSlimeBlockBounce"));
		// 禁用蜂蜜块减速（修改标签，子项为黏着）
		entries.add(modified("disableHoneyBlockSlowdown", true));
		entries.add(child("disableHoneyBlockSlowdown", "disableHoneyBlockStick"));
		entries.add(modified("connectedBlockOutline", true));
		entries.add(child("connectedBlockOutline", "connectedBlockOutlineDoorFullHeight"));
		entries.add(function("customBlockOutline", true));
		entries.add(child("customBlockOutline", TweakifyModConfig.BLOCK_OUTLINE_WIDTH_FIELD));
		entries.add(child("customBlockOutline", "blockOutlineAnimationSpeed"));
		entries.add(child("customBlockOutline", "blockOutlinePalette"));
		entries.add(child("customBlockOutline", "blockOutlineColorMode"));
		entries.add(function("betterBlockPlacement", true));
		entries.add(child("betterBlockPlacement", "betterBlockPlacementSneakBypass"));
		entries.add(child("betterBlockPlacement", "betterBlockPlacementMode"));
		// 更好的骨粉催熟（功能标签）
		entries.add(function("betterBoneMeal", true));
		entries.add(child("betterBoneMeal", "boneMealSugarCane"));
		entries.add(child("betterBoneMeal", "boneMealFlowers"));
		// 屏幕覆盖层与 HUD
		entries.add(modified("disableOverlayEffects", true));
		entries.add(child("disableOverlayEffects", "disablePowderSnowOverlay"));
		entries.add(child("disableOverlayEffects", "disableLavaOverlay"));
		entries.add(child("disableOverlayEffects", "disableWaterOverlay"));
		entries.add(child("disableOverlayEffects", "disableWaterFog"));
		entries.add(child("disableOverlayEffects", "disableLavaFog"));
		entries.add(child("disableOverlayEffects", "disablePowderSnowFog"));
		// 移动与挖掘
		entries.add(modified("disableSprintFov", true));
		entries.add(child("disableSprintFov", "sprintFovIntensity"));
		entries.add(modified("keepMiningProgressOnItemSwitch", true));
		entries.add(child("keepMiningProgressOnItemSwitch", "keepMiningProgressMaxDistance"));
		entries.add(modified("disableFarmlandTrampling", true));
		entries.add(child("disableFarmlandTrampling", "farmlandTramplingPlayerOnly"));
		// 滚动列表记忆（通用标签）
		entries.add(general("scrollListMemory", true));
		entries.add(child("scrollListMemory", "scrollListMemoryPersist"));
		entries.add(child("scrollListMemory", "malilibScrollMemory"));
		entries.add(child("scrollListMemory", "modMenuScrollMemory"));
		// 调试信息叠加层（测试标签）
		entries.add(testing("debugInfoOverlay", true));
		entries.add(child("debugInfoOverlay", "debugInfoShowPerformance"));
		entries.add(child("debugInfoOverlay", "debugInfoShowTarget"));
		entries.add(child("debugInfoOverlay", "debugInfoShowBiome"));
		entries.add(child("debugInfoOverlay", "debugInfoShowSpeed"));
		entries.add(child("debugInfoOverlay", "debugInfoShowEntities"));
		entries.add(child("debugInfoOverlay", "debugInfoShowConfigStats"));
		entries.add(child("debugInfoOverlay", "debugInfoShowEffects"));
		entries.add(child("debugInfoOverlay", "debugInfoShowHooks"));
		// 探针报表（测试标签，纯快捷键动作：把已激活的 Mixin 钩子打到聊天栏或剪贴板）
		entries.add(testing("debugProbeReport", false));
		entries.add(child("debugProbeReport", "debugProbeReportCopy"));
		// 已启用功能报表（测试标签，纯快捷键动作）
		entries.add(testing("debugEnabledFeaturesReport", false));
		entries.add(child("debugEnabledFeaturesReport", "debugEnabledFeaturesReportCopy"));
		validate(entries);
		return entries;
	}

	private static Entry root(String name) {
		return new Entry(name, null, null, false);
	}

	private static Entry function(String name, boolean command) {
		return new Entry(name, null, Tab.FUNCTION, command);
	}

	// 双标签项的工厂方法：当前没有配置项使用，保留以保证分类能力完整。
	@SuppressWarnings("unused")
	private static Entry functionAndGeneral(String name, boolean command) {
		return new Entry(name, null, Tab.FUNCTION_AND_GENERAL, command);
	}

	// 通用标签的根节点（可带命令）。
	private static Entry general(String name, boolean command) {
		return new Entry(name, null, null, command);
	}

	private static Entry modified(String name) {
		return new Entry(name, null, Tab.MODIFIED, false);
	}

	private static Entry modified(String name, boolean command) {
		return new Entry(name, null, Tab.MODIFIED, command);
	}

	// 修复标签的根节点（可带命令）。
	private static Entry fix(String name, boolean command) {
		return new Entry(name, null, Tab.FIX, command);
	}

	// 优化标签的根节点（可带命令）。
	private static Entry optimization(String name, boolean command) {
		return new Entry(name, null, Tab.OPTIMIZATION, command);
	}

	// 测试标签的根节点（可带命令）。
	private static Entry testing(String name, boolean command) {
		return new Entry(name, null, Tab.TESTING, command);
	}

	private static Entry child(String parent, String name) {
		return new Entry(name, parent, null, false);
	}

	private static Entry child(String parent, String name, boolean command) {
		return new Entry(name, parent, null, command);
	}

	// 名称唯一、父项存在且排在前面的校验：表写错时启动即失败，不留静默遗漏。
	private static void validate(List<Entry> entries) {
		Set<String> seen = new LinkedHashSet<>();
		for (Entry entry : entries) {
			if (!seen.add(entry.name())) {
				throw new IllegalStateException("Duplicate config entry: " + entry.name());
			}
			if (entry.parent() != null && !seen.contains(entry.parent())) {
				throw new IllegalStateException("Config entry " + entry.name()
					+ " must come after its parent " + entry.parent());
			}
		}
	}

	private static Map<String, Entry> indexByName() {
		Map<String, Entry> byName = new LinkedHashMap<>();
		for (Entry entry : ENTRIES) {
			byName.put(entry.name(), entry);
		}
		return Collections.unmodifiableMap(byName);
	}

	private static Map<String, List<Entry>> indexChildren() {
		Map<String, List<Entry>> children = new LinkedHashMap<>();
		for (Entry entry : ENTRIES) {
			if (entry.parent() != null) {
				children.computeIfAbsent(entry.parent(), key -> new ArrayList<>()).add(entry);
			}
		}
		children.replaceAll((key, value) -> List.copyOf(value));
		return Collections.unmodifiableMap(children);
	}

	// 全部配置项（界面顺序）。
	public static List<Entry> entries() {
		return ENTRIES;
	}

	// 全部配置项名（界面顺序），用于把 MaLiLib 选项排成同一顺序。
	public static List<String> optionNames() {
		return ENTRIES.stream().map(Entry::name).toList();
	}

	// 需要注册 /tweakify 命令的配置项。
	public static List<Entry> commandEntries() {
		return COMMAND_ENTRIES;
	}

	public static Entry entryOf(String name) {
		return BY_NAME.get(name);
	}

	public static List<Entry> childrenOf(String parent) {
		return CHILDREN.getOrDefault(parent, List.of());
	}

	// 界面分类：自身及祖先中第一个登记的分类，未登记时为“通用”（返回 null）。
	public static Tab tabOf(String name) {
		Entry entry = BY_NAME.get(name);
		while (entry != null) {
			if (entry.tab() != null) {
				return entry.tab();
			}
			entry = entry.parent() == null ? null : BY_NAME.get(entry.parent());
		}
		return null;
	}

	// 配置项是否属于某个分组根（含自身与全部后代），用于选择 prettyName 的翻译前缀。
	public static boolean isInsideGroup(String groupName, String name) {
		Entry entry = BY_NAME.get(name);
		while (entry != null) {
			if (entry.name().equals(groupName)) {
				return true;
			}
			entry = entry.parent() == null ? null : BY_NAME.get(entry.parent());
		}
		return false;
	}
}
