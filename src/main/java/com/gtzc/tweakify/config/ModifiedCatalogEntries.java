package com.gtzc.tweakify.config;

import java.util.List;

import static com.gtzc.tweakify.config.ConfigCatalog.*;

/**
 * 「修改」标签的配置目录条目。
 *
 * <p>条目顺序即界面顺序：调用方 {@link ConfigCatalog#buildEntries()} 按原表顺序
 * 逐段调用 addPartN。新增条目请追加到对应标签的最后一段，不要跨段搬动。
 */
final class ModifiedCatalogEntries {
	private ModifiedCatalogEntries() {
	}

	// 原表中的第 1 段连续条目。
	static void addPart1(List<Entry> entries) {
		// 掉落物
		entries.add(modified("itemDespawnTime"));
		entries.add(child("itemDespawnTime", "itemDespawnPlayerDroppedOnly"));
		entries.add(child("itemDespawnTime", "itemDespawnTimeXpOrbs"));
		// 物品数量数字缩放（50% ~ 200%）
		entries.add(modified("itemCountScale"));
	}

	// 原表中的第 2 段连续条目。
	static void addPart2(List<Entry> entries) {
		// 移动与视角
		entries.add(modified("disableUseItemSlowdown", true));
		entries.add(child("disableUseItemSlowdown", "useItemSlowdownExceptions"));
		entries.add(child("disableUseItemSlowdown", "useItemSlowdownKeepPercent"));
		entries.add(modified("disableHurtShake", true));
		entries.add(child("disableHurtShake", "hurtShakeKeepDeathTilt"));
		entries.add(child("disableHurtShake", "hurtShakePlayersOnly"));
		entries.add(modified("disableHurtTint", true));
		entries.add(child("disableHurtTint", "hurtTintKeepWhiteFlash"));
		entries.add(child("disableHurtTint", "hurtTintPlayersOnly"));
	}

	// 原表中的第 3 段连续条目。
	static void addPart3(List<Entry> entries) {
		// 经过可攀爬方块不减速（修改标签）
		entries.add(modified("climbablePassNoSlowdown", true));
		entries.add(child("climbablePassNoSlowdown", "climbablePassNoSlowdownSneakOnly"));
		entries.add(child("climbablePassNoSlowdown", "climbablePassLadders"));
		entries.add(child("climbablePassNoSlowdown", "climbablePassVines"));
		entries.add(child("climbablePassNoSlowdown", "climbablePassTrapdoors"));
	}

	// 原表中的第 4 段连续条目。
	static void addPart4(List<Entry> entries) {
		// 世界交互
		entries.add(modified("disableUnderwaterMiningPenalty", true));
		entries.add(child("disableUnderwaterMiningPenalty", "underwaterMiningPenaltyKeep"));
		entries.add(child("disableUnderwaterMiningPenalty", "underwaterMiningPenaltySwimmingOnly"));
		entries.add(modified("disableAirMiningPenalty", true));
		entries.add(child("disableAirMiningPenalty", "airMiningPenaltyKeep"));
		entries.add(child("disableAirMiningPenalty", "airMiningPenaltyOnlyWhenFalling"));
	}

	// 原表中的第 5 段连续条目。
	static void addPart5(List<Entry> entries) {
		// 不使用最后一根火把（修改标签）
		entries.add(modified("keepLastTorch", true));
		entries.add(child("keepLastTorch", "keepLastTorchMinCount"));
		entries.add(child("keepLastTorch", "keepLastTorchIgnoreOffhand"));
	}

	// 原表中的第 6 段连续条目。
	static void addPart6(List<Entry> entries) {
		// 第三人称自身名字（修改标签）
		entries.add(modified("thirdPersonPlayerName", true));
		entries.add(child("thirdPersonPlayerName", "thirdPersonPlayerNameSneakOnly"));
		entries.add(child("thirdPersonPlayerName", "thirdPersonPlayerNameIgnoreHudHidden"));
		// 雪傀儡不受群系影响（修改标签）
		entries.add(modified("snowGolemBiomeImmunity", true));
		entries.add(child("snowGolemBiomeImmunity", "snowGolemBiomeImmunityOverworldOnly"));
		entries.add(child("snowGolemBiomeImmunity", "snowGolemBiomeImmunityOnlyWhenRaining"));
	}

