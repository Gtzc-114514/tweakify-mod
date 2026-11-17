package com.gtzc.tweakify.client;

import com.gtzc.tweakify.util.HookProbe;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 附魔描述行的构建与缓存：把“附魔行名称 + 描述组件”作为计划缓存，
 * 每次 tooltip 只按当前原始行重新定位插入点，不重复翻译与创建组件。
 *
 * <p>缓存键包含注册表、物品、附魔组件、组件类型、潜行门控与 {@code Language} 实例：
 * 换世界/数据包、物品或附魔变化、潜行门控开关、语言或资源重载时都会换键重建。
 * 计划不包含插入位置，因此其它模组动态增删 tooltip 行时仍按当前列表重新定位。
 *
 * <p>只在客户端 tooltip 路径调用，缓存按客户端线程单线程访问，不做同步。
 */
public final class EnchantmentTooltipDescriptions {
	// 缓存条目：定位附魔行用的名称文本 + 已构建的描述组件。
	public record DescriptionPlan(String nameText, Component description) {
	}

	// 缓存键：字段用 Object 保存，比较语义沿用各值自身的 equals；
	// 也便于单测用替身对象覆盖每个失效维度。
	public record CacheKey(Object registries, Object item, Object enchantments, Object type,
						   boolean sneaking, Object language) {
	}

	// 描述行与插入位置：先收集、再按位置降序插入，避免前面的插入让后面的索引偏移。
	private record DescriptionInsertion(int position, Component description) {
	}

	private static final PlanCache<CacheKey> CACHE = new PlanCache<>();

	private EnchantmentTooltipDescriptions() {
	}

	// 由 mixin 调用：按缓存键取计划，未命中时用当前注册表构建。
	public static List<Component> appendCached(List<Component> original, CacheKey key,
			Supplier<List<DescriptionPlan>> builder) {
		return append(original, CACHE.get(key, builder));
	}

	// 构建描述计划：顺序与原本的内联实现完全一致，只是把结果交给缓存。
	public static List<DescriptionPlan> buildPlan(HolderLookup.Provider registries,
			ItemEnchantments enchantments) {
		// 按原版附魔行顺序（TOOLTIP_ORDER 标签序，其余按哈希序）遍历，重名时归属不会互换。
		HolderSet<Enchantment> tooltipOrder = registries.lookupOrThrow(Registries.ENCHANTMENT)
			.getOrThrow(EnchantmentTags.TOOLTIP_ORDER);
		List<Holder<Enchantment>> ordered = new ArrayList<>();
		for (Holder<Enchantment> holder : tooltipOrder) {
			if (enchantments.getLevel(holder) > 0) {
				ordered.add(holder);
			}
		}
		for (Holder<Enchantment> holder : enchantments.keySet()) {
			if (!tooltipOrder.contains(holder)) {
				ordered.add(holder);
			}
		}

		List<DescriptionPlan> plan = new ArrayList<>();
		for (Holder<Enchantment> holder : ordered) {
			Identifier id = holder.unwrapKey().map(ResourceKey::identifier).orElse(null);
			if (id == null) {
				continue;
			}
			String key = "enchantment." + id.getNamespace() + "." + id.getPath() + ".desc";
			Component description = Component.translatable(key);
			String text = description.getString();
			// 没有对应语言键（返回键本身）或描述为空时不追加。
			if (text.isBlank() || text.equals(key)) {
				continue;
			}
			Component descLine = Component.literal("  ")
				.append(description)
				.withStyle(ChatFormatting.DARK_GRAY);
			String nameText = Enchantment.getFullname(holder, enchantments.getLevel(holder)).getString();
			plan.add(new DescriptionPlan(nameText, descLine));
		}
		return plan;
	}

	// 把计划插入当前 tooltip：从索引 1 开始（第 0 行是物品悬停名称），
	// 同名附魔行按出现顺序对应；找不到对应行时按遍历顺序退回末尾。
	static List<Component> append(List<Component> original, List<DescriptionPlan> plan) {
		if (plan.isEmpty()) {
			return original;
		}
		List<Component> lines = new ArrayList<>(original);
		List<DescriptionInsertion> insertions = new ArrayList<>();
		List<Component> trailing = new ArrayList<>();
		// 每个附魔行只分配一次：同名附魔行按出现顺序对应，避免描述堆叠到同一行。
		Set<Integer> usedLines = new HashSet<>();
		for (DescriptionPlan entry : plan) {
			int lineIndex = -1;
			for (int i = 1; i < lines.size(); i++) {
				if (!usedLines.contains(i) && lines.get(i).getString().equals(entry.nameText())) {
					lineIndex = i;
					usedLines.add(i);
					break;
				}
			}
			if (lineIndex >= 0) {
				insertions.add(new DescriptionInsertion(lineIndex + 1, entry.description()));
			} else {
				trailing.add(entry.description());
			}
		}
		if (insertions.isEmpty() && trailing.isEmpty()) {
			return original;
		}
		insertions.sort((first, second) -> Integer.compare(second.position(), first.position()));
		for (DescriptionInsertion insertion : insertions) {
			lines.add(insertion.position(), insertion.description());
		}
		lines.addAll(trailing);
		HookProbe.markOnce("enchantmentDescriptions");
		return lines;
	}

	// 包私有：测试之间清空生产缓存，避免跨用例互相影响。
	static void resetCacheForTest() {
		CACHE.clear();
	}

	// 小型 LRU：tooltip 通常只查询悬停中的一个物品，8 条足够覆盖快捷切换；
	// 超出时淘汰最久未使用的条目，不做整表清空。
	static final class PlanCache<K> {
		static final int MAX_ENTRIES = 8;
		private final Map<K, List<DescriptionPlan>> entries =
			new LinkedHashMap<>(MAX_ENTRIES + 1, 0.75f, true) {
				@Override
				protected boolean removeEldestEntry(Map.Entry<K, List<DescriptionPlan>> eldest) {
					return size() > MAX_ENTRIES;
				}
			};

		List<DescriptionPlan> get(K key, Supplier<List<DescriptionPlan>> builder) {
			List<DescriptionPlan> cached = this.entries.get(key);
			if (cached != null) {
				return cached;
			}
			List<DescriptionPlan> built = List.copyOf(builder.get());
			this.entries.put(key, built);
			return built;
		}

		int size() {
			return this.entries.size();
		}

		int maxEntries() {
			return MAX_ENTRIES;
		}

		void clear() {
			this.entries.clear();
		}
	}
}
