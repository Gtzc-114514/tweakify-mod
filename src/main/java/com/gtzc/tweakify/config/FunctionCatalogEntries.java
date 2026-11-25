package com.gtzc.tweakify.config;

import java.util.List;

import static com.gtzc.tweakify.config.ConfigCatalog.*;

/**
 * 「功能」标签的配置目录条目。
 *
 * <p>条目顺序即界面顺序：调用方 {@link ConfigCatalog#buildEntries()} 按原表顺序
 * 逐段调用 addPartN。新增条目请追加到对应标签的最后一段，不要跨段搬动。
 */
final class FunctionCatalogEntries {
	private FunctionCatalogEntries() {
	}

	// 原表中的第 1 段连续条目。
	static void addPart1(List<Entry> entries) {
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
		entries.add(child("miningCooldown", "miningCooldownCooldownTicks"));
		entries.add(function("useCooldown", true));
		entries.add(child("useCooldown", "mainHandUseCooldown", true));
		entries.add(child("mainHandUseCooldown", "mainHandUseCooldownItems"));
		entries.add(child("useCooldown", "offhandUseCooldown", true));
		entries.add(child("offhandUseCooldown", "offhandUseCooldownItems"));
		entries.add(child("useCooldown", "blockOffhandFallback", true));
		entries.add(child("blockOffhandFallback", "interceptBlockInteraction", true));
		entries.add(child("blockOffhandFallback", "interceptEntityInteraction", true));
		entries.add(child("blockOffhandFallback", "restrictedOffhandItems"));
	}

	// 原表中的第 2 段连续条目。
	static void addPart2(List<Entry> entries) {
		entries.add(function("disableFlightDrift", true));
		entries.add(child("disableFlightDrift", "flightDriftVertical", true));
		entries.add(child("disableFlightDrift", "flightDriftOnlyCreative"));
		// 创造飞行穿墙（功能标签）
		entries.add(function("creativeFlightNoclip", true));
		// 如履平地（功能标签，子项为跨步高度）
		entries.add(function("autoStepUp", true));
		entries.add(child("autoStepUp", "autoStepUpHeight"));
		entries.add(child("autoStepUp", "autoStepUpSneakBypass"));
		// 更好的攀爬（功能标签，子开关）
		entries.add(function("betterClimbing", true));
		entries.add(child("betterClimbing", "climbingFastDescend"));
		entries.add(child("betterClimbing", "climbingFasterAscend"));
		entries.add(child("betterClimbing", "climbingJumpFromGround"));
		entries.add(child("betterClimbing", "climbingCancelCollision"));
		// 主动爬行（功能标签，子项为按住键）
		entries.add(function("forceCrawl", true));
		entries.add(child("forceCrawl", "forceCrawlKey"));
		entries.add(child("forceCrawl", "forceCrawlOnlyOnGround"));
	}

	// 原表中的第 3 段连续条目。
	static void addPart3(List<Entry> entries) {
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
		entries.add(child("netherPortalTeleportDelay", "netherPortalTeleportDelayTicks"));
	}

	// 原表中的第 4 段连续条目。
	static void addPart4(List<Entry> entries) {
		// 剑穿草打生物（功能标签）
		entries.add(function("swordCutThroughPlants", true));
		entries.add(child("swordCutThroughPlants", "cutThroughItems"));
		entries.add(child("swordCutThroughPlants", "cutThroughPlants"));
		entries.add(child("swordCutThroughPlants", "cutThroughSneakBypass"));
		// 中键获得液体桶（功能标签，子项为液体范围）
		entries.add(function("pickFluidBucket", true));
		entries.add(child("pickFluidBucket", "pickFluidBucketFluids"));
		entries.add(child("pickFluidBucket", "pickFluidBucketSourceOnly"));
		// 生存模式中键取物（功能标签，子项为液体分支）
		entries.add(function("survivalPickBlock", true));
		entries.add(child("survivalPickBlock", "survivalPickBlockIncludeFluids"));
		entries.add(child("survivalPickBlock", "survivalPickBlockHotbarOnly"));
		// 防猪灵装备切换（功能标签）
		entries.add(function("piglinGoldArmorSwap", true));
		entries.add(child("piglinGoldArmorSwap", "piglinGoldArmorSwapMode"));
		entries.add(child("piglinGoldArmorSwap", "piglinGoldArmorSwapRange"));
		// 聊天框内容记忆（功能标签，子项为命令草稿放行）
		entries.add(function("chatInputMemory", true));
		entries.add(child("chatInputMemory", "chatInputMemoryIgnoreCommands"));
		entries.add(child("chatInputMemory", "chatInputMemoryMaxLength"));
		entries.add(function("disableShovelSnowFullBreak", true));
		entries.add(child("disableShovelSnowFullBreak", "shovelSnowDropSnowball"));
		entries.add(child("disableShovelSnowFullBreak", "shovelSnowConsumeDurability"));
	}

