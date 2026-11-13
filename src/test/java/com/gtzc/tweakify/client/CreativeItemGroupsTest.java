package com.gtzc.tweakify.client;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreativeItemGroupsTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		BuiltInRegistries.ITEM.freeze();
	}

	@Test
	void vanillaCategoryTabsHaveDefaultGroups() {
		for (String tabId : List.of(
			"minecraft:building_blocks", "minecraft:colored_blocks", "minecraft:natural_blocks",
			"minecraft:functional_blocks", "minecraft:redstone_blocks",
			"minecraft:tools_and_utilities", "minecraft:combat",
			"minecraft:food_and_drinks", "minecraft:ingredients")) {
			assertFalse(CreativeItemGroups.definitionsFor(tabId).isEmpty(),
				"missing default groups for " + tabId);
		}
		// 搜索页、快捷栏与背包页不参与分组。
		for (String tabId : List.of("minecraft:search", "minecraft:hotbar", "minecraft:inventory")) {
			assertTrue(CreativeItemGroups.definitionsFor(tabId).isEmpty(),
				"non-category tab must not be grouped: " + tabId);
		}
	}

	@Test
	void collapsedGroupKeepsIconAndExpandedRestoresMembers() {
		ItemStack oak = stack(Items.OAK_STAIRS);
		ItemStack stone = stack(Items.STONE_STAIRS);
		ItemStack spruce = stack(Items.SPRUCE_STAIRS);
		ItemStack dirt = stack(Items.DIRT);
		List<ItemStack> source = List.of(oak, stone, spruce, dirt);

		CreativeItemGroups.Result collapsed = CreativeItemGroups.build(
			"minecraft:building_blocks", source, Set.of(), CreativeItemGroups.SORT_DEFAULT);
		assertEquals(List.of(oak, dirt), collapsed.displayItems());
		assertEquals(1, collapsed.groups().size());
		CreativeItemGroups.Group group = collapsed.groups().get("stairs");
		assertEquals(3, group.items().size());
		assertTrue(CreativeItemGroups.isRepresentative(oak, group));
		assertFalse(CreativeItemGroups.isRepresentative(stone, group));

		CreativeItemGroups.Result expanded = CreativeItemGroups.build(
			"minecraft:building_blocks", source, Set.of("stairs"), CreativeItemGroups.SORT_DEFAULT);
		assertEquals(List.of(oak, stone, spruce, dirt), expanded.displayItems());
	}

	@Test
	void groupsWithLessThanThreeMembersStayUngrouped() {
		List<ItemStack> source = List.of(
			stack(Items.OAK_STAIRS), stack(Items.STONE_STAIRS), stack(Items.DIRT));
		CreativeItemGroups.Result result = CreativeItemGroups.build(
			"minecraft:building_blocks", source, Set.of(), CreativeItemGroups.SORT_DEFAULT);
		assertTrue(result.groups().isEmpty());
		assertEquals(source, result.displayItems());
	}

	@Test
	void excludedItemsStayOutsideTheGroup() {
		ItemStack oakLog = stack(Items.OAK_LOG);
		ItemStack birchLog = stack(Items.BIRCH_LOG);
		ItemStack spruceLog = stack(Items.SPRUCE_LOG);
		ItemStack stripped = stack(Items.STRIPPED_OAK_LOG);
		List<ItemStack> source = List.of(oakLog, birchLog, spruceLog, stripped);

		CreativeItemGroups.Result collapsed = CreativeItemGroups.build(
			"minecraft:building_blocks", source, Set.of(), CreativeItemGroups.SORT_DEFAULT);
		assertEquals(List.of(oakLog, stripped), collapsed.displayItems());
		assertEquals(3, collapsed.groups().get("logs").items().size());
		assertFalse(collapsed.groupByItem().containsKey(stripped.getItem()));
	}

	@Test
	void alphabeticalSortOrdersMembersByItemId() {
		ItemStack birch = stack(Items.BIRCH_STAIRS);
		ItemStack oak = stack(Items.OAK_STAIRS);
		ItemStack stone = stack(Items.STONE_STAIRS);
		List<ItemStack> source = List.of(stone, oak, birch);

		CreativeItemGroups.Result result = CreativeItemGroups.build(
			"minecraft:building_blocks", source, Set.of(), CreativeItemGroups.SORT_ALPHABETICAL);
		List<Item> order = new ArrayList<>();
		for (ItemStack stack : result.groups().get("stairs").items()) {
			order.add(stack.getItem());
		}
		List<Item> expected = new ArrayList<>(List.of(birch.getItem(), oak.getItem(), stone.getItem()));
		expected.sort((left, right) -> BuiltInRegistries.ITEM.getKey(left).toString()
			.compareTo(BuiltInRegistries.ITEM.getKey(right).toString()));
		assertEquals(expected, order);
	}

	@Test
	void categoriesWithoutDefinitionsAreUnchanged() {
		List<ItemStack> source = List.of(stack(Items.DIRT), stack(Items.STONE));
		CreativeItemGroups.Result result = CreativeItemGroups.build(
			"minecraft:search", source, Set.of(), CreativeItemGroups.SORT_DEFAULT);
		assertEquals(source, result.displayItems());
		assertTrue(result.groups().isEmpty());
		assertTrue(result.groupByItem().isEmpty());
	}

	@Test
	void groupKeysAreUniqueWithinEachTab() {
		for (String tabId : List.of(
			"minecraft:building_blocks", "minecraft:colored_blocks", "minecraft:natural_blocks",
			"minecraft:functional_blocks", "minecraft:redstone_blocks",
			"minecraft:tools_and_utilities", "minecraft:combat",
			"minecraft:food_and_drinks", "minecraft:ingredients")) {
			Set<String> keys = new HashSet<>();
			for (CreativeItemGroups.Definition definition : CreativeItemGroups.definitionsFor(tabId)) {
				assertTrue(keys.add(definition.key()),
					"duplicate group key " + definition.key() + " in " + tabId);
				assertFalse(definition.containedItems().isEmpty(),
					"group " + definition.key() + " must have at least one pattern");
			}
		}
	}

	@Test
	void expandedBackgroundOnlyCoversExpandedMembers() {
		CreativeItemGroups.Group group = new CreativeItemGroups.Group("stairs", List.of(
			stack(Items.OAK_STAIRS), stack(Items.STONE_STAIRS), stack(Items.SPRUCE_STAIRS)));

		assertTrue(CreativeItemGroups.shouldDrawExpandedBackground(
			0, 45, true, group, true, false), "expanded member should get a background");
		assertFalse(CreativeItemGroups.shouldDrawExpandedBackground(
			0, 45, true, group, true, true), "group icon must keep its normal background");
		assertFalse(CreativeItemGroups.shouldDrawExpandedBackground(
			0, 45, true, group, false, false), "collapsed group member is not visible");
		assertFalse(CreativeItemGroups.shouldDrawExpandedBackground(
			45, 45, true, group, true, false), "player inventory slot must not be painted");
		assertFalse(CreativeItemGroups.shouldDrawExpandedBackground(
			-1, 45, true, group, true, false), "negative slot index must not be painted");
		assertFalse(CreativeItemGroups.shouldDrawExpandedBackground(
			0, 45, false, group, true, false), "disabled grouping must not paint");
		assertFalse(CreativeItemGroups.shouldDrawExpandedBackground(
			0, 45, true, null, true, false), "ungrouped slot must not paint");
		CreativeItemGroups.Group small = new CreativeItemGroups.Group("small", List.of(
			stack(Items.OAK_STAIRS), stack(Items.STONE_STAIRS)));
		assertFalse(CreativeItemGroups.shouldDrawExpandedBackground(
			0, 45, true, small, true, false), "group below the fold threshold must not paint");
	}

	@Test
	void supportedSortsAreStable() {
		assertTrue(CreativeItemGroups.isSupportedSort(CreativeItemGroups.SORT_DEFAULT));
		assertTrue(CreativeItemGroups.isSupportedSort(CreativeItemGroups.SORT_ALPHABETICAL));
		assertFalse(CreativeItemGroups.isSupportedSort("unknown"));
		assertFalse(CreativeItemGroups.isSupportedSort(null));
	}

	private static ItemStack stack(Item item) {
		// 单元测试没有完整数据包加载流程，手动绑定空组件表，避免 Holder 的组件尚未绑定。
		// Item.builtInRegistryHolder() 已弃用，改从注册表取 holder。
		BuiltInRegistries.ITEM.getResourceKey(item)
			.flatMap(BuiltInRegistries.ITEM::get)
			.ifPresent(holder -> holder.bindComponents(DataComponentMap.EMPTY));
		return new ItemStack(item);
	}
}
