package com.gtzc.tweakify.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 配置迁移与旧键清洗：从原始 JSON 读取已移除/改名的旧键，写入当前配置字段，
 * 返回是否发生修正（调用方据此决定是否回写文件）。
 *
 * <p>与 {@link TweakifyModConfig} 同包：只操作配置分区的公开字段与字段表写入接口，
 * 不持有状态；新增版本迁移时在此追加方法，并在 {@code load} 的 corrected 聚合处接线。
 */
final class ConfigMigration {
	private static final Logger LOGGER = LoggerFactory.getLogger("tweakify-mod-config");

	private ConfigMigration() {
	}

	// 修正配置版本：旧文件（缺少该字段）按 0 处理并升级到当前版本。
	static boolean sanitizeConfigVersion(TweakifyModConfig config, boolean versionPresent) {
		int detected = versionPresent ? config.configVersion : 0;
		if (detected == TweakifyModConfig.CURRENT_CONFIG_VERSION) {
			return false;
		}
		LOGGER.info("Upgrading config version {} -> {}", detected, TweakifyModConfig.CURRENT_CONFIG_VERSION);
		config.configVersion = TweakifyModConfig.CURRENT_CONFIG_VERSION;
		return true;
	}

	// 版本 2：滚动列表记忆默认改为开启。版本 1 里该开关默认关闭，
	// 统一升级为开，避免旧配置文件一直停在关闭状态。
	static boolean migrateScrollListMemoryDefault(TweakifyModConfig config, int loadedVersion) {
		if (loadedVersion >= 2 || config.general.scrollListMemory) {
			return false;
		}
		LOGGER.info("Enabling scrollListMemory for configs older than version 2");
		config.general.scrollListMemory = true;
		return true;
	}

	// 版本 3：相机运动与物品分组取代旧的“运动相机横移倾斜”与“创造物品栏条目合并”。
	// 旧字段已从配置类移除，Gson 会忽略它们，因此需要从原始 JSON 里读出旧值迁移。
	static boolean migrateLegacyFeatureKeys(TweakifyModConfig config, JsonObject root) {
		boolean changed = false;
		if (!root.has("cameraMotion") && root.has("motionCameraTilt")) {
			config.functions.cameraMotion = legacyBoolean(root, "motionCameraTilt");
			changed = true;
		}
		if (!root.has("hotkeyCameraMotion") && root.has("hotkeyMotionCameraTilt")) {
			config.functions.hotkeyCameraMotion = legacyString(root, "hotkeyMotionCameraTilt");
			changed = true;
		}
		if (!root.has("itemGroups") && root.has("mergeCreativeItemEntries")) {
			config.functions.itemGroups = legacyBoolean(root, "mergeCreativeItemEntries");
			changed = true;
		}
		if (!root.has("hotkeyItemGroups") && root.has("hotkeyMergeCreativeItemEntries")) {
			config.functions.hotkeyItemGroups = legacyString(root, "hotkeyMergeCreativeItemEntries");
			changed = true;
		}
		// 优化标签已移除快捷键：旧文件里的字段没有对应配置字段，Gson 会忽略；
		// 这里标记一次修正，让下一次写盘把旧键清掉。
		if (root.has("hotkeyMiniHudFpsOptimization")) {
			LOGGER.info("Removing obsolete config hotkey: hotkeyMiniHudFpsOptimization");
			changed = true;
		}
		if (changed) {
			LOGGER.info("Migrating legacy config keys");
		}
		return changed;
	}

