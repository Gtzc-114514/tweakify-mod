package com.gtzc.tweakify.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 创造模式物品栏的“物品分组”规则（参考 Inventory Item Groups 的基岩版分组方式）。
 *
 * <p>每个物品栏分类按物品 ID 的包含/排除规则生成默认分组（楼梯、台阶、羊毛、
 * 药水等）；命中分组的物品只显示组图标，点击图标展开剩余成员。搜索页、
 * 快捷栏与玩家背包不调用这里，保持原版行为。
 */
public final class CreativeItemGroups {
	// 排序方式：保持物品栏原顺序 / 按物品 ID 字母序。
	public static final String SORT_DEFAULT = "default";
	public static final String SORT_ALPHABETICAL = "alphabetical";
	public static final List<String> SUPPORTED_SORTS = List.of(SORT_DEFAULT, SORT_ALPHABETICAL);

	// 与参考模组一致：少于 3 个成员的组不折叠，避免把两个物品塞进一个图标。
	public static final int MIN_GROUP_SIZE = 3;

	private static final String BUILDING_BLOCKS = "minecraft:building_blocks";
	private static final String COLORED_BLOCKS = "minecraft:colored_blocks";
	private static final String NATURAL_BLOCKS = "minecraft:natural_blocks";
	private static final String FUNCTIONAL_BLOCKS = "minecraft:functional_blocks";
	private static final String REDSTONE_BLOCKS = "minecraft:redstone_blocks";
	private static final String TOOLS_AND_UTILITIES = "minecraft:tools_and_utilities";
	private static final String COMBAT = "minecraft:combat";
	private static final String FOOD_AND_DRINKS = "minecraft:food_and_drinks";
	private static final String INGREDIENTS = "minecraft:ingredients";

	private static final Map<String, List<Definition>> DEFINITIONS = buildDefinitions();

	private CreativeItemGroups() {
	}

	/**
	 * 一个默认分组规则：物品 ID 包含任一 {@code containedItems} 且不包含任何
	 * {@code excludedItems} 时命中。
	 */
	public record Definition(String key, List<String> containedItems, List<String> excludedItems) {
		public Definition {
			containedItems = List.copyOf(containedItems);
			excludedItems = List.copyOf(excludedItems);
		}

		public boolean matches(String itemId) {
			boolean contained = false;
			for (String pattern : this.containedItems) {
				if (itemId.contains(pattern)) {
					contained = true;
					break;
				}
			}
			if (!contained) {
				return false;
			}
			for (String pattern : this.excludedItems) {
				if (itemId.contains(pattern)) {
					return false;
				}
			}
			return true;
		}
	}

	public record Group(String key, List<ItemStack> items) {
		public Group {
			items = List.copyOf(items);
		}

		public ItemStack icon() {
			return this.items.getFirst();
		}

		public boolean isGrouped() {
			return this.items.size() >= MIN_GROUP_SIZE;
		}
	}

	/**
	 * 分组结果：显示列表已按展开状态折叠/展开，{@code groupByItem} 用于
	 * 判断某个槽位是否属于分组以及是否为组图标。
	 */
	public record Result(List<ItemStack> displayItems, Map<String, Group> groups,
						 Map<Item, Group> groupByItem) {
		public Result {
			displayItems = List.copyOf(displayItems);
			groups = Map.copyOf(groups);
			groupByItem = Map.copyOf(groupByItem);
		}

		public Group groupOf(ItemStack stack) {
			return stack == null || stack.isEmpty() ? null : this.groupByItem.get(stack.getItem());
		}
	}

	public static boolean isSupportedSort(String sort) {
		return sort != null && SUPPORTED_SORTS.contains(sort);
	}

	public static List<Definition> definitionsFor(String tabId) {
		return DEFINITIONS.getOrDefault(tabId, List.of());
	}

