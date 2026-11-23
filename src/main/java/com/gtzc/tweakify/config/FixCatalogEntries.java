package com.gtzc.tweakify.config;

import java.util.List;

import static com.gtzc.tweakify.config.ConfigCatalog.*;

/**
 * 「修复」标签的配置目录条目。
 *
 * <p>条目顺序即界面顺序：调用方 {@link ConfigCatalog#buildEntries()} 按原表顺序
 * 逐段调用 addPartN。新增条目请追加到对应标签的最后一段，不要跨段搬动。
 */
final class FixCatalogEntries {
	private FixCatalogEntries() {
	}

	// 原表中的第 1 段连续条目。
	static void addPart1(List<Entry> entries) {
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
		entries.add(child("frostWalkerJumpFix", "frostWalkerJumpFixSprintOnly"));
		// 物品栏鼠标按键（MC-577）
		entries.add(fix("fixMouseButtonInventory", true));
		entries.add(child("fixMouseButtonInventory", "mouseButtonInventoryClose"));
		entries.add(child("fixMouseButtonInventory", "mouseButtonInventoryDrop"));
		// 聊天卡顿（MC-247973）
		entries.add(fix("chatLagFix", true));
		entries.add(child("chatLagFix", "chatLagFixTimeoutSeconds"));
		entries.add(child("chatLagFix", "chatLagFixCacheSeconds"));
		// 高等级经验条（MC-79545）
		entries.add(fix("experienceBarHighLevelFix", true));
		entries.add(child("experienceBarHighLevelFix", "experienceBarHighLevelFixMinLevel"));
		// 书本界面垂直居中（MC-61489）
		entries.add(fix("bookGuiVerticalCenterFix", true));
		entries.add(child("bookGuiVerticalCenterFix", "bookGuiCenterViewScreen"));
		entries.add(child("bookGuiVerticalCenterFix", "bookGuiCenterEditScreen"));
		// 药水效果分类（MC-84661 / MC-170462）
		entries.add(fix("potionTooltipCategoryFix", true));
		entries.add(child("potionTooltipCategoryFix", "potionTooltipCategoryGlowing"));
		entries.add(child("potionTooltipCategoryFix", "potionTooltipCategoryBadOmen"));
		// 举盾受伤音效（MC-105068）
		entries.add(fix("shieldBlockHurtSoundFix", true));
		entries.add(child("shieldBlockHurtSoundFix", "shieldBlockHurtSoundFixPlayersOnly"));
		// 旁观者覆盖层（MC-217716）
		entries.add(fix("spectatorOverlayFix", true));
		entries.add(child("spectatorOverlayFix", "spectatorOverlayHidePortal"));
		entries.add(child("spectatorOverlayFix", "spectatorOverlayHideNausea"));
		entries.add(child("spectatorOverlayFix", "spectatorOverlayHideSpinning"));
		// 冷却指示器闪烁（MC-268420）
		entries.add(fix("cooldownIndicatorFix", true));
		entries.add(child("cooldownIndicatorFix", "cooldownIndicatorFixMinProgress"));
		// 第三人称鱼线（MC-4490）
		entries.add(fix("fishingLineThirdPersonFix", true));
		entries.add(child("fishingLineThirdPersonFix", "fishingLineThirdPersonFixOffset"));
		// 重生快捷栏（MC-143474）
		entries.add(fix("respawnHotbarFix", true));
		entries.add(child("respawnHotbarFix", "respawnHotbarKeepOnDimensionChange"));
		// 潜行动画重复（MC-159163）
		entries.add(fix("sneakAnimationFix", true));
		entries.add(child("sneakAnimationFix", "sneakAnimationFixOnlyOnGround"));
		entries.add(child("sneakAnimationFix", "sneakAnimationFixOnlyWhenSneaking"));
		// 骷髅瞄准（MC-121706）
		entries.add(fix("skeletonAimingFix", true));
		entries.add(child("skeletonAimingFix", "skeletonAimingOnlySkeletons"));
		entries.add(child("skeletonAimingFix", "skeletonAimingOnlyTargetAbove"));
		// 2×2 树苗生长（MC-8187）
		entries.add(fix("saplingGrowthFix", true));
		// 床边落地伤害（MC-19830）
		entries.add(fix("bedFallDamageFix", true));
	}
}