	// 版本 4：定位栏玩家过滤由“单名单 + 模式”改为“包含/排除”两个名单。
	// 旧键不参与反序列化：从原始 JSON 读出后按当前模式写入对应名单，返回 true 让写盘时清掉旧键。
	static boolean migrateLocatorHeadOptions(TweakifyModConfig config, JsonObject root) {
		JsonElement legacy = root.get("locatorBarHeadFilterNames");
		if (legacy == null || !legacy.isJsonArray()) {
			return false;
		}
		boolean exclude = TweakifyModConfig.LOCATOR_HEAD_FILTER_EXCLUDE.equals(
			config.functions.locatorBarHeadFilter);
		List<String> target = exclude
			? config.functions.locatorBarHeadExcludedPlayers
			: config.functions.locatorBarHeadIncludedPlayers;
		if (target.isEmpty()) {
			for (JsonElement name : legacy.getAsJsonArray()) {
				if (name.isJsonPrimitive() && name.getAsJsonPrimitive().isString()) {
					String value = name.getAsString().trim();
					if (!value.isEmpty()) {
						target.add(value);
					}
				}
			}
		}
		LOGGER.info("Migrating locator filter names into {}",
			exclude ? TweakifyModConfig.LOCATOR_HEAD_EXCLUDED_PLAYERS_FIELD
				: TweakifyModConfig.LOCATOR_HEAD_INCLUDED_PLAYERS_FIELD);
		return true;
	}

	// 版本 5：盔甲架手臂并入「更好的盔甲架」父开关。
	// 旧配置里已经打开手臂的玩家自动打开父开关，避免升级后功能静默失效；
	// 已经写了新键的配置尊重用户自己的选择。
	static boolean migrateArmorStandGroup(TweakifyModConfig config, JsonObject root, int loadedVersion) {
		if (loadedVersion >= 5 || root.has(TweakifyModConfig.BETTER_ARMOR_STANDS_FIELD)) {
			return false;
		}
		if (!legacyBoolean(root, "armorStandArms")) {
			return false;
		}
		LOGGER.info("Enabling betterArmorStands for configs older than version 5");
		config.functions.betterArmorStands = true;
		return true;
	}

	// 旧配置文件缺少物品列表键时补回默认值。
	// 这三个列表的空列表语义是“不做任何限制”，而不是“使用默认值”：
	// Gson 反序列化缺键会保留字段初始化器的空列表，对应功能就会静默失效。
	// 用 root.has() 而不是判断列表是否为空，是为了保留用户主动清空列表的意图。
	static boolean applyMissingListDefaults(TweakifyModConfig config, JsonObject root) {
		boolean changed = false;
		if (!root.has(TweakifyModConfig.RESTRICTED_OFFHAND_ITEMS_FIELD)) {
			LOGGER.info("Filling missing default list: {}", TweakifyModConfig.RESTRICTED_OFFHAND_ITEMS_FIELD);
			config.setStringList(TweakifyModConfig.RESTRICTED_OFFHAND_ITEMS_FIELD,
				TweakifyModConfig.defaultLightSourceItemIds());
			changed = true;
		}
		if (!root.has(TweakifyModConfig.MAIN_HAND_USE_COOLDOWN_ITEMS_FIELD)) {
			LOGGER.info("Filling missing default list: {}", TweakifyModConfig.MAIN_HAND_USE_COOLDOWN_ITEMS_FIELD);
			config.setStringList(TweakifyModConfig.MAIN_HAND_USE_COOLDOWN_ITEMS_FIELD,
				TweakifyModConfig.defaultInteractableItemIds());
			changed = true;
		}
		if (!root.has(TweakifyModConfig.OFFHAND_USE_COOLDOWN_ITEMS_FIELD)) {
			LOGGER.info("Filling missing default list: {}", TweakifyModConfig.OFFHAND_USE_COOLDOWN_ITEMS_FIELD);
			config.setStringList(TweakifyModConfig.OFFHAND_USE_COOLDOWN_ITEMS_FIELD,
				TweakifyModConfig.defaultInteractableItemIds());
			changed = true;
		}
		return changed;
	}

	private static boolean legacyBoolean(JsonObject root, String key) {
		JsonElement value = root.get(key);
		return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()
			&& value.getAsBoolean();
	}

	private static String legacyString(JsonObject root, String key) {
		JsonElement value = root.get(key);
		return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
			? value.getAsString() : "";
	}
}
