package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.SharedConstants;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
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
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

		// 正向：切到 combat 页后，同一个配置文件里的“剑”组应生效，楼梯保持原样。
		CreativeItemGroups.beginTab(BuiltInRegistries.CREATIVE_MODE_TAB.getValue(
			CreativeModeTabs.COMBAT));
		List<ItemStack> combatStripped = CreativeItemGroups.stripMembers(
			List.of(oak, stone, spruce, iron, gold, diamond));

		assertEquals(4, combatStripped.size(), "combat 页只应摘除剑成员，楼梯保持原样");
		assertTrue(combatStripped.contains(oak));
		assertTrue(combatStripped.contains(iron));
		assertFalse(combatStripped.contains(gold));
		CreativeItemGroups.Group swordGroup = CreativeItemGroups.iconGroup(3);
		assertNotNull(swordGroup);
		assertEquals("剑", swordGroup.name().getString());
		assertEquals(List.of(iron, gold, diamond), swordGroup.items());
	}

	// 自定义组 tabId 是自由文本：缺命名空间或大小写不同时也要能匹配已注册的标签页。
	@Test
	void customGroupTabIdsAreNormalized() {
		ItemStack oakLog = stack(Items.OAK_LOG);
		ItemStack spruceLog = stack(Items.SPRUCE_LOG);
		ItemStack birchLog = stack(Items.BIRCH_LOG);
		ItemStack stone = stack(Items.STONE);
		ItemStack stoneBricks = stack(Items.STONE_BRICKS);
		ItemStack smoothStone = stack(Items.SMOOTH_STONE);

		assertEquals("minecraft:building_blocks", CreativeItemGroups.normalizeTabId(" Building_Blocks "));
		assertEquals("minecraft:building_blocks", CreativeItemGroups.normalizeTabId("Minecraft:Building_Blocks"));
		assertEquals("", CreativeItemGroups.normalizeTabId(null));
		assertEquals("", CreativeItemGroups.normalizeTabId("   "));
		// 重复命名空间/多冒号只小写，不尝试修正，交给告警谓词报错。
		assertEquals("minecraft:minecraft:stone", CreativeItemGroups.normalizeTabId("Minecraft:Minecraft:Stone"));
		// 空命名空间交给 Identifier 补全：":combat" 与 "minecraft:combat" 必须等价。
		assertEquals("minecraft:combat", CreativeItemGroups.normalizeTabId(":combat"));
		assertEquals("minecraft:stone", CreativeItemGroups.normalizeTabId(":stone"));
		assertEquals("minecraft:", CreativeItemGroups.normalizeTabId(":"));

		TweakifyMod.CONFIG.functions.itemGroupsAddAll = false;
		TweakifyMod.CONFIG.setStringList(TweakifyModConfig.CUSTOM_ITEM_GROUPS_FIELD, List.of(
			"building_blocks|原木|_log",
			"Building_Blocks|石制|stone"));

		CreativeItemGroups.beginTab(BuiltInRegistries.CREATIVE_MODE_TAB.getValue(
			CreativeModeTabs.BUILDING_BLOCKS));
		List<ItemStack> stripped = CreativeItemGroups.stripMembers(
			List.of(oakLog, spruceLog, birchLog, stone, stoneBricks, smoothStone));

		// 两组匹配集不重叠：去掉小写归一化时第二条会失败，结果大小从 2 变成 4。
		assertEquals(2, stripped.size(), "两条自定义组都应归一化到 building_blocks 并各自成组");
		assertSame(oakLog, stripped.get(0));
		assertSame(stone, stripped.get(1));
		CreativeItemGroups.Group logGroup = CreativeItemGroups.iconGroup(0);
		CreativeItemGroups.Group stoneGroup = CreativeItemGroups.iconGroup(1);
		assertNotNull(logGroup);
		assertNotNull(stoneGroup);
		assertEquals("原木", logGroup.name().getString());
		assertEquals("石制", stoneGroup.name().getString());
	}

	// 空命名空间（":combat"）归一化后必须与 minecraft:combat 完全等价并能成组。
	@Test
	void colonPrefixedTabIdIsNormalizedAndGrouped() {
		ItemStack iron = stack(Items.IRON_SWORD);
		ItemStack gold = stack(Items.GOLDEN_SWORD);
		ItemStack diamond = stack(Items.DIAMOND_SWORD);

		TweakifyMod.CONFIG.functions.itemGroupsAddAll = false;
		TweakifyMod.CONFIG.setStringList(TweakifyModConfig.CUSTOM_ITEM_GROUPS_FIELD, List.of(
			":combat|剑|sword"));
		CreativeModeTab combat = BuiltInRegistries.CREATIVE_MODE_TAB.getValue(CreativeModeTabs.COMBAT);
		assertNotNull(combat, "COMBAT 标签页必须在注册表中");
		CreativeItemGroups.beginTab(combat);

		List<ItemStack> stripped = CreativeItemGroups.stripMembers(List.of(iron, gold, diamond));

		assertEquals(1, stripped.size(), "空命名空间 tabId 归一化后应在 combat 页成组");
		CreativeItemGroups.Group group = CreativeItemGroups.iconGroup(0);
		assertNotNull(group);
		assertEquals("剑", group.name().getString());
	}

	// 空命名空间/未注册 tabId 的告警端到端：配置行写错时切页后必须留下告警记录。
	@Test
	void colonPrefixedInvalidTabsWarnEndToEnd() {
		TweakifyMod.CONFIG.functions.itemGroupsAddAll = false;
		TweakifyMod.CONFIG.setStringList(TweakifyModConfig.CUSTOM_ITEM_GROUPS_FIELD, List.of(
			":search|搜索|stone",
			":hotbar|快捷栏|stone",
			":inventory|背包|stone",
			":nonexistent|缺失|stone"));
		CreativeItemGroups.beginTab(BuiltInRegistries.CREATIVE_MODE_TAB.getValue(
			CreativeModeTabs.BUILDING_BLOCKS));

		assertTrue(CreativeItemGroups.isCustomTabWarnedForTest(":search"),
			"非分类页 ID 必须告警");
		assertTrue(CreativeItemGroups.isCustomTabWarnedForTest(":hotbar"),
			"快捷栏 ID 必须告警");
		assertTrue(CreativeItemGroups.isCustomTabWarnedForTest(":inventory"),
			"背包 ID 必须告警");
		assertTrue(CreativeItemGroups.isCustomTabWarnedForTest(":nonexistent"),
			"未注册 ID 必须告警");
		assertFalse(CreativeItemGroups.warnUnsupportedCustomTab(":search"),
			"端到端已告警过，再次调用不应重复");

		List<ItemStack> stripped = CreativeItemGroups.stripMembers(
			List.of(stack(Items.STONE), stack(Items.STONE_BRICKS), stack(Items.SMOOTH_STONE)));
		assertEquals(3, stripped.size(), "无效标签页的自定义组不得摘除物品");
	}

	// ":" / "minecraft:"（空 path）端到端：两行归一化到同一键后只登记一次。
	@Test
	void emptyPathTabsWarnEndToEnd() {
		TweakifyMod.CONFIG.functions.itemGroupsAddAll = false;
		// 先只放第一行，确认登记一次。
		TweakifyMod.CONFIG.setStringList(TweakifyModConfig.CUSTOM_ITEM_GROUPS_FIELD,
			List.of(":|空路径|stone"));
		CreativeItemGroups.beginTab(BuiltInRegistries.CREATIVE_MODE_TAB.getValue(
			CreativeModeTabs.BUILDING_BLOCKS));

		assertTrue(CreativeItemGroups.isCustomTabWarnedForTest("minecraft:"),
			"空 path 的 tabId 必须告警");
		int warnedAfterFirstRow = CreativeItemGroups.warnedTabCountForTest();
		assertEquals(1, warnedAfterFirstRow, "第一行应登记一次");

		// 追加归一化到同一键的第二行：去重集合不应增加。
		TweakifyMod.CONFIG.setStringList(TweakifyModConfig.CUSTOM_ITEM_GROUPS_FIELD, List.of(
			":|空路径|stone",
			"minecraft:|空路径2|stone"));
		CreativeItemGroups.warnUnsupportedCustomGroups();
		assertEquals(warnedAfterFirstRow, CreativeItemGroups.warnedTabCountForTest(),
			"两行归一化后只登记一次");
		assertFalse(CreativeItemGroups.warnUnsupportedCustomTab("minecraft:"));

		List<ItemStack> stripped = CreativeItemGroups.stripMembers(
			List.of(stack(Items.STONE), stack(Items.STONE_BRICKS), stack(Items.SMOOTH_STONE)));
		assertEquals(3, stripped.size(), "空 path 的自定义组不得摘除物品");
	}

	// 默认组开启（默认值）时，自定义组仍应参与成组，不能被默认组配置分支跳过。
	@Test
	void customGroupsWorkWithDefaultGroups() {
		ItemStack oakStairs = stack(Items.OAK_STAIRS);
		ItemStack stoneStairs = stack(Items.STONE_STAIRS);
		ItemStack spruceStairs = stack(Items.SPRUCE_STAIRS);
		ItemStack dirt = stack(Items.DIRT);
		ItemStack coarse = stack(Items.COARSE_DIRT);
		ItemStack rooted = stack(Items.ROOTED_DIRT);

		TweakifyMod.CONFIG.setStringList(TweakifyModConfig.CUSTOM_ITEM_GROUPS_FIELD, List.of(
			"minecraft:building_blocks|泥土|dirt"));

		CreativeItemGroups.beginTab(BuiltInRegistries.CREATIVE_MODE_TAB.getValue(
			CreativeModeTabs.BUILDING_BLOCKS));
		List<ItemStack> stripped = CreativeItemGroups.stripMembers(
			List.of(oakStairs, stoneStairs, spruceStairs, dirt, coarse, rooted));

		// 默认 stairs 组与自定义 dirt 组必须同时成立：只启用一个时 stripped 会是 4 项。
		assertEquals(2, stripped.size(), "默认组与自定义组应同时成立");
		CreativeItemGroups.Group defaultGroup = CreativeItemGroups.iconGroup(0);
		CreativeItemGroups.Group customGroup = CreativeItemGroups.iconGroup(1);
		assertNotNull(defaultGroup, "默认楼梯组应成立");
		assertNotNull(customGroup, "自定义泥土组应成立");
		assertSame(oakStairs, defaultGroup.icon());
		assertSame(dirt, customGroup.icon());
	}

	// 非分类页（搜索/快捷栏/背包）不得成组：各页独立用例，失败不会中断其他页的判定。
	@Test
	void customGroupsAreIgnoredOnSearchTab() {
		assertNonCategoryTabIgnored(CreativeModeTabs.SEARCH);
	}

	@Test
	void customGroupsAreIgnoredOnHotbarTab() {
		assertNonCategoryTabIgnored(CreativeModeTabs.HOTBAR);
	}

	@Test
	void customGroupsAreIgnoredOnInventoryTab() {
		assertNonCategoryTabIgnored(CreativeModeTabs.INVENTORY);
	}

	// 每个非分类页都用它自己的 tabId 配置自定义组：门禁只排除某一类时其余页会成组。
	private static void assertNonCategoryTabIgnored(ResourceKey<CreativeModeTab> key) {
		TweakifyMod.CONFIG.functions.itemGroupsAddAll = false;
		CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValue(key);
		assertNotNull(tab, "标签页必须已注册: " + key.identifier());
		String tabId = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab).toString();
		TweakifyMod.CONFIG.setStringList(TweakifyModConfig.CUSTOM_ITEM_GROUPS_FIELD,
			List.of(tabId + "|测试|stone"));
		CreativeItemGroups.clear();
		CreativeItemGroups.beginTab(tab);

		List<ItemStack> stripped = CreativeItemGroups.stripMembers(
			List.of(stack(Items.STONE), stack(Items.STONE_BRICKS), stack(Items.SMOOTH_STONE)));

		assertEquals(3, stripped.size(), tabId + " 非分类页不得摘除任何物品");
		assertNull(CreativeItemGroups.iconGroup(0), tabId + " 不应成组");
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
		ItemStack gravel = stack(Items.GRAVEL);
		ItemStack sand = stack(Items.SAND);
		ItemStack oak = stack(Items.OAK_STAIRS);
		ItemStack stone = stack(Items.STONE_STAIRS);
		ItemStack spruce = stack(Items.SPRUCE_STAIRS);
		CreativeItemGroups.Group group = new CreativeItemGroups.Group(
			Component.literal("Stairs"), List.of(oak, stone, spruce));

		SimpleContainer container = new SimpleContainer(3);
		List<Slot> slots = List.of(
			new Slot(container, 0, 0, 0), new Slot(container, 1, 0, 0), new Slot(container, 2, 0, 0));
		container.setItem(0, dirt);
		container.setItem(1, gravel);
		container.setItem(2, sand);

		// 组图标 oak 位于基准列表第 3 位：初始视图完全不含图标。
		// 基准只含图标（与 stripMembers 摘除成员后的生产形态一致）。
		CreativeItemGroups.installGroupsForTest(List.of(group), List.of(dirt, gravel, sand, oak));
		assertEquals(0, CreativeItemGroups.displayIndex(slots, 0));
		assertNull(CreativeItemGroups.iconGroup(CreativeItemGroups.displayIndex(slots, 0)));

		// 模拟滚动到图标所在窗口：slots 引用与槽位对象都不变，只换容器内容。
		container.setItem(0, oak);
		container.setItem(1, ItemStack.EMPTY);
		container.setItem(2, ItemStack.EMPTY);

		assertEquals(3, CreativeItemGroups.displayIndex(slots, 0), "滚动后必须重新计算偏移");
		assertSame(group, CreativeItemGroups.iconGroup(CreativeItemGroups.displayIndex(slots, 0)),
			"图标标记必须跟随滚动后的槽位");
		assertTrue(CreativeItemGroups.isCollapsedIconSlot(CreativeItemGroups.displayIndex(slots, 0)));
		assertFalse(CreativeItemGroups.isMemberSlot(CreativeItemGroups.displayIndex(slots, 0)));
	}

	// 只比较首个锚点会漏掉“首个锚点仍是同一引用、第二个锚点变化”的滚动场景。
	@Test
	void displayIndexRecomputesWhenOnlySecondAnchorChanges() {
		ItemStack dirt = stack(Items.DIRT);
		ItemStack gravel = stack(Items.GRAVEL);
		ItemStack oak = stack(Items.OAK_STAIRS);
		ItemStack stone = stack(Items.STONE_STAIRS);

		SimpleContainer container = new SimpleContainer(2);
		List<Slot> slots = List.of(new Slot(container, 0, 0, 0), new Slot(container, 1, 0, 0));
		container.setItem(0, dirt);
		container.setItem(1, gravel);

		// dirt 在基准列表中出现两次：滚动后首个锚点仍是同一个 dirt 引用。
		CreativeItemGroups.installGroupsForTest(List.of(), List.of(dirt, gravel, dirt, oak, stone));
		assertEquals(0, CreativeItemGroups.displayIndex(slots, 0));

		container.setItem(1, oak);
		assertEquals(2, CreativeItemGroups.displayIndex(slots, 0),
			"第二个锚点变化也必须重算偏移");
	}

	// 只比较第二个锚点会漏掉“首个锚点变化、第二个锚点保持同一引用”的滚动场景。
	@Test
	void displayIndexRecomputesWhenOnlyFirstAnchorChanges() {
		ItemStack dirt = stack(Items.DIRT);
		ItemStack gravel = stack(Items.GRAVEL);
		ItemStack stone = stack(Items.STONE);

		SimpleContainer container = new SimpleContainer(2);
		List<Slot> slots = List.of(new Slot(container, 0, 0, 0), new Slot(container, 1, 0, 0));
		container.setItem(0, dirt);
		container.setItem(1, gravel);

		// gravel 在基准列表中重复：滚动后两个锚点都指向同一个 gravel 引用，
		// 只有首个锚点从 dirt 变成了 gravel。
		CreativeItemGroups.installGroupsForTest(List.of(), List.of(dirt, gravel, gravel, stone));
		assertEquals(0, CreativeItemGroups.displayIndex(slots, 0));

		container.setItem(0, gravel);
		assertEquals(1, CreativeItemGroups.displayIndex(slots, 0),
			"首个锚点变化、第二个锚点不变时也必须重算偏移");
	}

	// slots.size()==1：第二个锚点越界要回退到 EMPTY，并用首槽物品反推偏移。
	@Test
	void displayIndexHandlesSingleSlotViewport() {
		ItemStack dirt = stack(Items.DIRT);
		ItemStack oak = stack(Items.OAK_STAIRS);
		ItemStack stone = stack(Items.STONE);

		SimpleContainer container = new SimpleContainer(1);
		List<Slot> slots = List.of(new Slot(container, 0, 0, 0));
		container.setItem(0, oak);

		CreativeItemGroups.installGroupsForTest(List.of(), List.of(dirt, oak, stone));
		assertEquals(1, CreativeItemGroups.displayIndex(slots, 0));
		assertEquals(2, CreativeItemGroups.displayIndex(slots, 1));
	}

	// 展开态下滚动到成员窗口：成员标记必须跟随当前视图，而不是停在展开时的位置。
	@Test
	void expandedGroupMembersFollowViewportScroll() {
		ItemStack dirt = stack(Items.DIRT);
		ItemStack oak = stack(Items.OAK_STAIRS);
		ItemStack stone = stack(Items.STONE_STAIRS);
		ItemStack spruce = stack(Items.SPRUCE_STAIRS);
		ItemStack sand = stack(Items.SAND);
		CreativeItemGroups.Group group = new CreativeItemGroups.Group(
			Component.literal("Stairs"), List.of(oak, stone, spruce));

		NonNullList<ItemStack> items = NonNullList.create();
		items.add(dirt);
		items.add(oak);
		items.add(sand);
		// toggle 只重建 GROUPS 中的组：先把组注入，模拟生产路径里从 iconGroup 拿到的对象。
		CreativeItemGroups.installGroupsForTest(List.of(group), List.of(dirt, oak, sand));
		group.setIconIndex(1);
		CreativeItemGroups.toggle(group, items);

		SimpleContainer container = new SimpleContainer(3);
		List<Slot> slots = List.of(
			new Slot(container, 0, 0, 0), new Slot(container, 1, 0, 0), new Slot(container, 2, 0, 0));
		container.setItem(0, dirt);
		container.setItem(1, oak);
		container.setItem(2, oak);

		assertTrue(CreativeItemGroups.isExpandedIconSlot(CreativeItemGroups.displayIndex(slots, 1)));
		assertTrue(CreativeItemGroups.isMemberSlot(CreativeItemGroups.displayIndex(slots, 2)));

		// 滚动到成员窗口：槽位 0 变为成员 oak 的副本。
		container.setItem(0, oak);
		container.setItem(1, stone);
		container.setItem(2, spruce);

		assertTrue(CreativeItemGroups.isMemberSlot(CreativeItemGroups.displayIndex(slots, 0)),
			"滚动到成员窗口后，成员标记必须跟随滚动后的槽位");
		assertFalse(CreativeItemGroups.isExpandedIconSlot(CreativeItemGroups.displayIndex(slots, 0)));
	}

	// 配置变化只需重建分类页：快捷栏/背包页物品来自其他数据源，搜索页由搜索框维护。
	@Test
	void configChangeOnlyRefreshesCategoryContents() {
		assertTrue(CreativeItemGroups.needsContentRefreshOnConfigChange(CreativeModeTab.Type.CATEGORY));
		assertFalse(CreativeItemGroups.needsContentRefreshOnConfigChange(CreativeModeTab.Type.SEARCH));
		assertFalse(CreativeItemGroups.needsContentRefreshOnConfigChange(CreativeModeTab.Type.HOTBAR));
		assertFalse(CreativeItemGroups.needsContentRefreshOnConfigChange(CreativeModeTab.Type.INVENTORY));
	}

	// 配置签名逐项覆盖：必需字段各自变化都必须重建，轮播开关不参与签名。
	@Test
	void configSignatureTracksGroupingFieldsOnly() {
		String base = signatureAfter(config -> {
		});

		assertNotEquals(base, signatureAfter(config -> config.functions.itemGroups = true),
			"总开关必须触发重建");
		assertNotEquals(base, signatureAfter(config -> config.functions.itemGroupsAddAll = false),
			"默认组开关必须触发重建");
		assertNotEquals(base, signatureAfter(
				config -> config.functions.itemGroupsSort = TweakifyModConfig.ITEM_GROUP_SORT_ALPHABETICAL),
			"排序必须触发重建");
		assertNotEquals(base, signatureAfter(config -> config.setStringList(
			TweakifyModConfig.CUSTOM_ITEM_GROUPS_FIELD, List.of("minecraft:building_blocks|A|dirt"))),
			"自定义组必须触发重建");
		assertEquals(base, signatureAfter(config -> config.functions.itemGroupsShowItems = true),
			"轮播开关不触发重建");
		assertEquals("", CreativeItemGroups.configSignature(null));
	}

	// 告警谓词：先归一化再分类；分类页受支持，搜索/快捷栏/背包与未注册 ID 需要告警。
	@Test
	void customTabWarningClassifiesTabs() {
		assertNull(CreativeItemGroups.customTabWarning(null));
		assertNull(CreativeItemGroups.customTabWarning(""));
		assertNull(CreativeItemGroups.customTabWarning("   "), "空白归一化为空 ID，视为无配置");
		assertNull(CreativeItemGroups.customTabWarning("minecraft:building_blocks"));
		assertNull(CreativeItemGroups.customTabWarning("minecraft:combat"));
		assertNull(CreativeItemGroups.customTabWarning("MINECRAFT:COMBAT"), "归一化后大小写不敏感");
		assertEquals(CreativeItemGroups.CustomTabWarning.NON_CATEGORY,
			CreativeItemGroups.customTabWarning("minecraft:search"));
		assertEquals(CreativeItemGroups.CustomTabWarning.NON_CATEGORY,
			CreativeItemGroups.customTabWarning("minecraft:hotbar"));
		assertEquals(CreativeItemGroups.CustomTabWarning.NON_CATEGORY,
			CreativeItemGroups.customTabWarning("minecraft:inventory"));
		assertEquals(CreativeItemGroups.CustomTabWarning.NON_CATEGORY,
			CreativeItemGroups.customTabWarning(":search"));
		assertEquals(CreativeItemGroups.CustomTabWarning.NON_CATEGORY,
			CreativeItemGroups.customTabWarning(":hotbar"));
		assertEquals(CreativeItemGroups.CustomTabWarning.NON_CATEGORY,
			CreativeItemGroups.customTabWarning(":inventory"));
		assertEquals(CreativeItemGroups.CustomTabWarning.UNKNOWN,
			CreativeItemGroups.customTabWarning("somemod:missing"));
		assertEquals(CreativeItemGroups.CustomTabWarning.UNKNOWN,
			CreativeItemGroups.customTabWarning(":nonexistent"));
		assertEquals(CreativeItemGroups.CustomTabWarning.UNKNOWN,
			CreativeItemGroups.customTabWarning(":"));
		assertEquals(CreativeItemGroups.CustomTabWarning.UNKNOWN,
			CreativeItemGroups.customTabWarning("minecraft:"));
		assertEquals(CreativeItemGroups.CustomTabWarning.UNKNOWN,
			CreativeItemGroups.customTabWarning("minecraft:/"));
		assertEquals(CreativeItemGroups.CustomTabWarning.UNKNOWN,
			CreativeItemGroups.customTabWarning("minecraft: search"));
		assertEquals(CreativeItemGroups.CustomTabWarning.UNKNOWN,
			CreativeItemGroups.customTabWarning("minecraft:minecraft:stone"));
		assertEquals(CreativeItemGroups.CustomTabWarning.UNKNOWN,
			CreativeItemGroups.customTabWarning("a:b:c"));
		assertEquals(CreativeItemGroups.CustomTabWarning.UNKNOWN,
			CreativeItemGroups.customTabWarning(":stone"));
		assertNull(CreativeItemGroups.customTabWarning(":combat"));
		assertNull(CreativeItemGroups.customTabWarning(":building_blocks"));

		// 非 minecraft 命名空间的已注册分类页通过查找接缝验证，不向全局注册表注册测试 tab。
		CreativeModeTab modCategory = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
			.title(Component.literal("Mod Category"))
			.icon(() -> new ItemStack(Items.STONE))
			.build();
		Map<Identifier, CreativeModeTab> modTabs = Map.of(
			Identifier.parse("tweakify_test:category"), modCategory);
		// 大写输入经归一化后必须查到小写键，直接验证“归一化后的 ID 传入查找”。
		assertNull(CreativeItemGroups.customTabWarning("Tweakify_Test:Category", modTabs::get));
		assertEquals(CreativeItemGroups.CustomTabWarning.UNKNOWN,
			CreativeItemGroups.customTabWarning("tweakify_test:missing", modTabs::get));
		assertEquals(CreativeItemGroups.CustomTabWarning.UNKNOWN,
			CreativeItemGroups.customTabWarning("tweakify_test:boom", id -> {
				throw new IllegalStateException("boom");
			}), "查找异常按未知处理，不得冒泡");
		int failuresBefore = CreativeItemGroups.lookupFailureCountForTest();
		assertEquals(CreativeItemGroups.CustomTabWarning.UNKNOWN,
			CreativeItemGroups.customTabWarning("tweakify_test:boom2", id -> {
				throw new IllegalStateException("boom2");
			}), "查找异常按未知处理，不得冒泡");
		assertEquals(failuresBefore + 1, CreativeItemGroups.lookupFailureCountForTest(),
			"异常降级必须留下诊断计数");
		assertThrows(AssertionError.class,
			() -> CreativeItemGroups.customTabWarning("tweakify_test:error", id -> {
				throw new AssertionError("boom");
			}), "Error 必须传播，不得被接缝吞掉");
		CreativeModeTab search = BuiltInRegistries.CREATIVE_MODE_TAB.getValue(CreativeModeTabs.SEARCH);
		assertNotNull(search);
		Map<Identifier, CreativeModeTab> searchTabs = Map.of(
			Identifier.parse("tweakify_test:search"), search);
		assertEquals(CreativeItemGroups.CustomTabWarning.NON_CATEGORY,
			CreativeItemGroups.customTabWarning("Tweakify_Test:Search", searchTabs::get));

		// Unicode 与超长 ID 不会命中注册表，仍归为未知。
		assertEquals(CreativeItemGroups.CustomTabWarning.UNKNOWN,
			CreativeItemGroups.customTabWarning("中文:combat"));
		assertEquals(CreativeItemGroups.CustomTabWarning.UNKNOWN,
			CreativeItemGroups.customTabWarning("minecraft:" + "a".repeat(300)));
	}

	// 告警去重：每个标签页只真正告警一次；不需要告警的 tabId 不占用名额；clear 后复位。
	@Test
	void customTabWarningsOnlyFireOncePerSession() {
		assertTrue(CreativeItemGroups.warnUnsupportedCustomTab(":search"));
		assertFalse(CreativeItemGroups.warnUnsupportedCustomTab("minecraft:search"),
			"不同写法归一化后应合并去重");
		assertTrue(CreativeItemGroups.isCustomTabWarnedForTest(":search"));
		assertTrue(CreativeItemGroups.isCustomTabWarnedForTest("minecraft:search"));

		assertFalse(CreativeItemGroups.warnUnsupportedCustomTab("minecraft:building_blocks"));
		assertFalse(CreativeItemGroups.isCustomTabWarnedForTest("minecraft:building_blocks"),
			"受支持的标签页不应占用去重名额");

		assertTrue(CreativeItemGroups.warnUnsupportedCustomTab("somemod:missing"));
		assertFalse(CreativeItemGroups.warnUnsupportedCustomTab("somemod:missing"));

		CreativeItemGroups.clear();
		assertFalse(CreativeItemGroups.isCustomTabWarnedForTest("minecraft:search"));
		assertTrue(CreativeItemGroups.warnUnsupportedCustomTab("minecraft:search"),
			"clear 后应可重新告警");
	}

	// 告警副作用：beginTab 与 rebuildGroups 都会检查，功能关闭时跳过。
	@Test
	void warningsFireFromBeginTabAndRebuildGroupsOnlyWhenEnabled() {
		TweakifyMod.CONFIG.setStringList(TweakifyModConfig.CUSTOM_ITEM_GROUPS_FIELD,
			List.of("minecraft:search|搜索|stone"));

		// beginTab：切页时检查（与当前页类型无关）。
		CreativeItemGroups.beginTab(BuiltInRegistries.CREATIVE_MODE_TAB.getValue(
			CreativeModeTabs.BUILDING_BLOCKS));
		assertTrue(CreativeItemGroups.isCustomTabWarnedForTest("minecraft:search"),
			"beginTab 必须检查自定义组");

		// rebuildGroups：关闭功能时 beginTab 不检查，恢复后列表重建必须检查。
		CreativeItemGroups.clear();
		TweakifyMod.CONFIG.functions.itemGroups = false;
		CreativeItemGroups.beginTab(BuiltInRegistries.CREATIVE_MODE_TAB.getValue(
			CreativeModeTabs.SEARCH));
		assertFalse(CreativeItemGroups.isCustomTabWarnedForTest("minecraft:search"),
			"功能关闭时不应检查");

		TweakifyMod.CONFIG.functions.itemGroups = true;
		CreativeItemGroups.stripMembers(
			List.of(stack(Items.STONE), stack(Items.STONE_BRICKS), stack(Items.SMOOTH_STONE)));
		assertTrue(CreativeItemGroups.isCustomTabWarnedForTest("minecraft:search"),
			"rebuildGroups 必须检查自定义组");
	}

	// clear() 必须同时清空组表与渲染快照，避免“图标在但点不动”。
	@Test
	void clearClearsGroupsAndRenderState() {
		ItemStack dirt = stack(Items.DIRT);
		ItemStack oak = stack(Items.OAK_STAIRS);
		ItemStack stone = stack(Items.STONE_STAIRS);
		ItemStack spruce = stack(Items.SPRUCE_STAIRS);
		CreativeItemGroups.Group group = new CreativeItemGroups.Group(
			Component.literal("Stairs"), List.of(oak, stone, spruce));
		CreativeItemGroups.installGroupsForTest(List.of(group), List.of(dirt, oak));
		assertTrue(CreativeItemGroups.groupCountForTest() > 0);
		assertNotNull(CreativeItemGroups.iconGroup(1));

		CreativeItemGroups.clear();

		assertEquals(0, CreativeItemGroups.groupCountForTest(), "clear 后组表必须清空");
		assertNull(CreativeItemGroups.iconGroup(1), "clear 后渲染快照必须清空");
	}

	// 视觉空白行（空串/制表符/全角/NBSP/零宽/BOM）都视为未配置，不得刷格式错误告警。
	@Test
	void blankAndInvisibleRowsAreNotWarned() {
		List<String> blankRows = List.of(
			"", "   ", "\t", "\u3000", "\u00A0", "\u2007", "\u202F", "\u200B", "\uFEFF");
		TweakifyMod.CONFIG.setStringList(TweakifyModConfig.CUSTOM_ITEM_GROUPS_FIELD, blankRows);
		CreativeItemGroups.warnUnsupportedCustomGroups();
		for (String row : blankRows) {
			assertFalse(CreativeItemGroups.isInvalidRowWarnedForTest(row),
				"空白/不可见行不得告警");
		}
	}

	// 格式错误行（字段不足/空 tabId/空包含项）必须留下一次告警，合法行与空白行不得误报。
	@Test
	void malformedCustomRowsWarnOnce() {
		String shortRow = "bad row";
		String emptyTab = "|没有标签页|stairs";
		String emptyContained = "minecraft:building_blocks|名|,,";
		String validRow = "minecraft:building_blocks|名|stone";
		String blankRow = "   ";
		TweakifyMod.CONFIG.setStringList(TweakifyModConfig.CUSTOM_ITEM_GROUPS_FIELD,
			List.of(shortRow, emptyTab, emptyContained, validRow, blankRow));

		CreativeItemGroups.warnUnsupportedCustomGroups();
		assertTrue(CreativeItemGroups.isInvalidRowWarnedForTest(shortRow));
		assertTrue(CreativeItemGroups.isInvalidRowWarnedForTest(emptyTab));
		assertTrue(CreativeItemGroups.isInvalidRowWarnedForTest(emptyContained));
		assertFalse(CreativeItemGroups.isInvalidRowWarnedForTest(validRow), "合法行不得告警");
		assertFalse(CreativeItemGroups.isInvalidRowWarnedForTest(blankRow), "整行空白视为未配置");
		assertTrue(CreativeItemGroups.isInvalidRowWarnedForTest("  " + shortRow.toUpperCase()),
			"大小写/空白变体共用去重键");

		CreativeItemGroups.clear();
		assertFalse(CreativeItemGroups.isInvalidRowWarnedForTest(shortRow), "clear 后应复位");
	}

	// 配置变化决策：分类页要求重建列表；非分类页不重建但必须检查自定义组。
	@Test
	void onConfigChangedRefreshesOnlyCategoryTabs() {
		ItemStack oakStairs = stack(Items.OAK_STAIRS);
		ItemStack stoneStairs = stack(Items.STONE_STAIRS);
		ItemStack spruceStairs = stack(Items.SPRUCE_STAIRS);
		TweakifyMod.CONFIG.setStringList(TweakifyModConfig.CUSTOM_ITEM_GROUPS_FIELD,
			List.of("minecraft:search|搜索|stone"));

		CreativeModeTab search = BuiltInRegistries.CREATIVE_MODE_TAB.getValue(CreativeModeTabs.SEARCH);
		assertNotNull(search);
		assertFalse(CreativeItemGroups.onConfigChanged(search), "非分类页不重建列表");
		assertTrue(CreativeItemGroups.isCustomTabWarnedForTest("minecraft:search"),
			"非分类页配置变化必须检查自定义组");

		CreativeItemGroups.clear();
		CreativeModeTab building = BuiltInRegistries.CREATIVE_MODE_TAB.getValue(
			CreativeModeTabs.BUILDING_BLOCKS);
		assertNotNull(building);
		assertTrue(CreativeItemGroups.onConfigChanged(building), "分类页需要重建列表");
		assertTrue(CreativeItemGroups.isCustomTabWarnedForTest("minecraft:search"));

		// 前置：分类页建立默认楼梯组。
		CreativeItemGroups.stripMembers(List.of(oakStairs, stoneStairs, spruceStairs));
		assertNotNull(CreativeItemGroups.iconGroup(0), "前置：分类页已成立楼梯组");

		// 非分类页只告警，不得清空既有组状态。
		assertFalse(CreativeItemGroups.onConfigChanged(search), "非分类页不重建列表");
		assertNotNull(CreativeItemGroups.iconGroup(0), "非分类页不得清空既有组状态");
		assertTrue(CreativeItemGroups.groupCountForTest() > 0, "非分类页不得清空组表");

		assertFalse(CreativeItemGroups.onConfigChanged(null));
	}

	// 转发接缝：与 onConfigChanged 的返回值完全一致（null/非分类页 false，分类页 true）。
	@Test
	void shouldRefreshAfterConfigChangeMatchesOnConfigChanged() {
		TweakifyMod.CONFIG.setStringList(TweakifyModConfig.CUSTOM_ITEM_GROUPS_FIELD, List.of());
		CreativeModeTab search = BuiltInRegistries.CREATIVE_MODE_TAB.getValue(CreativeModeTabs.SEARCH);
		CreativeModeTab building = BuiltInRegistries.CREATIVE_MODE_TAB.getValue(
			CreativeModeTabs.BUILDING_BLOCKS);
		assertNotNull(search);
		assertNotNull(building);

		assertFalse(CreativeItemGroups.shouldRefreshAfterConfigChange(null));
		assertFalse(CreativeItemGroups.shouldRefreshAfterConfigChange(search),
			"非分类页不重建列表");
		assertTrue(CreativeItemGroups.shouldRefreshAfterConfigChange(building),
			"分类页需要重建列表");

		CreativeItemGroups.clear();
	}

	// 轮播只用于展开的组：收起时图标固定为首个成员，关闭轮播时也不会返回替换图标。
	@Test
	void collapsedIconDoesNotRotate() {
		ItemStack oak = stack(Items.OAK_STAIRS);
		ItemStack stone = stack(Items.STONE_STAIRS);
		ItemStack spruce = stack(Items.SPRUCE_STAIRS);
		CreativeItemGroups.Group group = new CreativeItemGroups.Group(
			Component.literal("Stairs"), List.of(oak, stone, spruce));
		TweakifyMod.CONFIG.functions.itemGroupsShowItems = true;

		assertNull(CreativeItemGroups.rotatingIcon(null, 0L), "空组不轮播");
		assertNull(CreativeItemGroups.rotatingIcon(group, 0L), "收起的组不轮播");

		group.setVisible(true);
		assertSame(oak, CreativeItemGroups.rotatingIcon(group, 0L), "0ms 显示首个成员");
		assertSame(stone, CreativeItemGroups.rotatingIcon(group, 1500L), "1.5s 轮播到第二个成员");
		assertSame(spruce, CreativeItemGroups.rotatingIcon(group, 3000L), "3s 轮播到第三个成员");
		assertSame(oak, CreativeItemGroups.rotatingIcon(group, 4500L), "4.5s 轮播回首个成员");

		CreativeItemGroups.Group empty = new CreativeItemGroups.Group(Component.literal("Empty"), List.of());
		empty.setVisible(true);
		assertNull(CreativeItemGroups.rotatingIcon(empty, 0L), "空成员的组不轮播");

		TweakifyMod.CONFIG.functions.itemGroupsShowItems = false;
		assertNull(CreativeItemGroups.rotatingIcon(group, 0L), "关闭显示组内物品后不轮播");
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

	// 每个用例从一份全新配置出发，避免逐项切换时互相干扰。
	private static String signatureAfter(Consumer<TweakifyModConfig> mutation) {
		TweakifyModConfig config = new TweakifyModConfig();
		mutation.accept(config);
		return CreativeItemGroups.configSignature(config);
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