	// 原表中的第 5 段连续条目。
	static void addPart5(List<Entry> entries) {
		// 便捷焚烧（功能标签，子项为潜行确认）
		entries.add(function("quickBurnItem", true));
		entries.add(child("quickBurnItem", "quickBurnItemRequireSneak"));
		entries.add(child("quickBurnItem", "quickBurnItemShowHint"));
		entries.add(child("quickBurnItem", "quickBurnItemIgniters"));
	}

	// 原表中的第 6 段连续条目。
	static void addPart6(List<Entry> entries) {
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
		// 更好的定位栏（功能标签，按 locator-heads 重做：头像/只显名字 + 描边 + 名字 + 过滤 + 指南针）
		entries.add(function("locatorBarHeads", true));
		entries.add(child("locatorBarHeads", "locatorBarAlwaysShowXp"));
		entries.add(child("locatorBarHeads", "locatorBarHeadRenderHeads"));
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
	}

	// 原表中的第 7 段连续条目。
	static void addPart7(List<Entry> entries) {
		// 渲染与 HUD（世界内可视化项已迁移到显示标签）
		entries.add(function("durabilityOverlay", true));
		entries.add(child("durabilityOverlay", "durabilityDisplayMode"));
		entries.add(child("durabilityOverlay", "durabilityOverlayColorMode"));
		entries.add(child("durabilityOverlay", "durabilityOverlayHotbarOnly"));
		entries.add(child("durabilityOverlay", "durabilityOverlayLowThreshold"));
		entries.add(function("statusEffectDurationDisplay", true));
		entries.add(child("statusEffectDurationDisplay", "statusEffectDurationAlwaysSeconds"));
		entries.add(child("statusEffectDurationDisplay", "statusEffectDurationLowThreshold"));
		entries.add(child("statusEffectDurationDisplay", "statusEffectDurationColorMode"));
		entries.add(child("statusEffectDurationDisplay", "statusEffectDurationScale"));
		entries.add(function("elytraFireworkCooldown", true));
		entries.add(child("elytraFireworkCooldown", "elytraFireworkCooldownTicks"));
		entries.add(child("elytraFireworkCooldown", "elytraFireworkCooldownMaxTicks"));
	}

	// 原表中的第 8 段连续条目。
	static void addPart8(List<Entry> entries) {
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
	}

	// 原表中的第 9 段连续条目。
	static void addPart9(List<Entry> entries) {
		entries.add(function("chatTextSelection", true));
		entries.add(child("chatTextSelection", "chatSelectionColor"));
		entries.add(child("chatTextSelection", "chatSelectionOpacity"));
		entries.add(child("chatTextSelection", "chatSelectionAutoCopy"));
		// 存档游玩时长（功能标签，子项为显示格式）
		entries.add(function("savePlayTime", true));
		entries.add(child("savePlayTime", "savePlayTimeFormat"));
		entries.add(child("savePlayTime", "savePlayTimeDecimals"));
		// 聊天框消失进度条（功能标签）
		entries.add(function("chatFadeProgressBar", true));
	}