	// 原表中的第 7 段连续条目。
	static void addPart7(List<Entry> entries) {
		// 强制关闭加载屏幕（修改标签）
		entries.add(modified("forceCloseLoadingScreen", true));
		entries.add(child("forceCloseLoadingScreen", "forceCloseLoadingScreenInstant"));
		entries.add(child("forceCloseLoadingScreen", "forceCloseLoadingScreenKeepFirst"));
		entries.add(child("forceCloseLoadingScreen", "forceCloseLoadingScreenSkipResourcePackFade"));
		entries.add(modified("disableResourcePackCompatibilityCheck", true));
		entries.add(child("disableResourcePackCompatibilityCheck", "resourcePackCompatibilityAlsoDataPacks"));
		entries.add(child("disableResourcePackCompatibilityCheck", "resourcePackCompatibilityWarnOnly"));
		entries.add(modified("disableCurseEnchantmentRestriction", true));
		entries.add(child("disableCurseEnchantmentRestriction", "curseRemovalScope"));
		entries.add(modified("disableTooExpensive", true));
		entries.add(child("disableTooExpensive", "tooExpensiveMaxLevelCost"));
		entries.add(child("disableTooExpensive", "tooExpensiveWarnOnly"));
		// 移除附魔冲突（修改标签）
		entries.add(modified("removeEnchantmentConflicts", true));
		entries.add(child("removeEnchantmentConflicts", "removeEnchantmentConflictsSameGroupOnly"));
		entries.add(child("removeEnchantmentConflicts", "removeEnchantmentConflictsRequireSneak"));
		// 附魔描述（修改标签）
		entries.add(modified("enchantmentDescriptions", true));
		entries.add(child("enchantmentDescriptions", "enchantmentDescriptionsSneakOnly"));
		entries.add(child("enchantmentDescriptions", "enchantmentDescriptionsIncludeCurses"));
		// 可见交易显示（修改标签）
		entries.add(modified("visibleTraders", true));
		entries.add(child("visibleTraders", "visibleTradersOnlyLocked"));
		entries.add(child("visibleTraders", "visibleTradersFutureLevels"));
		entries.add(child("visibleTraders", "visibleTradersMaxLockedOffers"));
	}

	// 原表中的第 8 段连续条目。
	static void addPart8(List<Entry> entries) {
		entries.add(modified("disableFlatWorldSlimeSpawn", true));
		entries.add(child("disableFlatWorldSlimeSpawn", "flatWorldSlimeSpawnAllWorlds"));
		entries.add(child("disableFlatWorldSlimeSpawn", "flatWorldSlimeSpawnBelowY"));
	}

	// 原表中的第 9 段连续条目。
	static void addPart9(List<Entry> entries) {
		entries.add(modified("betterInvisibility", true));
		entries.add(child("betterInvisibility", "betterInvisibilityEffectParticles"));
		entries.add(child("betterInvisibility", "betterInvisibilityLayers"));
		entries.add(child("betterInvisibility", "betterInvisibilityLlamaCarpet"));
		entries.add(modified("disableSmithingTemplateConsumption", true));
		entries.add(child("disableSmithingTemplateConsumption", "smithingTemplateKeepChance"));
		entries.add(child("disableSmithingTemplateConsumption", "smithingTemplateSurvivalOnly"));
	}

