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
	}

	// 原表中的第 2 段连续条目。
	static void addPart2(List<Entry> entries) {
		// 移动与视角
		entries.add(modified("disableUseItemSlowdown", true));
		entries.add(child("disableUseItemSlowdown", "useItemSlowdownExceptions"));
		entries.add(child("disableUseItemSlowdown", "useItemSlowdownKeepPercent"));
		entries.add(modified("disableHurtShake", true));
		entries.add(child("disableHurtShake", "hurtShakeKeepDeathTilt"));
		entries.add(modified("disableHurtTint", true));
		entries.add(child("disableHurtTint", "hurtTintKeepWhiteFlash"));
	}

	// 原表中的第 3 段连续条目。
	static void addPart3(List<Entry> entries) {
		// 经过可攀爬方块不减速（修改标签）
		entries.add(modified("climbablePassNoSlowdown", true));
		entries.add(child("climbablePassNoSlowdown", "climbablePassNoSlowdownSneakOnly"));
	}

	// 原表中的第 4 段连续条目。
	static void addPart4(List<Entry> entries) {
		// 世界交互
		entries.add(modified("disableUnderwaterMiningPenalty", true));
		entries.add(child("disableUnderwaterMiningPenalty", "underwaterMiningPenaltyKeep"));
		entries.add(modified("disableAirMiningPenalty", true));
		entries.add(child("disableAirMiningPenalty", "airMiningPenaltyKeep"));
	}

	// 原表中的第 5 段连续条目。
	static void addPart5(List<Entry> entries) {
		// 不使用最后一根火把（修改标签）
		entries.add(modified("keepLastTorch", true));
		entries.add(child("keepLastTorch", "keepLastTorchMinCount"));
	}

	// 原表中的第 6 段连续条目。
	static void addPart6(List<Entry> entries) {
		// 第三人称自身名字（修改标签）
		entries.add(modified("thirdPersonPlayerName", true));
		entries.add(child("thirdPersonPlayerName", "thirdPersonPlayerNameSneakOnly"));
		// 雪傀儡不受群系影响（修改标签）
		entries.add(modified("snowGolemBiomeImmunity", true));
		entries.add(child("snowGolemBiomeImmunity", "snowGolemBiomeImmunityOverworldOnly"));
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
	}

	// 原表中的第 8 段连续条目。
	static void addPart8(List<Entry> entries) {
		entries.add(modified("disableFlatWorldSlimeSpawn", true));
		entries.add(child("disableFlatWorldSlimeSpawn", "flatWorldSlimeSpawnAllWorlds"));
	}

	// 原表中的第 9 段连续条目。
	static void addPart9(List<Entry> entries) {
		entries.add(modified("betterInvisibility", true));
		entries.add(child("betterInvisibility", "betterInvisibilityEffectParticles"));
		entries.add(child("betterInvisibility", "betterInvisibilityLayers"));
		entries.add(child("betterInvisibility", "betterInvisibilityLlamaCarpet"));
		entries.add(modified("disableSmithingTemplateConsumption", true));
		entries.add(child("disableSmithingTemplateConsumption", "smithingTemplateKeepChance"));
	}

	// 原表中的第 10 段连续条目。
	static void addPart10(List<Entry> entries) {
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
		// 不死图腾背包生效（修改标签）：图腾放在快捷栏或背包即可触发。
		entries.add(modified("totemFromInventory", true));
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
		entries.add(modified("keepMiningProgressOnItemSwitch", true));
		entries.add(child("keepMiningProgressOnItemSwitch", "keepMiningProgressMaxDistance"));
		entries.add(modified("disableFarmlandTrampling", true));
		entries.add(child("disableFarmlandTrampling", "farmlandTramplingPlayerOnly"));
	}
}