	/**
	 * 构建指定分类的显示列表。
	 *
	 * @param tabId    物品栏分类 ID（如 {@code minecraft:building_blocks}）
	 * @param source   该分类的完整物品列表
	 * @param expanded 当前展开的分组 key
	 * @param sort     分组内排序方式
	 */
	public static Result build(String tabId, Collection<ItemStack> source, Set<String> expanded,
							   String sort) {
		List<ItemStack> stacks = new ArrayList<>(source);
		List<Definition> definitions = definitionsFor(tabId);
		if (definitions.isEmpty()) {
			return new Result(stacks, Map.of(), Map.of());
		}

		Map<Item, String> idCache = new HashMap<>();
		Set<Item> consumed = new HashSet<>();
		List<Group> accepted = new ArrayList<>();
		Map<Item, Group> groupByItem = new LinkedHashMap<>();
		for (Definition definition : definitions) {
			List<ItemStack> members = new ArrayList<>();
			for (ItemStack stack : stacks) {
				if (stack == null || stack.isEmpty() || consumed.contains(stack.getItem())) {
					continue;
				}
				if (definition.matches(itemId(stack.getItem(), idCache))) {
					members.add(stack);
				}
			}
			if (members.size() < MIN_GROUP_SIZE) {
				// 未达标的组不占用物品，后续更具体的规则仍可使用它们。
				continue;
			}
			if (SORT_ALPHABETICAL.equals(sort)) {
				members.sort((left, right) -> itemId(left.getItem(), idCache)
					.compareTo(itemId(right.getItem(), idCache)));
			}
			Group group = new Group(definition.key(), members);
			accepted.add(group);
			for (ItemStack stack : members) {
				consumed.add(stack.getItem());
				groupByItem.put(stack.getItem(), group);
			}
		}

		if (accepted.isEmpty()) {
			return new Result(stacks, Map.of(), Map.of());
		}

		List<ItemStack> display = new ArrayList<>(stacks.size());
		for (ItemStack stack : stacks) {
			if (stack == null || stack.isEmpty()) {
				display.add(stack);
				continue;
			}
			Group group = groupByItem.get(stack.getItem());
			if (group == null) {
				display.add(stack);
				continue;
			}
			if (stack != group.icon()) {
				// 组图标保留在原位置，其余成员只在展开时插入到图标之后。
				continue;
			}
			display.add(stack);
			if (expanded.contains(group.key())) {
				for (ItemStack member : group.items()) {
					if (member != stack) {
						display.add(member);
					}
				}
			}
		}

		Map<String, Group> groups = new LinkedHashMap<>();
		for (Group group : accepted) {
			groups.put(group.key(), group);
		}
		return new Result(display, groups, groupByItem);
	}

	public static boolean isRepresentative(ItemStack stack, Group group) {
		return stack != null && group != null && group.isGrouped()
			&& ItemStack.isSameItemSameComponents(stack, group.icon());
	}

	// 展开分组的成员槽位需要黑色底；组图标、普通槽位与背包槽位都不画。
	public static boolean shouldDrawExpandedBackground(int slotIndex, int creativeSlotCount,
			boolean groupingActive, Group group, boolean expanded, boolean representative) {
		return slotIndex >= 0 && slotIndex < creativeSlotCount
			&& groupingActive && group != null && group.isGrouped() && expanded && !representative;
	}

	private static String itemId(Item item, Map<Item, String> cache) {
		String cached = cache.get(item);
		if (cached != null) {
			return cached;
		}
		Identifier id = BuiltInRegistries.ITEM.getKey(item);
		String value = id == null ? "minecraft:air" : id.toString();
		cache.put(item, value);
		return value;
	}

	private record DefaultGroup(String tabId, Definition definition) {
	}

