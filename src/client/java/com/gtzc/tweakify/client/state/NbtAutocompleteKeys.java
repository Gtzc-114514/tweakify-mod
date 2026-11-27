package com.gtzc.tweakify.client.state;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * NBT 自动补全的候选数据源：
 * 组件方括号内使用注册表里的全部数据组件 ID；花括号内使用一份常用 NBT 键名表。
 *
 * <p>键名表只收录跨版本较稳定的常用键（实体、方块实体与物品堆叠），
 * 不追求覆盖所有模组自定义 NBT；未知 NBT 仍由原版补全或手动输入。
 */
public final class NbtAutocompleteKeys {
	// 常用 NBT 键名：实体、方块实体与物品堆叠三类里最常见的字段。
	public static final List<String> NBT_KEYS = List.of(
		"CustomName", "CustomNameVisible", "Silent", "NoGravity", "NoAI", "Invulnerable",
		"Glowing", "PersistenceRequired", "Health", "AbsorptionAmount", "Tags", "Rotation",
		"ArmorItems", "HandItems", "ArmorDropChances", "HandDropChances", "Passengers",
		"Attributes", "ActiveEffects", "Pose", "Invisible", "Marker", "Small", "ShowArms",
		"NoBasePlate", "id", "count", "components", "Item", "Items", "Slot", "Lock",
		"LootTable", "LootTableSeed", "Saddle", "ChestedHorse", "Tame", "Owner");

	// 数据组件 ID 缓存：注册表在客户端启动后不再变化，首次用到时构建一次。
	private static volatile List<String> componentIds;

	private NbtAutocompleteKeys() {
	}

	public static List<String> componentIds() {
		List<String> cached = componentIds;
		// 注册表条目数变化时才重建：既覆盖开机首次构建，也能感知运行期动态注册组件的模组。
		if (cached != null && cached.size() == BuiltInRegistries.DATA_COMPONENT_TYPE.size()) {
			return cached;
		}
		List<String> ids = new ArrayList<>();
		for (Identifier id : BuiltInRegistries.DATA_COMPONENT_TYPE.keySet()) {
			ids.add(id.toString());
		}
		ids.sort(String::compareTo);
		componentIds = List.copyOf(ids);
		return componentIds;
	}
}