	// 原表中的第 10 段连续条目。
	static void addPart10(List<Entry> entries) {
		entries.add(function("betterBlockPlacement", true));
		entries.add(child("betterBlockPlacement", "betterBlockPlacementSneakBypass"));
		entries.add(child("betterBlockPlacement", "betterBlockPlacementMode"));
		// 更好的骨粉催熟（功能标签）
		entries.add(function("betterBoneMeal", true));
		entries.add(child("betterBoneMeal", "boneMealSugarCane"));
		entries.add(child("betterBoneMeal", "boneMealFlowers"));
		entries.add(child("betterBoneMeal", "boneMealFlowerCount"));
		entries.add(child("betterBoneMeal", "boneMealFlowerRadius"));
		// 附魔台附魔信息：悬停附魔选项显示实际附魔。
		entries.add(function("enchantingTableInfo", true));
		entries.add(child("enchantingTableInfo", "enchantingTableInfoShowCost"));
		entries.add(child("enchantingTableInfo", "enchantingTableInfoShowMaxLevel"));
		entries.add(child("enchantingTableInfo", "enchantingTableInfoRequireSneak"));
		// 手持物品信息（参考 held-item-info）：名称下方的附魔/容器/告示牌等内容与排版选项。
		entries.add(function("heldItemInfo", true));
		entries.add(child("heldItemInfo", "heldItemInfoShowName"));
		entries.add(child("heldItemInfo", "heldItemInfoShowEnchantments"));
		entries.add(child("heldItemInfo", "heldItemInfoShowPotionEffects"));
		entries.add(child("heldItemInfo", "heldItemInfoShowContainerContent"));
		entries.add(child("heldItemInfo", "heldItemInfoMergeSimilarContainerItems"));
		entries.add(child("heldItemInfo", "heldItemInfoShowFireworkAttributes"));
		entries.add(child("heldItemInfo", "heldItemInfoShowCommandBlockInfo"));
		entries.add(child("heldItemInfo", "heldItemInfoShowBeehiveContent"));
		entries.add(child("heldItemInfo", "heldItemInfoShowSpawnerEntity"));
		entries.add(child("heldItemInfo", "heldItemInfoShowCrossbowProjectiles"));
		entries.add(child("heldItemInfo", "heldItemInfoShowLore"));
		entries.add(child("heldItemInfo", "heldItemInfoShowUnbreakable"));
		entries.add(child("heldItemInfo", "heldItemInfoShowSignText"));
		entries.add(child("heldItemInfo", "heldItemInfoShowMusicDiscDescription"));
		entries.add(child("heldItemInfo", "heldItemInfoShowPaintingDescription"));
		entries.add(child("heldItemInfo", "heldItemInfoShowGoatHornInstrument"));
		entries.add(child("heldItemInfo", "heldItemInfoShowBookMeta"));
		entries.add(child("heldItemInfo", "heldItemInfoShowFilledMapId"));
		entries.add(child("heldItemInfo", "heldItemInfoShowEntityBucketContent"));
		entries.add(child("heldItemInfo", "heldItemInfoShowBlockState"));
		entries.add(child("heldItemInfo", "heldItemInfoShowHiddenLinesCount"));
		entries.add(child("heldItemInfo", "heldItemInfoMaxLines"));
		entries.add(child("heldItemInfo", "heldItemInfoMaxLineLength"));
		entries.add(child("heldItemInfo", "heldItemInfoMaxCommandLines"));
		entries.add(child("heldItemInfo", "heldItemInfoMaxLoreLines"));
		entries.add(child("heldItemInfo", "heldItemInfoRespectHideFlags"));
		entries.add(child("heldItemInfo", "heldItemInfoLineHeight"));
		entries.add(child("heldItemInfo", "heldItemInfoOffsetPerExtraLine"));
		entries.add(child("heldItemInfo", "heldItemInfoItemNameSpacing"));
		entries.add(child("heldItemInfo", "heldItemInfoVerticalOffset"));
		entries.add(child("heldItemInfo", "heldItemInfoBaseFadeDuration"));
		entries.add(child("heldItemInfo", "heldItemInfoFadeDurationPerExtraLine"));
		entries.add(child("heldItemInfo", "heldItemInfoPreventOverlap"));
		entries.add(child("heldItemInfo", "heldItemInfoBackground"));
		entries.add(child("heldItemInfo", "heldItemInfoShowOnlyFilteredEnchantments"));
		entries.add(child("heldItemInfo", "heldItemInfoFilteredEnchantments"));
		// 背包快捷右键：右键点击背包里的方块物品，直接打开对应的功能方块/潜影盒/末影箱。
		entries.add(function("inventoryQuickRightClick", true));
		entries.add(child("inventoryQuickRightClick", "quickRightClickFunctionalBlocks"));
		entries.add(child("inventoryQuickRightClick", "quickRightClickShulkerBoxes"));
		entries.add(child("inventoryQuickRightClick", "quickRightClickEnderChest"));
		entries.add(child("inventoryQuickRightClick", "quickRightClickRequireSneak"));
	}
}