	// 原表中的第 10 段连续条目。
	static void addPart10(List<Entry> entries) {
		entries.add(modified("nightVisionFadeOut", true));
		entries.add(child("nightVisionFadeOut", "nightVisionFadeOutDuration"));
		entries.add(child("nightVisionFadeOut", "nightVisionFadeOutCurve"));
		entries.add(modified("enchantmentLevelArabic", true));
		entries.add(child("enchantmentLevelArabic", "enchantmentLevelArabicStyle"));
		entries.add(child("enchantmentLevelArabic", "enchantmentLevelArabicMinLevel"));
		// 世界交互与移动
		entries.add(modified("wartBlockDecomposition", true));
		entries.add(child("wartBlockDecomposition", "wartBlockDecompositionOutput"));
		entries.add(child("wartBlockDecomposition", "wartBlockDecompositionRequireSneak"));
		entries.add(modified("elytraWaterFlight", true));
		entries.add(child("elytraWaterFlight", "elytraWaterFlightOnlyWater"));
		entries.add(child("elytraWaterFlight", "elytraWaterFlightOnlySubmerged"));
		entries.add(modified("disableNetherPortalEffect", true));
		entries.add(child("disableNetherPortalEffect", "disableNetherPortalOverlay"));
		entries.add(child("disableNetherPortalEffect", "disableNetherPortalCamera"));
		entries.add(child("disableNetherPortalEffect", "disableNetherPortalSound"));
		entries.add(modified("disableBedTooFar", true));
		entries.add(child("disableBedTooFar", "bedTooFarMaxDistance"));
		entries.add(modified("ironGolemRideMinecart", true));
		entries.add(child("ironGolemRideMinecart", "ironGolemRideMinecartMaxDistance"));
		entries.add(child("ironGolemRideMinecart", "ironGolemRideMinecartEmptyHandOnly"));
		// 穿牌打开容器（修改标签）
		entries.add(modified("signOpenContainer", true));
		entries.add(child("signOpenContainer", "signOpenContainerSneakBypass"));
		entries.add(child("signOpenContainer", "signOpenContainerSkipWaxed"));
		// 不死图腾背包生效（修改标签）：图腾放在快捷栏或背包即可触发。
		entries.add(modified("totemFromInventory", true));
		entries.add(child("totemFromInventory", "totemInventoryIncludeHotbar"));
		entries.add(child("totemFromInventory", "totemInventoryHotbarFirst"));
		// 禁用蛛网减速（修改标签）
		entries.add(modified("disableCobwebSlowdown", true));
		entries.add(child("disableCobwebSlowdown", "cobwebSlowdownAllEntities"));
		entries.add(child("disableCobwebSlowdown", "cobwebSlowdownKeepJumpPenalty"));
		// 禁用液体推动（修改标签）
		entries.add(modified("disableFluidPush", true));
		entries.add(child("disableFluidPush", "disableWaterPush"));
		entries.add(child("disableFluidPush", "disableLavaPush"));
		// 禁用粘液块减速（修改标签，子项为弹跳）
		entries.add(modified("disableSlimeBlockSlowdown", true));
		entries.add(child("disableSlimeBlockSlowdown", "disableSlimeBlockBounce"));
		entries.add(child("disableSlimeBlockSlowdown", "disableSlimeBlockSlowdownPlayersOnly"));
		// 禁用蜂蜜块减速（修改标签，子项为黏着）
		entries.add(modified("disableHoneyBlockSlowdown", true));
		entries.add(child("disableHoneyBlockSlowdown", "disableHoneyBlockStick"));
		entries.add(modified("connectedBlockOutline", true));
		entries.add(child("connectedBlockOutline", "connectedBlockOutlineDoorFullHeight"));
		entries.add(child("connectedBlockOutline", "connectedBlockOutlineDoors"));
		entries.add(child("connectedBlockOutline", "connectedBlockOutlineChests"));
		entries.add(child("connectedBlockOutline", "connectedBlockOutlineBeds"));
		entries.add(child("connectedBlockOutline", "connectedBlockOutlinePlants"));
	}

	// 原表中的第 11 段连续条目。
	static void addPart11(List<Entry> entries) {
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
		entries.add(child("disableSprintFov", "sprintFovOnlyFirstPerson"));
		entries.add(modified("keepMiningProgressOnItemSwitch", true));
		entries.add(child("keepMiningProgressOnItemSwitch", "keepMiningProgressMaxDistance"));
		entries.add(child("keepMiningProgressOnItemSwitch", "keepMiningProgressSameToolOnly"));
		entries.add(modified("disableFarmlandTrampling", true));
		entries.add(child("disableFarmlandTrampling", "farmlandTramplingPlayerOnly"));
		// 盔甲架默认显示手臂。
		entries.add(modified("armorStandArms"));
		entries.add(child("armorStandArms", "armorStandArmsOnlyWithItems"));
		entries.add(child("armorStandArms", "armorStandArmsOnlySmall"));
		// 装有物品的展示框只渲染物品，不渲染外框。
		entries.add(modified("hideItemFrames"));
		entries.add(child("hideItemFrames", "hideItemFramesOnlyMaps"));
		entries.add(child("hideItemFrames", "hideItemFramesHideEmpty"));
		// 取消服务端 moved too quickly 速度上限。
		entries.add(modified("removeSpeedLimit", true));
		entries.add(child("removeSpeedLimit", "removeSpeedLimitOnlyVehicles"));
		entries.add(child("removeSpeedLimit", "removeSpeedLimitOnlyFlying"));
		// 更好的进度（修改标签，参考 PaginatedAdvancements：自适应窗口 + 分页 + 固定标签 + 记住标签）
		entries.add(modified("betterAdvancements", true));
		entries.add(child("betterAdvancements", "betterAdvancementsPagination"));
		entries.add(child("betterAdvancements", "betterAdvancementsPinTabs"));
		entries.add(child("betterAdvancements", "betterAdvancementsRememberTab"));
		entries.add(child("betterAdvancements", "betterAdvancementsDisableFade"));
		entries.add(child("betterAdvancements", "betterAdvancementsCopyId"));
		entries.add(child("betterAdvancements", "betterAdvancementsTabSpacing"));
		entries.add(child("betterAdvancements", "betterAdvancementsPinnedTabSpacing"));
	}
}
