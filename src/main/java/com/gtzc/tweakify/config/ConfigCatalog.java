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
		// 掉落物
		entries.add(modified("itemDespawnTime"));
		entries.add(child("itemDespawnTime", "itemDespawnPlayerDroppedOnly"));
		entries.add(child("itemDespawnTime", "itemDespawnTimeXpOrbs"));
		// 复制物体 ID
		entries.add(function("copyItemId", true));
		entries.add(child("copyItemId", "copyTargetId"));
		entries.add(child("copyItemId", "copyItemIdFormat"));
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
		entries.add(modified("disableHurtShake", true));
		entries.add(modified("disableHurtTint", true));
		entries.add(modified("disableFlightDrift", true));
		entries.add(child("disableFlightDrift", "flightDriftVertical", true));
		// 维度传送
		entries.add(function("disableDimensionTeleport", true));
		entries.add(child("disableDimensionTeleport", "dimensionTeleportPlayersOnly"));
		entries.add(child("disableDimensionTeleport", "disableNetherPortalTeleport", true));
		entries.add(child("disableNetherPortalTeleport", "netherPortalTeleportBlacklist"));
		entries.add(child("disableDimensionTeleport", "disableEndPortalTeleport", true));
		entries.add(child("disableEndPortalTeleport", "endPortalTeleportBlacklist"));
		entries.add(child("disableDimensionTeleport", "disableEndGatewayTeleport", true));
		entries.add(child("disableEndGatewayTeleport", "endGatewayTeleportBlacklist"));
		// 世界交互
		entries.add(modified("disableUnderwaterMiningPenalty", true));
		entries.add(modified("disableAirMiningPenalty", true));
		// 剑穿草打生物（功能标签）
		entries.add(function("swordCutThroughPlants", true));
		entries.add(child("swordCutThroughPlants", "cutThroughItems"));
		entries.add(child("swordCutThroughPlants", "cutThroughPlants"));
		entries.add(child("swordCutThroughPlants", "cutThroughSneakBypass"));
		// 中键获得液体桶（功能标签）
		entries.add(function("pickFluidBucket", true));
		// 生存模式中键取物（功能标签，独立功能）
		entries.add(function("survivalPickBlock", true));
		entries.add(function("disableShovelSnowFullBreak", true));
		entries.add(child("disableShovelSnowFullBreak", "shovelSnowDropSnowball"));
		entries.add(child("disableShovelSnowFullBreak", "shovelSnowConsumeDurability"));
		entries.add(modified("fixBoatStuckOnDirtPath", true));
		// 强制关闭加载屏幕（修改标签）
		entries.add(modified("forceCloseLoadingScreen", true));
		entries.add(modified("disableResourcePackCompatibilityCheck", true));
		entries.add(modified("disableCurseEnchantmentRestriction", true));
		entries.add(modified("disableTooExpensive", true));
		// 实体高亮（prettyName 使用 highlight 前缀）
		entries.add(function("entityHighlight", true));
		entries.add(child("entityHighlight", "highlightPreset1", true));
		entries.add(child("highlightPreset1", "highlightPreset1Entities"));
		entries.add(child("entityHighlight", "highlightPreset2", true));
		entries.add(child("highlightPreset2", "highlightPreset2Entities"));
		entries.add(child("entityHighlight", "highlightPreset3", true));
		entries.add(child("highlightPreset3", "highlightPreset3Entities"));
		// 渲染
		entries.add(function("highlightSingleLayerBedrock", true));
		entries.add(child("highlightSingleLayerBedrock", "highlightSingleLayerBedrockMaxMarkers"));
		entries.add(function("barrierVisibility", true));
		entries.add(function("durabilityOverlay", true));
		entries.add(child("durabilityOverlay", "durabilityDisplayMode"));
		entries.add(child("durabilityOverlay", "durabilityOverlayHotbarOnly"));
		entries.add(child("durabilityOverlay", "durabilityOverlayLowThreshold"));
		entries.add(modified("elytraFireworkCooldown", true));
		entries.add(child("elytraFireworkCooldown", "elytraFireworkCooldownTicks"));
		entries.add(modified("disableFlatWorldSlimeSpawn", true));
		entries.add(child("disableFlatWorldSlimeSpawn", "flatWorldSlimeSpawnAllWorlds"));
		entries.add(modified("betterInvisibility", true));
		entries.add(child("betterInvisibility", "betterInvisibilityEffectParticles"));
		entries.add(child("betterInvisibility", "betterInvisibilityLayers"));
		entries.add(child("betterInvisibility", "betterInvisibilityLlamaCarpet"));
		entries.add(modified("disableSmithingTemplateConsumption", true));
		entries.add(function("chatTextSelection", true));
		entries.add(child("chatTextSelection", "chatSelectionColor"));
		entries.add(child("chatTextSelection", "chatSelectionAutoCopy"));
		entries.add(function("attackRangeDisplay", true));
		entries.add(child("attackRangeDisplay", "attackRangeDisplayMode"));
		entries.add(child("attackRangeDisplay", "attackRangeRenderMode"));
		entries.add(child("attackRangeDisplay", "attackRangeIncludeSelf", true));
		entries.add(child("attackRangeDisplay", "attackRangeMaxDistance"));
		entries.add(child("attackRangeDisplay", "attackRangeMaxTargets"));
		entries.add(child("attackRangeDisplay", "attackRangeEntities"));
		entries.add(modified("nightVisionFadeOut", true));
		entries.add(child("nightVisionFadeOut", "nightVisionFadeOutDuration"));
		entries.add(child("nightVisionFadeOut", "nightVisionFadeOutCurve"));
		entries.add(modified("enchantmentLevelArabic", true));
		// 世界交互与移动
		entries.add(modified("wartBlockDecomposition", true));
		entries.add(modified("elytraWaterFlight", true));
		entries.add(child("elytraWaterFlight", "elytraWaterFlightOnlyWater"));
		entries.add(modified("disableNetherPortalEffect", true));
		entries.add(child("disableNetherPortalEffect", "disableNetherPortalOverlay"));
		entries.add(child("disableNetherPortalEffect", "disableNetherPortalCamera"));
		entries.add(child("disableNetherPortalEffect", "disableNetherPortalSound"));
		entries.add(modified("disableBedTooFar", true));
		entries.add(modified("ironGolemRideMinecart", true));
		entries.add(modified("connectedBlockOutline", true));
		entries.add(modified("customBlockOutline", true));
		entries.add(child("customBlockOutline", TweakifyModConfig.BLOCK_OUTLINE_WIDTH_FIELD));
		entries.add(child("customBlockOutline", "blockOutlineAnimationSpeed"));
		entries.add(child("customBlockOutline", "blockOutlinePalette"));
		entries.add(child("customBlockOutline", "blockOutlineColorMode"));
		entries.add(modified("betterBlockPlacement", true));
		entries.add(child("betterBlockPlacement", "betterBlockPlacementMode"));
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
		entries.add(modified("keepMiningProgressOnItemSwitch", true));
		entries.add(modified("disableFarmlandTrampling", true));
		entries.add(child("disableFarmlandTrampling", "farmlandTramplingPlayerOnly"));
		// 滚动列表记忆（通用标签）
		entries.add(general("scrollListMemory", true));
		entries.add(child("scrollListMemory", "scrollListMemoryPersist"));
		entries.add(child("scrollListMemory", "malilibScrollMemory"));
		entries.add(child("scrollListMemory", "modMenuScrollMemory"));
		validate(entries);
		return entries;
	}

	private static Entry root(String name) {
		return new Entry(name, null, null, false);
	}

	private static Entry function(String name, boolean command) {
		return new Entry(name, null, Tab.FUNCTION, command);
	}

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
