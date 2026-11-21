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
		/** 修复类问题（草径卡船等）。 */
		FIX,
		/** 测试/开发辅助功能（档案编辑器等）。 */
		TESTING,
		/** 性能优化类（MiniHUD 掉帧优化等）。 */
		OPTIMIZATION,
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
		// 顺序即界面顺序：各标签的条目在原表中交错出现，这里按原顺序逐段追加；
		// 分片类的 addPartN 对应原表里一段连续的同类条目，新增条目请追加到对应段。
		GeneralCatalogEntries.addPart1(entries);
		ModifiedCatalogEntries.addPart1(entries);
		FunctionCatalogEntries.addPart1(entries);
		ModifiedCatalogEntries.addPart2(entries);
		FunctionCatalogEntries.addPart2(entries);
		ModifiedCatalogEntries.addPart3(entries);
		FunctionCatalogEntries.addPart3(entries);
		ModifiedCatalogEntries.addPart4(entries);
		FunctionCatalogEntries.addPart4(entries);
		ModifiedCatalogEntries.addPart5(entries);
		FunctionCatalogEntries.addPart5(entries);
		FixCatalogEntries.addPart1(entries);
		OptimizationCatalogEntries.addPart1(entries);
		ModifiedCatalogEntries.addPart6(entries);
		FunctionCatalogEntries.addPart6(entries);
		ModifiedCatalogEntries.addPart7(entries);
		FunctionCatalogEntries.addPart7(entries);
		ModifiedCatalogEntries.addPart8(entries);
		FunctionCatalogEntries.addPart8(entries);
		ModifiedCatalogEntries.addPart9(entries);
		FunctionCatalogEntries.addPart9(entries);
		ModifiedCatalogEntries.addPart10(entries);
		FunctionCatalogEntries.addPart10(entries);
		ModifiedCatalogEntries.addPart11(entries);
		GeneralCatalogEntries.addPart2(entries);
		TestingCatalogEntries.addPart1(entries);
		validate(entries);
		return entries;
	}

	static Entry root(String name) {
		return new Entry(name, null, null, false);
	}

	static Entry function(String name, boolean command) {
		return new Entry(name, null, Tab.FUNCTION, command);
	}

	// 双标签项的工厂方法：当前没有配置项使用，保留以保证分类能力完整。
	@SuppressWarnings("unused")
	static Entry functionAndGeneral(String name, boolean command) {
		return new Entry(name, null, Tab.FUNCTION_AND_GENERAL, command);
	}

	// 通用标签的根节点（可带命令）。
	static Entry general(String name, boolean command) {
		return new Entry(name, null, null, command);
	}

	static Entry modified(String name) {
		return new Entry(name, null, Tab.MODIFIED, false);
	}

	static Entry modified(String name, boolean command) {
		return new Entry(name, null, Tab.MODIFIED, command);
	}

	// 修复标签的根节点（可带命令）。
	static Entry fix(String name, boolean command) {
		return new Entry(name, null, Tab.FIX, command);
	}

	// 优化标签的根节点（可带命令）。
	static Entry optimization(String name, boolean command) {
		return new Entry(name, null, Tab.OPTIMIZATION, command);
	}

	// 测试标签的根节点（可带命令）。
	static Entry testing(String name, boolean command) {
		return new Entry(name, null, Tab.TESTING, command);
	}

	static Entry child(String parent, String name) {
		return new Entry(name, parent, null, false);
	}

	static Entry child(String parent, String name, boolean command) {
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
