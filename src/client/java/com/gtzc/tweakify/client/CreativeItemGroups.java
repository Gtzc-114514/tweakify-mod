package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * 创造模式物品分组：与 {@code BizCub/inventory-item-groups} 的行为保持一致。
 *
 * <p>要点（逐条对齐参考实现）：
 * <ul>
 *   <li>组由“标签页 ID + 包含/排除/等价 ID 模式”定义，组内物品从
 *       {@code CreativeModeTab.getDisplayItems()} 里按 ID 片段匹配；</li>
 *   <li>同一标签页内按定义顺序先到先得，后面组不再收录已被前面组占用的物品；</li>
 *   <li>成员少于 3 个的组不成立，物品保持原样散落；</li>
 *   <li>组图标是组内第一个物品，点击图标展开/收起，展开的成员紧跟在图标之后；</li>
 *   <li>展开时把成员插入 {@code ItemPickerMenu.items}，收起时从同一位置移除；</li>
 *   <li>展开状态下的“是否显示组内物品”会让图标按 1.5 秒轮播组内物品。</li>
 * </ul>
 *
 * <p>索引算法照搬参考实现：维护一份“成员全部被摘除”的基准列表，
 * 槽位索引先映射回基准列表再加偏移，从而在展开/收起后仍能定位到同一个组。
 *
 * <p>性能：渲染路径每帧要对 45 个槽位各查一次“该画什么”，所以把
 * “显示索引 → 图标/成员/组”预先算成数组，并把“槽位 → 显示索引”的偏移
 * 按物品版本缓存；两者只在列表真正变化时重建，每帧只剩一次数组下标访问。
 *
 * <p>缓存失效契约：组表、基准列表与渲染快照只在
 * {@link #beginTab}、{@link #stripMembers}、{@link #toggle}、{@link #clear} 中更新；
 * 槽位索引偏移额外跟随前两个槽位的物品变化（列表滚动）重算。
 * 这些生产路径覆盖了原版对 {@code ItemPickerMenu.items} 的全部改写：
 * 切页（{@code selectTab} 内的 {@code addAll}）、标签内容刷新
 * （{@code tryRefreshInvalidatedTabs → refreshCurrentTabContents}，它会脱离
 * {@code selectTab} 单独触发）、以及本功能的展开/收起。因此渲染与点击判定
 * 不会读到陈旧索引；万一某条未知路径直接改了列表，{@code setIndexes} 的
 * “图标不存在”兜底也会把组置为不可渲染，而不是留下错位高亮。
 *
 * <p>线程约束：所有状态只在客户端主线程（界面事件 / 渲染）读写。
 */
public final class CreativeItemGroups {
	// 与参考实现一致：成员少于 3 个的组不折叠。
	public static final int MIN_GROUP_SIZE = 3;
	// 创造模式物品区的槽位上限（0 ~ 44）。
	public static final int CREATIVE_SLOT_LIMIT = 44;
	// 组名翻译键前缀与参考实现一致，便于直接沿用已有的资源包翻译。
	private static final String GROUP_NAME_PREFIX = "group_name.inventory_item_groups.";
	// 图标轮播间隔：参考实现为 1.5 秒。
	private static final long ICON_ROTATE_MILLIS = 1500L;
	// 未配置排除项时的哨兵串：使「任一排除项都不包含」恒为真（参考实现的写法）。
	private static final String NO_EXCLUSION = "1111111";

	// 自定义组的字段分隔符：tabId|组名|包含|排除|等价。
	private static final String CUSTOM_FIELD_SEPARATOR = "\\|";
	private static final String CUSTOM_ITEM_SEPARATOR = ",";

	public static final String SORT_ALPHABETICAL = TweakifyModConfig.ITEM_GROUP_SORT_ALPHABETICAL;

	private static final boolean[] EMPTY_FLAGS = new boolean[0];
	private static final Group[] EMPTY_GROUPS = new Group[0];

	private static final List<GroupDefinition> DEFAULT_GROUPS = buildDefaultGroups();

	// 当前标签页的组、以及“成员被摘除后”的基准列表（参考实现的 groups / tempItemStacks）。
	private static final List<Group> GROUPS = new ArrayList<>();
	private static List<ItemStack> baseItems = new ArrayList<>();

	// 渲染期派生数据：把“显示索引 → 该画什么”预先算好，每帧只剩一次数组查表。
	// 只在物品列表变化（切页 / 展开 / 收起 / 内容刷新）时重建，渲染路径不再扫描组与成员。
	// 图标槽位只用一张 Group[] 当唯一真相：绘制与点击读同一个对象，不会出现判定不一致。
	private static boolean[] memberSlots = EMPTY_FLAGS;
	private static Group[] groupByIconSlot = EMPTY_GROUPS;

	// 当前标签页：切页时记录，物品列表重建时用它重建组定义。
	private static CreativeModeTab currentTab;

	// 槽位 → 显示索引的偏移：只取决于前两个槽位与基准列表，物品变化时一次失效。
	// 创造模式列表的滚动只改写槽位内容、不替换 slots 列表，因此除引用外还要比较前两个槽位的物品，
	// 否则滚动后仍用第一页的偏移，图标/成员标记与点击判定会跟着错位。
	private static int displayIndexOffset;
	private static boolean displayIndexValid;
	private static List<Slot> displayIndexSlots;
	private static ItemStack displayIndexAnchor0 = ItemStack.EMPTY;
	private static ItemStack displayIndexAnchor1 = ItemStack.EMPTY;

	private CreativeItemGroups() {
	}

	/**
	 * 一个组的定义：命中任一 {@code containedItems} 片段、且不被全部排除项同时命中，
	 * 或 ID 与 {@code equivalentItems} 完全相等时，物品属于该组。
	 */
	public record GroupDefinition(String name, String tabId, List<String> containedItems,
								  List<String> nonContainedItems, List<String> equivalentItems) {
		public GroupDefinition {
			// 自定义组是自由文本：统一归一化标签页 ID，避免缺命名空间/大小写不同时静默失效。
			tabId = normalizeTabId(tabId);
			containedItems = List.copyOf(containedItems);
			nonContainedItems = List.copyOf(nonContainedItems);
			equivalentItems = List.copyOf(equivalentItems);
		}

		public boolean matches(String itemId) {
			boolean contained = false;
			for (String pattern : this.containedItems) {
				if (itemId.contains(pattern)) {
					contained = true;
					break;
				}
			}
			// 参考实现：排除项用 anyMatch(!contains)，空列表时用哨兵串让它恒为真。
			boolean notExcluded = false;
			if (this.nonContainedItems.isEmpty()) {
				notExcluded = !itemId.contains(NO_EXCLUSION);
			} else {
				for (String pattern : this.nonContainedItems) {
					if (!itemId.contains(pattern)) {
						notExcluded = true;
						break;
					}
				}
			}
			return (contained && notExcluded) || this.equivalentItems.contains(itemId);
		}
	}

	/** 组内物品及其在基准列表中的当前位置；-1 表示当前不可见（已收起）。 */
	public static final class IndexedItemStack {
		private final ItemStack stack;
		private int index;

		IndexedItemStack(ItemStack stack, int index) {
			this.stack = stack;
			this.index = index;
		}

		public ItemStack stack() {
			return this.stack;
		}

		public int index() {
			return this.index;
		}

		public void setIndex(int index) {
			this.index = index;
		}
	}

	/** 一个已成立的组：图标、展开状态与成员索引。 */
	public static final class Group {
		private final Component name;
		private final List<ItemStack> items;
		private final List<IndexedItemStack> members = new ArrayList<>();
		private boolean visible;
		private int iconIndex = -1;

		Group(Component name, List<ItemStack> items) {
			this.name = name;
			this.items = List.copyOf(items);
			for (ItemStack stack : items) {
				this.members.add(new IndexedItemStack(stack, -1));
			}
		}

		public Component name() {
			return this.name;
		}

		public List<ItemStack> items() {
			return this.items;
		}

		public List<IndexedItemStack> members() {
			return this.members;
		}

		public ItemStack icon() {
			return this.items.getFirst();
		}

		public int iconIndex() {
			return this.iconIndex;
		}

		public void setIconIndex(int iconIndex) {
			this.iconIndex = iconIndex;
		}

		public boolean visible() {
			return this.visible;
		}

		public void setVisible(boolean visible) {
			this.visible = visible;
		}

		// 与参考实现一致：按物品相等性批量改写成员索引。
		public void setItemWithIndex(ItemStack item, int index) {
			for (IndexedItemStack entry : this.members) {
				if (entry.stack().equals(item)) {
					entry.setIndex(index);
				}
			}
		}
	}

	// ---------------------------------------------------------------- 组构建

	// 内置默认组按标签页过滤；包私有供单测与语言文件键校验使用。
	static List<GroupDefinition> definitionsFor(String tabId) {
		String normalized = normalizeTabId(tabId);
		List<GroupDefinition> definitions = new ArrayList<>();
		for (GroupDefinition definition : DEFAULT_GROUPS) {
			if (definition.tabId().equals(normalized)) {
				definitions.add(definition);
			}
		}
		return definitions;
	}

	// 面板注入点：切换标签页时只重置状态，真正的成组在随后的列表重建里完成。
	// 这样 selectTab 与“标签内容刷新”两条路径共用同一个入口，不会出现“列表换了、组还是旧的”。
	// 先检查自定义组引用的标签页：搜索/背包/快捷栏页也要给出配置无效的反馈。
	public static void beginTab(CreativeModeTab tab) {
		currentTab = tab;
		GROUPS.clear();
		baseItems = new ArrayList<>();
		warnUnsupportedCustomGroups();
		resetRenderState();
	}

	/**
	 * 纯构建：按定义顺序成组，先到先得地剔除重复物品，再丢掉成员不足 3 个的组。
	 * 包私有以便单测直接覆盖算法。
	 */
	static List<Group> create(List<GroupDefinition> definitions, List<ItemStack> displayItems) {
		return create(definitions, displayItems, isAlphabetical());
	}

	static List<Group> create(List<GroupDefinition> definitions, List<ItemStack> displayItems,
						  boolean alphabetical) {
		List<Group> created = new ArrayList<>();
		List<ItemStack> claimed = new ArrayList<>();
		for (GroupDefinition definition : definitions) {
			List<ItemStack> matched = new ArrayList<>();
			for (ItemStack stack : displayItems) {
				if (stack == null || stack.isEmpty()) {
					continue;
				}
				if (definition.matches(itemId(stack.getItem()))) {
					matched.add(stack);
				}
			}
			// 参考实现：已被前面组占用的物品不再重复入组。
			matched.removeAll(claimed);
			sortItems(matched, alphabetical);
			if (matched.isEmpty()) {
				continue;
			}
			claimed.addAll(matched);
			created.add(new Group(groupName(definition.name()), matched));
		}
		created.removeIf(group -> group.items().size() < MIN_GROUP_SIZE);
		return created;
	}

	// 组内排序：默认保持物品栏原顺序，字母序按物品 ID 排序。
	private static void sortItems(List<ItemStack> items, boolean alphabetical) {
		if (alphabetical) {
			items.sort((left, right) -> itemId(left.getItem()).compareTo(itemId(right.getItem())));
		}
	}

	// 面板注入点：每一次展示列表重建（切页 / 标签内容刷新 / 配置变更）都从这里过。
	// 先按当前列表重新成组，再摘除成员：旧组可能指向已经不存在的物品，直接复用会留下错位的图标/高亮。
	public static List<ItemStack> stripMembers(Collection<ItemStack> source) {
		List<ItemStack> stripped = new ArrayList<>(source);
		rebuildGroups(stripped);
		if (GROUPS.isEmpty()) {
			baseItems = stripped;
			resetRenderState();
			return stripped;
		}
		// 用哈希集合一次扫过：与 removeAll 语义相同（删除所有同值元素），
		// 但避免 m 个成员 × n 个物品的两两 equals。
		Set<ItemStack> removed = new HashSet<>();
		for (Group group : GROUPS) {
			removed.addAll(group.items());
			removed.remove(group.icon());
		}
		if (!removed.isEmpty()) {
			stripped.removeIf(removed::contains);
		}
		baseItems = stripped;
		setIndexes();
		return stripped;
	}

	// 按当前标签页 + 当前配置从给定列表重建组；无标签页或功能关闭时得到空组表。
	private static void rebuildGroups(List<ItemStack> displayItems) {
		GROUPS.clear();
		if (currentTab == null || !isEnabled()) {
			return;
		}
		// 先检查全部自定义组引用：无效/非分类页只告警一次。
		// 这段在类型门禁之前，因此搜索/背包页也能得到反馈。
		warnUnsupportedCustomGroups();
		// 分组只作用于分类页：快捷栏/背包页的物品来自原版其他数据源，搜索页由搜索框维护。
		if (currentTab.getType() != CreativeModeTab.Type.CATEGORY) {
			return;
		}
		String currentTabId = tabId(currentTab);
		List<GroupDefinition> definitions = new ArrayList<>();
		if (includeDefaultGroups()) {
			definitions.addAll(definitionsFor(currentTabId));
		}
		// 自定义组同样只在所属标签页生效：否则同一模式（如 log、sword）会在其他页误成组。
		for (GroupDefinition definition : parseCustomGroups(customGroupEntries())) {
			if (currentTabId.equals(definition.tabId())) {
				definitions.add(definition);
			}
		}
		GROUPS.addAll(create(definitions, displayItems));
	}

	// 告警类型：未注册/语法非法，或已注册但不是分类页（搜索/快捷栏/背包）。
	enum CustomTabWarning {
		UNKNOWN,
		NON_CATEGORY
	}

	// 包私有纯谓词：先归一化再分类，保证空白/大小写/空命名空间与分组匹配使用同一规则。
	static CustomTabWarning customTabWarning(String tabId) {
		return customTabWarning(tabId, CreativeItemGroups::lookupCreativeTab);
	}

	// 包私有接缝：测试可注入查找函数，验证非 minecraft 命名空间的已注册分类页，
	// 不必向全局注册表注册测试 tab（注册表在 Bootstrap 后已冻结，且注册会永久污染全局状态）。
	static CustomTabWarning customTabWarning(String tabId, Function<Identifier, CreativeModeTab> lookup) {
		String normalized = normalizeTabId(tabId);
		if (normalized.isEmpty()) {
			return null;
		}
		Identifier id = Identifier.tryParse(normalized);
		CreativeModeTab tab;
		try {
			tab = id == null || lookup == null ? null : lookup.apply(id);
		} catch (RuntimeException e) {
			// 契约：仅兜底 RuntimeException，Error（含 OOM/栈溢出）继续向上传播。
			LOOKUP_FAILURES.incrementAndGet();
			TweakifyMod.LOGGER.debug("Creative tab lookup failed for {}, treating as unknown", normalized, e);
			return CustomTabWarning.UNKNOWN;
		}
		if (tab == null) {
			return CustomTabWarning.UNKNOWN;
		}
		return tab.getType() == CreativeModeTab.Type.CATEGORY ? null : CustomTabWarning.NON_CATEGORY;
	}

	private static CreativeModeTab lookupCreativeTab(Identifier id) {
		return BuiltInRegistries.CREATIVE_MODE_TAB.get(id).map(holder -> holder.value()).orElse(null);
	}

	// 包私有：输出告警并返回是否真的写了一条日志；同一标签页每个屏幕会话只提示一次。
	// 去重键使用归一化后的 ID，":search" 与 "minecraft:search" 共享同一条告警。
	static boolean warnUnsupportedCustomTab(String tabId) {
		String normalized = normalizeTabId(tabId);
		CustomTabWarning warning = customTabWarning(normalized);
		if (warning == null || !WARNED_CUSTOM_TABS.add(normalized)) {
			return false;
		}
		if (warning == CustomTabWarning.UNKNOWN) {
			TweakifyMod.LOGGER.warn("Ignoring custom item group for unknown creative tab: {}", normalized);
		} else {
			TweakifyMod.LOGGER.warn(
				"Ignoring custom item group for non-category tab (only category pages are grouped): {}", normalized);
		}
		return true;
	}

	// 包私有：测试断言“不需要告警的标签页不占用去重名额”。
	static boolean isCustomTabWarnedForTest(String tabId) {
		return WARNED_CUSTOM_TABS.contains(normalizeTabId(tabId));
	}

	// 检查配置中的全部自定义组（不受当前页类型限制）：切页与列表重建都调用。
	// 同时报告格式错误行（字段不足/空 tabId/空包含项），避免“配了却无效”的完全静默。
	public static void warnUnsupportedCustomGroups() {
		if (!isEnabled()) {
			return;
		}
		List<String> entries = customGroupEntries();
		warnInvalidCustomRows(entries);
		for (GroupDefinition definition : parseCustomGroups(entries)) {
			warnUnsupportedCustomTab(definition.tabId());
		}
	}

	// 已告警过的格式错误行：避免每次列表重建都重复写日志。
	private static final Set<String> WARNED_INVALID_ROWS = new HashSet<>();

	private static void warnInvalidCustomRows(List<String> entries) {
		for (String entry : entries) {
			if (isBlankRow(entry)) {
				// 整行空白/不可见字符视为未配置。
				continue;
			}
			String[] parts = entry.split(CUSTOM_FIELD_SEPARATOR, -1);
			String tabId = parts.length > 0 ? parts[0].trim() : "";
			List<String> contained = parts.length > 2 ? splitItems(parts[2]) : List.of();
			boolean invalid = parts.length < 3 || tabId.isEmpty() || contained.isEmpty();
			if (invalid && WARNED_INVALID_ROWS.add(invalidRowKey(entry))) {
				TweakifyMod.LOGGER.warn("Ignoring malformed custom item group row: {}", entry);
			}
		}
	}

	// 整行空白/不可见字符判定：isBlank() 不识别 NBSP/零宽/BOM，这里显式覆盖。
	private static boolean isBlankRow(String entry) {
		if (entry == null) {
			return true;
		}
		return entry.codePoints().allMatch(cp ->
			Character.isWhitespace(cp) || Character.isSpaceChar(cp)
				|| cp == 0x200B || cp == 0xFEFF);
	}

	// 格式错误行的去重键：整行视觉空白已由 isBlankRow 跳过；这里只做 strip + 小写，
	// 非空白行内部的 NBSP/零宽变体不参与归一化（仅影响重复提示次数，不影响分组）。
	private static String invalidRowKey(String entry) {
		return entry == null ? "" : entry.strip().toLowerCase(Locale.ROOT);
	}

	// 包私有：测试断言格式错误行只告警一次。
	static boolean isInvalidRowWarnedForTest(String entry) {
		return WARNED_INVALID_ROWS.contains(invalidRowKey(entry));
	}

	// 包私有：测试断言异常降级确实发生（日志无法直接断言时的可观测计数）。
	static int lookupFailureCountForTest() {
		return LOOKUP_FAILURES.get();
	}

	// 包私有：测试断言归一化后的两行只登记一次（集合大小不增）。
	static int warnedTabCountForTest() {
		return WARNED_CUSTOM_TABS.size();
	}

	// 已告警过的标签页：避免每次列表重建都重复写日志（状态只在客户端主线程读写）。
	private static final Set<String> WARNED_CUSTOM_TABS = new HashSet<>();

	// 查找异常降级次数：纯诊断计数，不参与分组判定。
	private static final AtomicInteger LOOKUP_FAILURES = new AtomicInteger();

	// 配置变化时只重建分类页的物品列表：快捷栏/背包页的物品来自原版其他数据源，
	// 用 getDisplayItems 重填会清空预览；搜索页结果由搜索框维护，也不应在配置变化时重建。
	public static boolean needsContentRefreshOnConfigChange(CreativeModeTab.Type type) {
		return type == CreativeModeTab.Type.CATEGORY;
	}

	// 配置变化决策：分类页由调用方重建列表（beginTab 已清空组状态并在重建时重新成组）；
	// 非分类页不重建列表，但仍检查自定义组引用，避免搜索/背包页的无效配置静默无反馈。
	// 返回值：true 表示调用方需要重建当前页的物品列表。
	public static boolean onConfigChanged(CreativeModeTab tab) {
		if (tab == null) {
			return false;
		}
		if (!needsContentRefreshOnConfigChange(tab.getType())) {
			warnUnsupportedCustomGroups();
			return false;
		}
		beginTab(tab);
		return true;
	}

	// Mixin 转发接缝：配置变化后是否需要重建当前页。
	// 把「非空判断 + onConfigChanged」收敛到这里，Mixin 只保留签名比对与调用；
	// 单测覆盖 null / 分类页 / 非分类页三种返回值，源码契约测试锁定 Mixin 仍调用本接缝。
	public static boolean shouldRefreshAfterConfigChange(CreativeModeTab tab) {
		return tab != null && onConfigChanged(tab);
	}

	// 参考实现的 setIndexes：收起时图标取基准列表里的首个位置、成员索引置 -1；
	// 展开时图标取首个位置，同一物品的后续位置留给成员（用于重复物品的补位）。
	private static void setIndexes() {
		for (Group group : GROUPS) {
			// 图标已经不在基准列表里（列表被刷新过）：清空索引，避免残留图标与高亮。
			// 正常情况下 rebuildGroups 已保证图标存在，这里只是兜底。
			if (!baseItems.contains(group.icon())) {
				group.setIconIndex(-1);
				for (IndexedItemStack entry : group.members()) {
					entry.setIndex(-1);
				}
				continue;
			}
			boolean iconAssigned = false;
			for (ItemStack stack : group.items()) {
				int firstIndex = baseItems.indexOf(stack);
				int lastIndex = baseItems.lastIndexOf(stack);
				if (firstIndex < 0) {
					continue;
				}
				if (group.visible()) {
					if (firstIndex != lastIndex && !iconAssigned) {
						group.setIconIndex(firstIndex);
						group.setItemWithIndex(stack, lastIndex);
						iconAssigned = true;
					} else {
						group.setItemWithIndex(stack, firstIndex);
					}
				} else {
					group.setIconIndex(firstIndex);
					for (IndexedItemStack entry : group.members()) {
						entry.setIndex(-1);
					}
				}
			}
		}
		rebuildRenderState();
		displayIndexValid = false;
	}

	// ---------------------------------------------------------------- 交互

	public static boolean isCreativeSlot(int slotIndex) {
		return slotIndex >= 0 && slotIndex <= CREATIVE_SLOT_LIMIT;
	}

	// 槽位 → 显示索引：渲染与点击判定共用。
	// 偏移只取决于前两个槽位与基准列表：除了 slots 列表引用，还要看前两个槽位的物品，
	// 因为滚动只更新槽位内容。物品相同（引用）时偏移必然相同，可以安全复用。
	public static int displayIndex(List<Slot> slots, int slotIndex) {
		if (slots.isEmpty()) {
			return -1;
		}
		ItemStack anchor0 = itemAt(slots, 0);
		ItemStack anchor1 = itemAt(slots, 1);
		if (!displayIndexValid || displayIndexSlots != slots
			|| displayIndexAnchor0 != anchor0 || displayIndexAnchor1 != anchor1) {
			displayIndexSlots = slots;
			displayIndexOffset = offsetFor(slots, baseItems);
			displayIndexAnchor0 = anchor0;
			displayIndexAnchor1 = anchor1;
			displayIndexValid = true;
		}
		return displayIndexOffset + slotIndex;
	}

	private static ItemStack itemAt(List<Slot> slots, int index) {
		return index < slots.size() ? slots.get(index).getItem() : ItemStack.EMPTY;
	}

	// 参考实现用前两个槽位的物品反推基准偏移；取不到时归零。
	private static int offsetFor(List<Slot> slots, List<ItemStack> base) {
		int secondItemIndex = slots.size() >= 2 ? base.indexOf(slots.get(1).getItem()) : -1;
		int result;
		if (secondItemIndex >= 0) {
			result = secondItemIndex;
			if (!slots.get(0).getItem().equals(slots.get(1).getItem())) {
				result--;
			}
		} else {
			result = base.indexOf(slots.get(0).getItem());
		}
		return Math.max(result, 0);
	}

	// 以下四个查询供渲染与点击判定使用：已是 O(1) 数组查表，边界外统一返回 false / null。
	// 展开/收起状态直接读图标所属的组，与点击时拿到的 Group 是同一个对象，不存在双份状态。
	public static boolean isExpandedIconSlot(int displayIndex) {
		Group group = groupAt(displayIndex);
		return group != null && group.visible();
	}

	public static boolean isCollapsedIconSlot(int displayIndex) {
		Group group = groupAt(displayIndex);
		return group != null && !group.visible();
	}

	public static boolean isMemberSlot(int displayIndex) {
		return flagAt(memberSlots, displayIndex);
	}

	public static Group iconGroup(int displayIndex) {
		return groupAt(displayIndex);
	}

	private static Group groupAt(int displayIndex) {
		return displayIndex >= 0 && displayIndex < groupByIconSlot.length
			? groupByIconSlot[displayIndex]
			: null;
	}

	private static boolean flagAt(boolean[] flags, int displayIndex) {
		return displayIndex >= 0 && displayIndex < flags.length && flags[displayIndex];
	}

	// 物品列表变化时重建派生数据；索引均来自 setIndexes，尺寸与基准列表一致。
	private static void rebuildRenderState() {
		int size = baseItems.size();
		memberSlots = new boolean[size];
		groupByIconSlot = new Group[size];
		for (Group group : GROUPS) {
			int icon = group.iconIndex();
			if (icon < 0 || icon >= size) {
				continue;
			}
			groupByIconSlot[icon] = group;
			if (!group.visible()) {
				continue;
			}
			for (IndexedItemStack entry : group.members()) {
				int index = entry.index();
				if (index >= 0 && index < size) {
					memberSlots[index] = true;
				}
			}
		}
	}

	// 清空派生数据：关闭界面 / 切页时调用，避免残留旧页的标记。
	private static void resetRenderState() {
		memberSlots = EMPTY_FLAGS;
		groupByIconSlot = EMPTY_GROUPS;
		displayIndexValid = false;
		displayIndexSlots = null;
		displayIndexAnchor0 = ItemStack.EMPTY;
		displayIndexAnchor1 = ItemStack.EMPTY;
	}

	// 点击组图标：展开时把成员插到图标之后，收起时从同一位置移除，再按当前列表重建索引。
	public static void toggle(Group group, NonNullList<ItemStack> items) {
		if (group == null) {
			return;
		}
		group.setVisible(!group.visible());
		int insertIndex = Math.max(0, Math.min(items.size(), group.iconIndex() + 1));
		if (group.visible()) {
			// 反向逐个插入同一位置，最终顺序与组内顺序一致（参考实现写法）。
			List<ItemStack> members = new ArrayList<>(group.items());
			Collections.reverse(members);
			for (ItemStack stack : members) {
				items.add(insertIndex, stack);
			}
		} else {
			for (int i = 0; i < group.items().size() && insertIndex < items.size(); i++) {
				items.remove(insertIndex);
			}
		}
		baseItems = new ArrayList<>(items);
		setIndexes();
	}

	// 组图标当前应显示的物品：收起的组固定显示首个成员，只有展开且开启“显示组内物品”时才轮播。
	public static ItemStack rotatingIcon(Group group) {
		return rotatingIcon(group, Util.getMillis());
	}

	// 重载：渲染传当前时间，单测注入固定时刻验证轮播顺序。
	static ItemStack rotatingIcon(Group group, long millis) {
		if (group == null || !group.visible() || !showItemsInGroup() || group.items().isEmpty()) {
			return null;
		}
		int index = (int) ((millis / ICON_ROTATE_MILLIS) % group.items().size());
		return group.items().get(index);
	}

	public static void clear() {
		currentTab = null;
		GROUPS.clear();
		baseItems = new ArrayList<>();
		WARNED_CUSTOM_TABS.clear();
		WARNED_INVALID_ROWS.clear();
		resetRenderState();
	}

	// 包私有：单测注入一组组定义与基准列表，跑完 setIndexes 后可直接断言渲染快照。
	static void installGroupsForTest(List<Group> groups, List<ItemStack> base) {
		GROUPS.clear();
		GROUPS.addAll(groups);
		baseItems = List.copyOf(base);
		setIndexes();
	}

	// 包私有：测试同时断言组表与渲染快照，防止只清一处造成“图标在但点不动”。
	static int groupCountForTest() {
		return GROUPS.size();
	}

	// ---------------------------------------------------------------- 配置

	private static boolean isEnabled() {
		TweakifyModConfig config = TweakifyMod.config();
		return config.functions.itemGroups;
	}

	private static boolean includeDefaultGroups() {
		TweakifyModConfig config = TweakifyMod.config();
		return config.functions.itemGroupsAddAll;
	}

	private static boolean showItemsInGroup() {
		TweakifyModConfig config = TweakifyMod.config();
		return config.functions.itemGroupsShowItems;
	}

	private static boolean isAlphabetical() {
		TweakifyModConfig config = TweakifyMod.config();
		return SORT_ALPHABETICAL.equals(config.functions.itemGroupsSort);
	}

	private static List<String> customGroupEntries() {
		TweakifyModConfig config = TweakifyMod.config();
		return config.getStringList(TweakifyModConfig.CUSTOM_ITEM_GROUPS_FIELD);
	}

	// 配置签名：只包含会影响分组结果的项。轮播开关只影响渲染，不参与签名，
	// 切换时不应重建列表，否则已展开的组会被收起。
	public static String configSignature(TweakifyModConfig config) {
		if (config == null) {
			return "";
		}
		return config.functions.itemGroups + "|"
			+ config.functions.itemGroupsSort + "|"
			+ config.functions.itemGroupsAddAll + "|"
			+ String.join("\n", config.getStringList(TweakifyModConfig.CUSTOM_ITEM_GROUPS_FIELD));
	}

	// 自定义组：tabId|组名|包含(逗号分隔)|排除|等价；至少需要前三个字段。
	static List<GroupDefinition> parseCustomGroups(List<String> entries) {
		List<GroupDefinition> definitions = new ArrayList<>();
		if (entries == null) {
			return definitions;
		}
		for (String entry : entries) {
			if (entry == null) {
				continue;
			}
			String[] parts = entry.split(CUSTOM_FIELD_SEPARATOR, -1);
			if (parts.length < 3) {
				continue;
			}
			String tabId = parts[0].trim();
			String name = parts[1].trim();
			List<String> contained = splitItems(parts[2]);
			if (tabId.isEmpty() || contained.isEmpty()) {
				continue;
			}
			List<String> excluded = parts.length > 3 ? splitItems(parts[3]) : List.of();
			List<String> equivalent = parts.length > 4 ? splitItems(parts[4]) : List.of();
			definitions.add(new GroupDefinition(name.isEmpty() ? tabId : name, tabId,
				contained, excluded, equivalent));
		}
		return definitions;
	}

	private static List<String> splitItems(String raw) {
		List<String> items = new ArrayList<>();
		if (raw == null) {
			return items;
		}
		for (String part : raw.split(CUSTOM_ITEM_SEPARATOR, -1)) {
			String value = part.trim();
			if (!value.isEmpty()) {
				items.add(value);
			}
		}
		return items;
	}

	// ---------------------------------------------------------------- 工具

	// 标签页 ID：优先用注册表键，取不到时退回显示名（参考实现同此策略）。
	// 两种来源都归一化，确保与自定义组使用同一套比较规则。
	private static String tabId(CreativeModeTab tab) {
		if (tab == null) {
			return "";
		}
		Identifier key = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
		return normalizeTabId(key != null ? key.toString() : tab.getDisplayName().getString());
	}

	// 自定义组 tabId 的统一归一化：去空白、转小写，再交给 Identifier 补全/校验。
	// Identifier 会把空命名空间补成 minecraft（":combat" → minecraft:combat），
	// 因此匹配与告警使用同一个字符串，不会出现“谓词认为受支持、匹配却失败”的静默无效。
	// 解析失败（多冒号、冒号后空格等）保留原串，由告警谓词判为未知。
	static String normalizeTabId(String raw) {
		String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
		if (value.isEmpty()) {
			return value;
		}
		Identifier id = Identifier.tryParse(value);
		return id != null ? id.toString() : value;
	}

	private static String itemId(Item item) {
		Identifier key = BuiltInRegistries.ITEM.getKey(item);
		return key == null ? "minecraft:air" : key.toString();
	}

	// 组名优先走翻译键，缺失时退回原始文本（与参考实现一致）。
	private static Component groupName(String name) {
		String key = GROUP_NAME_PREFIX + name;
		return Language.getInstance().has(key) ? Component.translatable(key) : Component.literal(name);
	}

	// ---------------------------------------------------------------- 默认组

	private static void addGroup(List<GroupDefinition> list, String tabId, String name,
								 List<String> contained) {
		addGroup(list, tabId, name, contained, List.of());
	}

	private static void addGroup(List<GroupDefinition> list, String tabId, String name,
								 List<String> contained, List<String> excluded) {
		list.add(new GroupDefinition(name, tabId, contained, excluded, List.of()));
	}

	private static List<String> ids(String... values) {
		return List.of(values);
	}

	// 默认组表与参考实现逐条对齐（标签页、名称、包含/排除模式完全一致）。
	private static List<GroupDefinition> buildDefaultGroups() {
		List<GroupDefinition> all = new ArrayList<>();

		String tabId = "minecraft:building_blocks";
		addGroup(all, tabId, "logs", ids("log", "stem", "bamboo_block"), ids("stripped"));
		addGroup(all, tabId, "woods", ids("wood", "hyphae"), ids("stripped"));
		addGroup(all, tabId, "stripped_logs", ids("log", "stem", "bamboo_block"));
		addGroup(all, tabId, "stripped_woods", ids("wood", "hyphae"));
		addGroup(all, tabId, "stairs", ids("stair"));
		addGroup(all, tabId, "slabs", ids("slab"));
		addGroup(all, tabId, "planks", ids("planks", "mosaic"));
		addGroup(all, tabId, "fence_gates", ids("fence_gate"));
		addGroup(all, tabId, "fences", ids("fence"));
		addGroup(all, tabId, "trapdoors", ids("trapdoor"));
		addGroup(all, tabId, "doors", ids("door"));
		addGroup(all, tabId, "pressure_plates", ids("pressure_plate"));
		addGroup(all, tabId, "buttons", ids("button"));
		addGroup(all, tabId, "bars", ids("bar"), ids("cinnabar"));
		addGroup(all, tabId, "chains", ids("chain"));
		addGroup(all, tabId, "copper", ids("copper"));
		addGroup(all, tabId, "walls", ids("wall"));
		addGroup(all, tabId, "decorative_stone", ids("bricks", "chiseled", "tiles", "polished"));
		addGroup(all, tabId, "sandstone", ids("sandstone"));

		tabId = "minecraft:colored_blocks";
		addGroup(all, tabId, "wool_stairs", ids("wool_stairs"));
		addGroup(all, tabId, "wool_slabs", ids("wool_slab"));
		addGroup(all, tabId, "wool", ids("wool"));
		addGroup(all, tabId, "carpets", ids("carpet"));
		addGroup(all, tabId, "glazed_terracotta", ids("glazed_terracotta"));
		addGroup(all, tabId, "terracotta", ids("terracotta"));
		addGroup(all, tabId, "concrete_powder", ids("concrete_powder"));
		addGroup(all, tabId, "concrete_stairs", ids("concrete_stairs"));
		addGroup(all, tabId, "concrete_slabs", ids("concrete_slab"));
		addGroup(all, tabId, "concrete", ids("concrete"));
		addGroup(all, tabId, "glass_panes", ids("glass_pane"));
		addGroup(all, tabId, "glass", ids("glass"));
		addGroup(all, tabId, "shulker_boxes", ids("shulker_box"));
		addGroup(all, tabId, "candles", ids("candle"));
		addGroup(all, tabId, "banners", ids("banner"));
		addGroup(all, tabId, "beds", ids("bed"));
		addGroup(all, tabId, "cushions", ids("_cushion"));

		tabId = "minecraft:natural_blocks";
		addGroup(all, tabId, "ores", ids("_ore", "debris", "raw_"));
		addGroup(all, tabId, "mushrooms", ids("mushroom", "fungus"));
		addGroup(all, tabId, "saplings", ids("sapling", "propagule"));
		addGroup(all, tabId, "ground_cover",
			ids("fern", "_grass", "bush", "_sprouts", "hanging_moss", "_vines", "shrub"), ids("_bush"));
		addGroup(all, tabId, "seeds", ids("seeds", "_pod"));
		addGroup(all, tabId, "flowers", ids(
			"dandelion", "poppy", "orchid", "allium", "tulip", "daisy", "cornflower", "torchflower",
			"azure_bluet", "valley", "cactus_flower", "eyeblossom", "rose", "petals", "wildflower",
			"crimson_roots", "warped_roots", "sunflower", "peony", "lilac", "pitcher_plant"));
		addGroup(all, tabId, "leaves", ids("leaves"));
		addGroup(all, tabId, "coral_blocks", ids("coral_block"));
		addGroup(all, tabId, "coral_decorations", ids("coral"));
		addGroup(all, tabId, "stone", ids(
			":stone", "diorite", "andesite", "granite", "tuff", "basalt", "blackstone", "deepslate"));
		addGroup(all, tabId, "logs", ids("log", "stem"));

		tabId = "minecraft:functional_blocks";
		addGroup(all, tabId, "lanterns", ids("lantern"), ids("sea"));
		addGroup(all, tabId, "chains", ids("chain"));
		addGroup(all, tabId, "bulbs", ids("bulb"));
		addGroup(all, tabId, "anvils", ids("anvil"));
		addGroup(all, tabId, "lightning_rods", ids("lightning_rod"));
		addGroup(all, tabId, "shelves", ids("_shelf"));
		addGroup(all, tabId, "hanging_signs", ids("hanging_sign"));
		addGroup(all, tabId, "signs", ids("sign"));
		addGroup(all, tabId, "chests", ids("chest"));
		addGroup(all, tabId, "shulker_boxes", ids("shulker_box"));
		addGroup(all, tabId, "beds", ids("_bed"));
		addGroup(all, tabId, "candles", ids("candle"));
		addGroup(all, tabId, "banners", ids("banner"));
		addGroup(all, tabId, "skulls", ids("head", "skull"));
		addGroup(all, tabId, "golem_statues", ids("golem_statue"));
		addGroup(all, tabId, "infested_stone", ids("infested"));
		addGroup(all, tabId, "paintings", ids("painting"));

		tabId = "minecraft:redstone_blocks";
		addGroup(all, tabId, "bulbs", ids("bulb"));
		addGroup(all, tabId, "pressure_plates", ids("pressure_plate"));
		addGroup(all, tabId, "transport", ids("minecart", "boat", "_raft"));
		addGroup(all, tabId, "chests", ids("chest"));
		addGroup(all, tabId, "rails", ids("rail"));

		tabId = "minecraft:tools_and_utilities";
		addGroup(all, tabId, "shovels", ids("shovel"));
		addGroup(all, tabId, "pickaxes", ids("pickaxe"));
		addGroup(all, tabId, "axes", ids("axe"));
		addGroup(all, tabId, "hoes", ids("hoe"));
		addGroup(all, tabId, "bundles", ids("bundle"));
		addGroup(all, tabId, "firework_rockets", ids("firework_rocket"));
		addGroup(all, tabId, "harnesses", ids("harness"));
		addGroup(all, tabId, "chest_boats", ids("chest_boat", "chest_raft"));
		addGroup(all, tabId, "boats", ids("boat", "_raft"));
		addGroup(all, tabId, "rails", ids("rail"));
		addGroup(all, tabId, "minecarts", ids("minecart"));
		addGroup(all, tabId, "discs", ids("disc"));
		addGroup(all, tabId, "goat_horns", ids("goat_horn"));
		addGroup(all, tabId, "creature_buckets", ids(
			"cod_bucket", "salmon_bucket", "tropical_fish_bucket", "pufferfish_bucket",
			"axolotl_bucket", "tadpole_bucket", "sulfur_cube_bucket"));

		tabId = "minecraft:combat";
		addGroup(all, tabId, "swords", ids("sword"));
		addGroup(all, tabId, "spears", ids("spear"));
		addGroup(all, tabId, "axes", ids("axe"));
		addGroup(all, tabId, "helmets", ids("helmet"));
		addGroup(all, tabId, "chestplates", ids("chestplate"));
		addGroup(all, tabId, "leggings", ids("leggings"));
		addGroup(all, tabId, "boots", ids("boots"));
		addGroup(all, tabId, "horse_armor", ids("horse_armor"));
		addGroup(all, tabId, "nautilus_armor", ids("nautilus_armor"));
		addGroup(all, tabId, "eggs", ids("egg"));
		addGroup(all, tabId, "tipped_arrows", ids("tipped_arrow"));
		addGroup(all, tabId, "firework_rockets", ids("firework_rocket"));

		tabId = "minecraft:food_and_drinks";
		addGroup(all, tabId, "suspicious_stews", ids("suspicious_stew"));
		addGroup(all, tabId, "ominous_bottles", ids("ominous_bottle"));
		addGroup(all, tabId, "splash_potions", ids("splash_potion"));
		addGroup(all, tabId, "lingering_potions", ids("lingering_potion"));
		addGroup(all, tabId, "potions", ids("potion"));
		addGroup(all, tabId, "cooked_food", ids("cooked"));
		addGroup(all, tabId, "raw_food", ids(
			"beef", "porkchop", "mutton", "chicken", "rabbit", ":cod", "salmon"), ids("rabbit_"));

		tabId = "minecraft:ingredients";
		addGroup(all, tabId, "dyes", ids("dye"));
		addGroup(all, tabId, "banner_patterns", ids("banner_pattern"));
		addGroup(all, tabId, "pottery_sherds", ids("pottery_sherd"));
		addGroup(all, tabId, "smithing_templates", ids("smithing_template"));
		addGroup(all, tabId, "enchanted_books", ids("enchanted_book"));

		return List.copyOf(all);
	}
}
