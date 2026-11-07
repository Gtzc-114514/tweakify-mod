package com.gtzc.tweakify.config;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ArmorStandItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.EmptyMapItem;
import net.minecraft.world.item.EnderEyeItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.HangingEntityItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemFrameItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.KnowledgeBookItem;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.item.NameTagItem;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.WritableBookItem;
import net.minecraft.world.item.WrittenBookItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 默认物品列表：从注册表扫描“光源方块”与“可交互物品”，扫描结果缓存一次。
 * 注册表不可用（如单测环境）时退回硬编码兜底列表，且不写缓存，等注册表就绪后仍能重新扫描。
 */
final class DefaultItemLists {
	private static final Logger LOGGER = LoggerFactory.getLogger("tweakify-mod-default-items");

	private static List<String> cachedLightItems;
	private static List<String> cachedInteractableItems;

	private DefaultItemLists() {
	}

	// 扫描所有发光方块对应的物品，返回可变副本供调用方自由修改。
	static synchronized List<String> lightSourceItemIds() {
		if (cachedLightItems == null) {
			Set<String> ids = new HashSet<>();
			try {
				BuiltInRegistries.BLOCK.stream().forEach(block -> {
					if (block.defaultBlockState().getLightEmission() <= 0) {
						return;
					}
					Item item = block.asItem();
					if (item == null || item == Items.AIR) {
						return;
					}
					Identifier id = idOf(item);
					if (id != null) {
						ids.add(id.toString());
					}
				});
			} catch (Exception | LinkageError e) {
				// 注册表未初始化（如单测环境）时抛的是 Error 而非 Exception，需一并兜底。
				LOGGER.warn("Failed to scan light-source blocks, using fallback list", e);
			}
			if (ids.isEmpty()) {
				// 注册表不可用时不写缓存：等注册表就绪后仍能拿到真实扫描结果。
				LOGGER.warn("Light-source registry scan returned nothing, using fallback list");
				return new ArrayList<>(FALLBACK_LIGHT_ITEMS);
			}
			cachedLightItems = sortedCopy(ids);
		}
		return new ArrayList<>(cachedLightItems);
	}

	// 扫描所有右键会触发使用行为的物品，返回可变副本供调用方自由修改。
	static synchronized List<String> interactableItemIds() {
		if (cachedInteractableItems == null) {
			Set<String> ids = new HashSet<>();
			try {
				BuiltInRegistries.ITEM.stream().forEach(item -> {
					if (isInteractableItem(item)) {
						Identifier id = idOf(item);
						if (id != null) {
							ids.add(id.toString());
						}
					}
				});
			} catch (Exception | LinkageError e) {
				LOGGER.warn("Failed to scan interactable items, using fallback list", e);
			}
			if (ids.isEmpty()) {
				// 注册表不可用时不写缓存：等注册表就绪后仍能拿到真实扫描结果。
				LOGGER.warn("Interactable item registry scan returned nothing, using fallback list");
				return new ArrayList<>(FALLBACK_INTERACTABLE_ITEMS);
			}
			cachedInteractableItems = sortedCopy(ids);
		}
		return new ArrayList<>(cachedInteractableItems);
	}

	// 物品 ID；注册表未初始化时返回 null，避免 LinkageError 穿透到调用方。
	static Identifier idOf(Item item) {
		try {
			return BuiltInRegistries.ITEM.getKey(item);
		} catch (Exception | LinkageError e) {
			return null;
		}
	}

	private static List<String> sortedCopy(Set<String> ids) {
		List<String> sortedIds = new ArrayList<>(ids);
		Collections.sort(sortedIds);
		return List.copyOf(sortedIds);
	}

	// 判断物品是否“可交互”（右键会触发使用行为）：可放置方块、有使用动画，
	// 或属于常见右键可用物品类别。
	private static boolean isInteractableItem(Item item) {
		if (item == null || item == Items.AIR) {
			return false;
		}
		if (item instanceof BlockItem) {
			return true;
		}
		try {
			DataComponentMap components = item.components();
			if (components.has(DataComponents.FOOD) || components.has(DataComponents.CONSUMABLE)) {
				return true;
			}
		} catch (Exception | LinkageError ignored) {
			// 组件读取失败（如单测环境未加载数据组件），继续后续白名单判断。
		}
		try {
			if (item.getUseAnimation(new ItemStack(item)) != ItemUseAnimation.NONE) {
				return true;
			}
		} catch (Exception | LinkageError ignored) {
			// 落到白名单继续判断。
		}
		return item instanceof ProjectileItem
			|| item instanceof EnderpearlItem
			|| item instanceof BucketItem
			|| item instanceof FishingRodItem
			|| item instanceof BoatItem
			|| item instanceof SpawnEggItem
			|| item instanceof BoneMealItem
			|| item instanceof EnderEyeItem
			|| item instanceof ExperienceBottleItem
			|| item instanceof FlintAndSteelItem
			|| item instanceof FireChargeItem
			|| item instanceof LeadItem
			|| item instanceof NameTagItem
			|| item instanceof WritableBookItem
			|| item instanceof WrittenBookItem
			|| item instanceof EmptyMapItem
			|| item instanceof MapItem
			|| item instanceof KnowledgeBookItem
			|| item instanceof FireworkRocketItem
			|| item instanceof MinecartItem
			|| item instanceof ItemFrameItem
			|| item instanceof HangingEntityItem
			|| item instanceof ArmorStandItem;
	}

	// 仅在注册表扫描失败时兜底使用。
	private static final List<String> FALLBACK_LIGHT_ITEMS = List.of(
		"minecraft:torch", "minecraft:soul_torch", "minecraft:redstone_torch",
		"minecraft:lantern", "minecraft:soul_lantern", "minecraft:sea_lantern",
		"minecraft:glowstone", "minecraft:shroomlight", "minecraft:end_rod",
		"minecraft:beacon", "minecraft:conduit", "minecraft:jack_o_lantern",
		"minecraft:magma_block", "minecraft:glow_lichen", "minecraft:candle"
	);

	// 仅在注册表扫描失败时兜底使用。
	private static final List<String> FALLBACK_INTERACTABLE_ITEMS = List.of(
		"minecraft:apple", "minecraft:cooked_beef", "minecraft:bread",
		"minecraft:bow", "minecraft:crossbow", "minecraft:fishing_rod",
		"minecraft:ender_pearl", "minecraft:snowball", "minecraft:egg",
		"minecraft:water_bucket", "minecraft:lava_bucket", "minecraft:stone",
		"minecraft:spyglass", "minecraft:shield", "minecraft:trident",
		"minecraft:goat_horn", "minecraft:bone_meal", "minecraft:ender_eye",
		"minecraft:experience_bottle", "minecraft:flint_and_steel",
		"minecraft:lead", "minecraft:name_tag", "minecraft:writable_book",
		"minecraft:written_book", "minecraft:map", "minecraft:boat",
		"minecraft:minecart", "minecraft:item_frame", "minecraft:armor_stand",
		"minecraft:firework_rocket", "minecraft:splash_potion", "minecraft:potion"
	);
}