	private static Map<String, List<Definition>> buildDefinitions() {
		List<DefaultGroup> all = new ArrayList<>();

		// 建筑方块
		add(all, BUILDING_BLOCKS, "logs", contains("log", "stem", "bamboo_block"), excluded("stripped"));
		add(all, BUILDING_BLOCKS, "woods", contains("wood", "hyphae"), excluded("stripped"));
		add(all, BUILDING_BLOCKS, "stripped_logs", contains("log", "stem", "bamboo_block"));
		add(all, BUILDING_BLOCKS, "stripped_woods", contains("wood", "hyphae"));
		add(all, BUILDING_BLOCKS, "stairs", contains("stair"));
		add(all, BUILDING_BLOCKS, "slabs", contains("slab"));
		add(all, BUILDING_BLOCKS, "planks", contains("planks", "mosaic"));
		add(all, BUILDING_BLOCKS, "fence_gates", contains("fence_gate"));
		add(all, BUILDING_BLOCKS, "fences", contains("fence"));
		add(all, BUILDING_BLOCKS, "trapdoors", contains("trapdoor"));
		add(all, BUILDING_BLOCKS, "doors", contains("door"));
		add(all, BUILDING_BLOCKS, "pressure_plates", contains("pressure_plate"));
		add(all, BUILDING_BLOCKS, "buttons", contains("button"));
		add(all, BUILDING_BLOCKS, "bars", contains("bar"), excluded("cinnabar"));
		add(all, BUILDING_BLOCKS, "chains", contains("chain"));
		add(all, BUILDING_BLOCKS, "copper", contains("copper"));
		add(all, BUILDING_BLOCKS, "walls", contains("wall"));
		add(all, BUILDING_BLOCKS, "decorative_stone",
			contains("bricks", "chiseled", "tiles", "polished"));
		add(all, BUILDING_BLOCKS, "sandstone", contains("sandstone"));

		// 彩色方块
		add(all, COLORED_BLOCKS, "wool_stairs", contains("wool_stairs"));
		add(all, COLORED_BLOCKS, "wool_slabs", contains("wool_slab"));
		add(all, COLORED_BLOCKS, "wool", contains("wool"));
		add(all, COLORED_BLOCKS, "carpets", contains("carpet"));
		add(all, COLORED_BLOCKS, "glazed_terracotta", contains("glazed_terracotta"));
		add(all, COLORED_BLOCKS, "terracotta", contains("terracotta"));
		add(all, COLORED_BLOCKS, "concrete_powder", contains("concrete_powder"));
		add(all, COLORED_BLOCKS, "concrete_stairs", contains("concrete_stairs"));
		add(all, COLORED_BLOCKS, "concrete_slabs", contains("concrete_slab"));
		add(all, COLORED_BLOCKS, "concrete", contains("concrete"));
		add(all, COLORED_BLOCKS, "glass_panes", contains("glass_pane"));
		add(all, COLORED_BLOCKS, "glass", contains("glass"));
		add(all, COLORED_BLOCKS, "shulker_boxes", contains("shulker_box"));
		add(all, COLORED_BLOCKS, "candles", contains("candle"));
		add(all, COLORED_BLOCKS, "banners", contains("banner"));
		add(all, COLORED_BLOCKS, "beds", contains("bed"));
		add(all, COLORED_BLOCKS, "cushions", contains("_cushion"));

		// 自然方块
		add(all, NATURAL_BLOCKS, "ores", contains("_ore", "debris", "raw_"));
		add(all, NATURAL_BLOCKS, "mushrooms", contains("mushroom", "fungus"));
		add(all, NATURAL_BLOCKS, "saplings", contains("sapling", "propagule"));
		add(all, NATURAL_BLOCKS, "ground_cover",
			contains("fern", "_grass", "bush", "_sprouts", "hanging_moss", "_vines", "shrub"),
			excluded("_bush"));
		add(all, NATURAL_BLOCKS, "seeds", contains("seeds", "_pod"));
		add(all, NATURAL_BLOCKS, "flowers", contains(
			"dandelion", "poppy", "orchid", "allium", "tulip", "daisy", "cornflower",
			"torchflower", "azure_bluet", "valley", "cactus_flower", "eyeblossom", "rose",
			"petals", "wildflower", "crimson_roots", "warped_roots", "sunflower", "peony",
			"lilac", "pitcher_plant"));
		add(all, NATURAL_BLOCKS, "leaves", contains("leaves"));
		add(all, NATURAL_BLOCKS, "coral_blocks", contains("coral_block"));
		add(all, NATURAL_BLOCKS, "coral_decorations", contains("coral"));
		add(all, NATURAL_BLOCKS, "stone", contains(
			":stone", "diorite", "andesite", "granite", "tuff", "basalt", "blackstone",
			"deepslate"));
		add(all, NATURAL_BLOCKS, "logs", contains("log", "stem"));

		// 功能方块
		add(all, FUNCTIONAL_BLOCKS, "lanterns", contains("lantern"), excluded("sea"));
		add(all, FUNCTIONAL_BLOCKS, "chains", contains("chain"));
		add(all, FUNCTIONAL_BLOCKS, "bulbs", contains("bulb"));
		add(all, FUNCTIONAL_BLOCKS, "anvils", contains("anvil"));
		add(all, FUNCTIONAL_BLOCKS, "lightning_rods", contains("lightning_rod"));
		add(all, FUNCTIONAL_BLOCKS, "shelves", contains("_shelf"));
		add(all, FUNCTIONAL_BLOCKS, "hanging_signs", contains("hanging_sign"));
		add(all, FUNCTIONAL_BLOCKS, "signs", contains("sign"));
		add(all, FUNCTIONAL_BLOCKS, "chests", contains("chest"));
		add(all, FUNCTIONAL_BLOCKS, "shulker_boxes", contains("shulker_box"));
		add(all, FUNCTIONAL_BLOCKS, "beds", contains("_bed"));
		add(all, FUNCTIONAL_BLOCKS, "candles", contains("candle"));
		add(all, FUNCTIONAL_BLOCKS, "banners", contains("banner"));
		add(all, FUNCTIONAL_BLOCKS, "skulls", contains("head", "skull"));
		add(all, FUNCTIONAL_BLOCKS, "golem_statues", contains("golem_statue"));
		add(all, FUNCTIONAL_BLOCKS, "infested_stone", contains("infested"));
		add(all, FUNCTIONAL_BLOCKS, "paintings", contains("painting"));

		// 红石方块
		add(all, REDSTONE_BLOCKS, "bulbs", contains("bulb"));
		add(all, REDSTONE_BLOCKS, "pressure_plates", contains("pressure_plate"));
		add(all, REDSTONE_BLOCKS, "transport", contains("minecart", "boat", "_raft"));
		add(all, REDSTONE_BLOCKS, "chests", contains("chest"));
		add(all, REDSTONE_BLOCKS, "rails", contains("rail"));

		// 工具与实用物品
		add(all, TOOLS_AND_UTILITIES, "shovels", contains("shovel"));
		add(all, TOOLS_AND_UTILITIES, "pickaxes", contains("pickaxe"));
		add(all, TOOLS_AND_UTILITIES, "axes", contains("axe"));
		add(all, TOOLS_AND_UTILITIES, "hoes", contains("hoe"));
		add(all, TOOLS_AND_UTILITIES, "bundles", contains("bundle"));
		add(all, TOOLS_AND_UTILITIES, "firework_rockets", contains("firework_rocket"));
		add(all, TOOLS_AND_UTILITIES, "harnesses", contains("harness"));
		add(all, TOOLS_AND_UTILITIES, "chest_boats", contains("chest_boat", "chest_raft"));
		add(all, TOOLS_AND_UTILITIES, "boats", contains("boat", "_raft"));
		add(all, TOOLS_AND_UTILITIES, "rails", contains("rail"));
		add(all, TOOLS_AND_UTILITIES, "minecarts", contains("minecart"));
		add(all, TOOLS_AND_UTILITIES, "discs", contains("disc"));
		add(all, TOOLS_AND_UTILITIES, "goat_horns", contains("goat_horn"));
		add(all, TOOLS_AND_UTILITIES, "creature_buckets", contains(
			"cod_bucket", "salmon_bucket", "tropical_fish_bucket", "pufferfish_bucket",
			"axolotl_bucket", "tadpole_bucket", "sulfur_cube_bucket"));

		// 战斗
		add(all, COMBAT, "swords", contains("sword"));
		add(all, COMBAT, "spears", contains("spear"));
		add(all, COMBAT, "axes", contains("axe"));
		add(all, COMBAT, "helmets", contains("helmet"));
		add(all, COMBAT, "chestplates", contains("chestplate"));
		add(all, COMBAT, "leggings", contains("leggings"));
		add(all, COMBAT, "boots", contains("boots"));
		add(all, COMBAT, "horse_armor", contains("horse_armor"));
		add(all, COMBAT, "nautilus_armor", contains("nautilus_armor"));
		add(all, COMBAT, "eggs", contains("egg"));
		add(all, COMBAT, "tipped_arrows", contains("tipped_arrow"));
		add(all, COMBAT, "firework_rockets", contains("firework_rocket"));

		// 食物与饮品
		add(all, FOOD_AND_DRINKS, "suspicious_stews", contains("suspicious_stew"));
		add(all, FOOD_AND_DRINKS, "ominous_bottles", contains("ominous_bottle"));
		add(all, FOOD_AND_DRINKS, "splash_potions", contains("splash_potion"));
		add(all, FOOD_AND_DRINKS, "lingering_potions", contains("lingering_potion"));
		add(all, FOOD_AND_DRINKS, "potions", contains("potion"));
		add(all, FOOD_AND_DRINKS, "cooked_food", contains("cooked"));
		add(all, FOOD_AND_DRINKS, "raw_food", contains(
			"beef", "porkchop", "mutton", "chicken", "rabbit", ":cod", "salmon"),
			excluded("rabbit_"));

		// 原材料
		add(all, INGREDIENTS, "dyes", contains("dye"));
		add(all, INGREDIENTS, "banner_patterns", contains("banner_pattern"));
		add(all, INGREDIENTS, "pottery_sherds", contains("pottery_sherd"));
		add(all, INGREDIENTS, "smithing_templates", contains("smithing_template"));
		add(all, INGREDIENTS, "enchanted_books", contains("enchanted_book"));

		Map<String, List<Definition>> result = new LinkedHashMap<>();
		for (DefaultGroup entry : all) {
			result.computeIfAbsent(entry.tabId(), ignored -> new ArrayList<>())
				.add(entry.definition());
		}
		Map<String, List<Definition>> frozen = new LinkedHashMap<>();
		for (Map.Entry<String, List<Definition>> entry : result.entrySet()) {
			frozen.put(entry.getKey(), List.copyOf(entry.getValue()));
		}
		return Map.copyOf(frozen);
	}

	private static void add(List<DefaultGroup> definitions, String tabId, String key,
							List<String> containedItems) {
		add(definitions, tabId, key, containedItems, List.of());
	}

	private static void add(List<DefaultGroup> definitions, String tabId, String key,
							List<String> containedItems, List<String> excludedItems) {
		definitions.add(new DefaultGroup(tabId,
			new Definition(key, containedItems, excludedItems)));
	}

	private static List<String> contains(String... patterns) {
		return List.of(patterns);
	}

	private static List<String> excluded(String... patterns) {
		return List.of(patterns);
	}
}
