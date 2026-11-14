package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.SharedConstants;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 物品分组的纯逻辑：组定义匹配、去重顺序、最小成员数、排序、自定义组解析与展开/收起。
 * 行为对齐 {@code BizCub/inventory-item-groups}。
 */
class CreativeItemGroupsTest {
	private static final String TAB = "minecraft:building_blocks";

	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		BuiltInRegistries.ITEM.freeze();
	}

	// 分组状态是进程级静态：逐用例准备/清理，避免顺序依赖。
	@BeforeEach
	void enableGrouping() {
		TweakifyMod.CONFIG = new TweakifyModConfig();
		TweakifyMod.CONFIG.functions.itemGroups = true;
	}

	@AfterEach
	void resetGroupState() {
		CreativeItemGroups.clear();
		TweakifyMod.CONFIG = null;
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
	void definitionMatchesContainedPatterns() {
		CreativeItemGroups.GroupDefinition stairs = definition("stairs", List.of("stair"), List.of());

		assertTrue(stairs.matches("minecraft:oak_stairs"));
		assertFalse(stairs.matches("minecraft:oak_slab"));
	}

	@Test
	void definitionExcludesByAnyNotContainedPattern() {
		// 与参考实现一致：排除项用 anyMatch(!contains)，任意一个不命中即放行。
		CreativeItemGroups.GroupDefinition logs = definition("logs",
			List.of("log", "stem", "bamboo_block"), List.of("stripped"));

		assertTrue(logs.matches("minecraft:oak_log"));
		assertFalse(logs.matches("minecraft:stripped_oak_log"));
		assertFalse(logs.matches("minecraft:stone"));
	}

	@Test
	void equivalentItemsMatchByFullId() {
		CreativeItemGroups.GroupDefinition quartz = new CreativeItemGroups.GroupDefinition(
			"quartz", TAB, List.of("quartz_block"), List.of(), List.of("minecraft:quartz"));

		assertTrue(quartz.matches("minecraft:quartz"));
		assertTrue(quartz.matches("minecraft:quartz_block"));
		assertFalse(quartz.matches("minecraft:quartz_stairs"));
	}

	@Test
	void groupsBelowThreeMembersAreDropped() {
		CreativeItemGroups.GroupDefinition stairs = definition("stairs", List.of("stair"), List.of());
		List<ItemStack> twoMembers = List.of(stack(Items.OAK_STAIRS), stack(Items.STONE_STAIRS));

		assertTrue(CreativeItemGroups.create(List.of(stairs), twoMembers).isEmpty(),
			"成员少于 3 个的组不成立");

		List<ItemStack> threeMembers = List.of(
			stack(Items.OAK_STAIRS), stack(Items.STONE_STAIRS), stack(Items.SPRUCE_STAIRS));
		List<CreativeItemGroups.Group> groups = CreativeItemGroups.create(List.of(stairs), threeMembers);
		assertEquals(1, groups.size());
		assertEquals(3, groups.getFirst().items().size());
		assertFalse(groups.getFirst().visible(), "新建的组默认收起");
	}

	@Test
	void iconIsFirstMemberAndLaterGroupsDoNotReclaimItems() {
		ItemStack oak = stack(Items.OAK_STAIRS);
		ItemStack stone = stack(Items.STONE_STAIRS);
		ItemStack spruce = stack(Items.SPRUCE_STAIRS);
		CreativeItemGroups.GroupDefinition first = definition("stairs", List.of("stair"), List.of());
		CreativeItemGroups.GroupDefinition second = definition("again", List.of("stair"), List.of());

		List<CreativeItemGroups.Group> groups = CreativeItemGroups.create(
			List.of(first, second), List.of(oak, stone, spruce));

		assertEquals(1, groups.size(), "后一个组拿不到任何物品，会被最小成员数过滤掉");
		assertSame(oak, groups.getFirst().icon(), "组图标是组内第一个物品");
	}

	@Test
	void alphabeticalSortOrdersMembersByItemId() {
		ItemStack birch = stack(Items.BIRCH_STAIRS);
		ItemStack oak = stack(Items.OAK_STAIRS);
		ItemStack stone = stack(Items.STONE_STAIRS);
		List<CreativeItemGroups.GroupDefinition> definitions =
			List.of(definition("stairs", List.of("stair"), List.of()));

		List<CreativeItemGroups.Group> sorted = CreativeItemGroups.create(
			definitions, List.of(stone, oak, birch), true);
		assertEquals(List.of(birch, oak, stone), sorted.getFirst().items());

		List<CreativeItemGroups.Group> original = CreativeItemGroups.create(
			definitions, List.of(stone, oak, birch), false);
		assertEquals(List.of(stone, oak, birch), original.getFirst().items(),
			"默认排序保持物品栏原顺序");
	}

	@Test
	void customGroupsParseFromListFormat() {
		List<CreativeItemGroups.GroupDefinition> definitions = CreativeItemGroups.parseCustomGroups(List.of(
			"minecraft:building_blocks|石英|quartz",
			"  minecraft:combat | 武器 | sword, axe | | minecraft:trident ",
			"字段不足",
			"|没有标签页|stairs"));

		assertEquals(2, definitions.size(), "字段不足或缺少标签页的行会被忽略");

		CreativeItemGroups.GroupDefinition quartz = definitions.get(0);
		assertEquals("石英", quartz.name());
		assertEquals(TAB, quartz.tabId());
		assertTrue(quartz.matches("minecraft:quartz_block"));

		CreativeItemGroups.GroupDefinition weapons = definitions.get(1);
		assertEquals("minecraft:combat", weapons.tabId());
		assertEquals(List.of("sword", "axe"), weapons.containedItems());
		assertTrue(weapons.matches("minecraft:iron_sword"));
		assertTrue(weapons.matches("minecraft:trident"), "等价 ID 不参与包含匹配但能直接命中");
	}

	// 自定义组必须只在声明的标签页生效：否则 combat 页的 sword 组会在建筑页把剑也折叠起来。
	@Test
	void customGroupsOnlyApplyToTheirTab() {
		ItemStack oak = stack(Items.OAK_STAIRS);
		ItemStack stone = stack(Items.STONE_STAIRS);
		ItemStack spruce = stack(Items.SPRUCE_STAIRS);
		ItemStack iron = stack(Items.IRON_SWORD);
		ItemStack gold = stack(Items.GOLDEN_SWORD);
		ItemStack diamond = stack(Items.DIAMOND_SWORD);

		TweakifyMod.CONFIG.functions.itemGroupsAddAll = false;
		TweakifyMod.CONFIG.setStringList(TweakifyModConfig.CUSTOM_ITEM_GROUPS_FIELD, List.of(
			"minecraft:building_blocks|楼梯|stair",
			"minecraft:combat|剑|sword"));

		CreativeItemGroups.beginTab(BuiltInRegistries.CREATIVE_MODE_TAB.getValue(
			CreativeModeTabs.BUILDING_BLOCKS));
		List<ItemStack> stripped = CreativeItemGroups.stripMembers(
			List.of(oak, stone, spruce, iron, gold, diamond));

		assertEquals(4, stripped.size(), "当前页只应摘除楼梯成员，三把剑保持原样");
		assertTrue(stripped.contains(iron));
		assertTrue(stripped.contains(gold));
		assertTrue(stripped.contains(diamond));
		assertFalse(stripped.contains(stone), "楼梯成员已被摘除");
		assertTrue(stripped.contains(oak), "组图标保留在列表中");
	}

	@Test
	void toggleInsertsMembersAfterIconAndRemovesThem() {
		ItemStack oak = stack(Items.OAK_STAIRS);
		ItemStack stone = stack(Items.STONE_STAIRS);
		ItemStack spruce = stack(Items.SPRUCE_STAIRS);
		ItemStack dirt = stack(Items.DIRT);
		ItemStack sand = stack(Items.SAND);
		CreativeItemGroups.Group group = new CreativeItemGroups.Group(
			Component.literal("Stairs"), List.of(oak, stone, spruce));

		NonNullList<ItemStack> items = NonNullList.create();
		items.add(dirt);
		items.add(oak);
		items.add(sand);
		group.setIconIndex(1);

		CreativeItemGroups.toggle(group, items);
		assertTrue(group.visible());
		// 参考实现会把组内全部物品（含图标物品本身）插到图标之后。
		assertEquals(List.of(dirt, oak, oak, stone, spruce, sand), items);

		CreativeItemGroups.toggle(group, items);
		assertFalse(group.visible());
		assertEquals(List.of(dirt, oak, sand), items, "收起后回到展开前的内容");
	}

	@Test
	void displayIndexMapsDisplayedSlotsBackToTheList() {
		ItemStack oak = stack(Items.OAK_STAIRS);
		ItemStack stone = stack(Items.STONE_STAIRS);
		ItemStack spruce = stack(Items.SPRUCE_STAIRS);
		CreativeItemGroups.installGroupsForTest(List.of(), List.of(oak, stone, spruce));
		List<Slot> slots = List.of(slot(oak), slot(stone), slot(spruce));

		assertEquals(0, CreativeItemGroups.displayIndex(slots, 0));
		assertEquals(1, CreativeItemGroups.displayIndex(slots, 1));
		assertEquals(2, CreativeItemGroups.displayIndex(slots, 2));
		assertEquals(-1, CreativeItemGroups.displayIndex(List.of(), 0));
	}

	@Test
	void creativeSlotLimitMatchesTheItemPickerGrid() {
		assertTrue(CreativeItemGroups.isCreativeSlot(0));
		assertTrue(CreativeItemGroups.isCreativeSlot(CreativeItemGroups.CREATIVE_SLOT_LIMIT));
		assertFalse(CreativeItemGroups.isCreativeSlot(CreativeItemGroups.CREATIVE_SLOT_LIMIT + 1));
		assertFalse(CreativeItemGroups.isCreativeSlot(-1));
	}

	// 显示索引的偏移只取决于“前两个槽位 + 基准列表”，基准变化后必须重新计算。
	@Test
	void displayIndexRecomputesWhenItemsChange() {
		ItemStack dirt = stack(Items.DIRT);
		ItemStack oak = stack(Items.OAK_STAIRS);
		ItemStack stone = stack(Items.STONE_STAIRS);
		ItemStack spruce = stack(Items.SPRUCE_STAIRS);
		// 前两个槽位固定显示 oak / stone，基准列表变化会改变偏移。
		List<Slot> slots = List.of(slot(oak), slot(stone), slot(spruce));

		CreativeItemGroups.installGroupsForTest(List.of(), List.of(dirt, oak, stone, spruce));
		assertEquals(1, CreativeItemGroups.displayIndex(slots, 0), "oak 在基准列表的第 1 位");
		assertEquals(3, CreativeItemGroups.displayIndex(slots, 2));

		CreativeItemGroups.installGroupsForTest(List.of(), List.of(oak, stone, spruce));
		assertEquals(0, CreativeItemGroups.displayIndex(slots, 0), "基准变化后偏移必须失效重算");
	}

	// 创造模式列表滚动只更新槽位内容、不替换 slots 列表：偏移必须跟着重算，
	// 否则滚动后组图标/成员标记与点击判定仍停留在第一页。
	@Test
	void displayIndexRecomputesAfterViewportScroll() {
		ItemStack dirt = stack(Items.DIRT);
		ItemStack oak = stack(Items.OAK_STAIRS);
		ItemStack stone = stack(Items.STONE_STAIRS);
		ItemStack spruce = stack(Items.SPRUCE_STAIRS);
		ItemStack sand = stack(Items.SAND);
		ItemStack gravel = stack(Items.GRAVEL);

		SimpleContainer container = new SimpleContainer(3);
		List<Slot> slots = List.of(
			new Slot(container, 0, 0, 0), new Slot(container, 1, 0, 0), new Slot(container, 2, 0, 0));
		container.setItem(0, dirt);
		container.setItem(1, oak);
		container.setItem(2, stone);

		CreativeItemGroups.installGroupsForTest(List.of(), List.of(dirt, oak, stone, spruce, sand, gravel));
		assertEquals(0, CreativeItemGroups.displayIndex(slots, 0));
		assertEquals(2, CreativeItemGroups.displayIndex(slots, 2));

		// 模拟滚动：slots 列表引用与槽位对象都不变，只换容器里的当前视图内容。
		container.setItem(0, spruce);
		container.setItem(1, sand);
		container.setItem(2, gravel);

		assertEquals(3, CreativeItemGroups.displayIndex(slots, 0), "滚动后必须重新计算偏移");
		assertEquals(5, CreativeItemGroups.displayIndex(slots, 2));
	}

	// 配置变化只需重建分类页：快捷栏/背包页物品来自其他数据源，搜索页由搜索框维护。
	@Test
	void configChangeOnlyRefreshesCategoryContents() {
		assertTrue(CreativeItemGroups.needsContentRefreshOnConfigChange(CreativeModeTab.Type.CATEGORY));
		assertFalse(CreativeItemGroups.needsContentRefreshOnConfigChange(CreativeModeTab.Type.SEARCH));
		assertFalse(CreativeItemGroups.needsContentRefreshOnConfigChange(CreativeModeTab.Type.HOTBAR));
		assertFalse(CreativeItemGroups.needsContentRefreshOnConfigChange(CreativeModeTab.Type.INVENTORY));
	}

	// 轮播只用于展开的组：收起时图标固定为首个成员，关闭轮播时也不会返回替换图标。
	@Test
	void collapsedIconDoesNotRotate() {
		CreativeItemGroups.Group group = new CreativeItemGroups.Group(
			Component.literal("Stairs"), List.of(
				stack(Items.OAK_STAIRS), stack(Items.STONE_STAIRS), stack(Items.SPRUCE_STAIRS)));
		TweakifyMod.CONFIG.functions.itemGroupsShowItems = true;

		assertNull(CreativeItemGroups.rotatingIcon(group), "收起的组不轮播");

		group.setVisible(true);
		assertNotNull(CreativeItemGroups.rotatingIcon(group), "展开后开启显示组内物品才轮播");

		TweakifyMod.CONFIG.functions.itemGroupsShowItems = false;
		assertNull(CreativeItemGroups.rotatingIcon(group), "关闭显示组内物品后不轮播");
	}

	// 渲染快照：收起只标图标（可展开），展开时标图标 + 三个成员位（含图标物品自身）。
	@Test
	void renderSnapshotTracksExpandedIconsAndMembers() {
		ItemStack dirt = stack(Items.DIRT);
		ItemStack oak = stack(Items.OAK_STAIRS);
		ItemStack stone = stack(Items.STONE_STAIRS);
		ItemStack spruce = stack(Items.SPRUCE_STAIRS);
		CreativeItemGroups.Group group = new CreativeItemGroups.Group(
			Component.literal("Stairs"), List.of(oak, stone, spruce));
		CreativeItemGroups.installGroupsForTest(List.of(group), List.of(dirt, oak));

		assertTrue(CreativeItemGroups.isCollapsedIconSlot(1));
		assertFalse(CreativeItemGroups.isExpandedIconSlot(1));
		assertFalse(CreativeItemGroups.isMemberSlot(1));
		assertSame(group, CreativeItemGroups.iconGroup(1));
		assertNull(CreativeItemGroups.iconGroup(0), "非图标槽位不返回组");
		assertNull(CreativeItemGroups.iconGroup(-1));

		NonNullList<ItemStack> items = NonNullList.create();
		items.add(dirt);
		items.add(oak);
		CreativeItemGroups.toggle(group, items);

		assertTrue(CreativeItemGroups.isExpandedIconSlot(1));
		assertFalse(CreativeItemGroups.isCollapsedIconSlot(1));
		assertFalse(CreativeItemGroups.isMemberSlot(1), "图标槽位不算成员位");
		assertTrue(CreativeItemGroups.isMemberSlot(2));
		assertTrue(CreativeItemGroups.isMemberSlot(3));
		assertTrue(CreativeItemGroups.isMemberSlot(4));
		assertFalse(CreativeItemGroups.isMemberSlot(5), "越界索引不返回 true");
	}

	// 列表被刷新后旧组可能指向不存在的物品：必须不画图标/高亮，而不是沿用旧索引。
	@Test
	void staleGroupWithoutIconIsNotRendered() {
		ItemStack dirt = stack(Items.DIRT);
		ItemStack oak = stack(Items.OAK_STAIRS);
		CreativeItemGroups.Group group = new CreativeItemGroups.Group(
			Component.literal("Stairs"), List.of(oak, stack(Items.STONE_STAIRS), stack(Items.SPRUCE_STAIRS)));
		CreativeItemGroups.installGroupsForTest(List.of(group), List.of(dirt, oak));
		assertSame(group, CreativeItemGroups.iconGroup(1));

		// 模拟“列表换了但没重新成组”：基准里已经没有组图标。
		CreativeItemGroups.installGroupsForTest(List.of(group),
			List.of(dirt, stack(Items.SAND), stack(Items.GRAVEL)));
		assertNull(CreativeItemGroups.iconGroup(1), "图标不存在的组不能占用槽位");
		assertFalse(CreativeItemGroups.isExpandedIconSlot(1));
		assertFalse(CreativeItemGroups.isCollapsedIconSlot(1));
		assertFalse(CreativeItemGroups.isMemberSlot(1));
	}

	// 展示列表重建时必须重新成组，而不是复用上一份列表算出的旧组。
	@Test
	void stripMembersRebuildsGroupsInsteadOfReusingStaleOnes() {
		ItemStack dirt = stack(Items.DIRT);
		ItemStack oak = stack(Items.OAK_STAIRS);
		ItemStack stone = stack(Items.STONE_STAIRS);
		ItemStack spruce = stack(Items.SPRUCE_STAIRS);
		CreativeItemGroups.Group group = new CreativeItemGroups.Group(
			Component.literal("Stairs"), List.of(oak, stone, spruce));
		CreativeItemGroups.installGroupsForTest(List.of(group), List.of(dirt, oak));
		assertSame(group, CreativeItemGroups.iconGroup(1));

		// 没有当前标签页（等价于外部刷新时的重建）：旧组必须被丢弃，成员不再被摘除。
		List<ItemStack> stripped = CreativeItemGroups.stripMembers(List.of(dirt, oak, stone, spruce));
		assertEquals(4, stripped.size(), "旧组已被丢弃，成员不再被摘除");
		assertNull(CreativeItemGroups.iconGroup(1));
		assertFalse(CreativeItemGroups.isMemberSlot(2));
	}

	private static CreativeItemGroups.GroupDefinition definition(String name, List<String> contained,
																 List<String> excluded) {
		return new CreativeItemGroups.GroupDefinition(name, TAB, contained, excluded, List.of());
	}

	private static ItemStack stack(Item item) {
		// 单元测试没有完整数据包加载流程，手动绑定空组件表，避免 Holder 的组件尚未绑定。
		BuiltInRegistries.ITEM.getResourceKey(item)
			.flatMap(BuiltInRegistries.ITEM::get)
			.ifPresent(holder -> holder.bindComponents(DataComponentMap.EMPTY));
		return new ItemStack(item);
	}

	private static Slot slot(ItemStack stack) {
		SimpleContainer container = new SimpleContainer(stack);
		return new Slot(container, 0, 0, 0);
	}
}
