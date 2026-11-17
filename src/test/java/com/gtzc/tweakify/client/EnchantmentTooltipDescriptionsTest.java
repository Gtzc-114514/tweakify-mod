package com.gtzc.tweakify.client;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 附魔描述计划的缓存语义：键的每个维度都要失效、命中复用同一计划，
 * 插入结果必须与无缓存的内联路径一致。
 */
class EnchantmentTooltipDescriptionsTest {
	@BeforeEach
	void resetCache() {
		EnchantmentTooltipDescriptions.resetCacheForTest();
	}

	@Test
	void cacheKeyChangesWhenAnyInputChanges() {
		Object registries = new Object();
		Object item = new Object();
		Object enchantments = new Object();
		Object type = new Object();
		Object language = new Object();
		EnchantmentTooltipDescriptions.CacheKey base =
			new EnchantmentTooltipDescriptions.CacheKey(registries, item, enchantments, type, false, language);

		assertEquals(base, new EnchantmentTooltipDescriptions.CacheKey(
			registries, item, enchantments, type, false, language));
		assertNotEquals(base, new EnchantmentTooltipDescriptions.CacheKey(
			new Object(), item, enchantments, type, false, language), "注册表变化失效");
		assertNotEquals(base, new EnchantmentTooltipDescriptions.CacheKey(
			registries, new Object(), enchantments, type, false, language), "物品变化失效");
		assertNotEquals(base, new EnchantmentTooltipDescriptions.CacheKey(
			registries, item, new Object(), type, false, language), "附魔组件变化失效");
		assertNotEquals(base, new EnchantmentTooltipDescriptions.CacheKey(
			registries, item, enchantments, new Object(), false, language), "组件类型变化失效");
		assertNotEquals(base, new EnchantmentTooltipDescriptions.CacheKey(
			registries, item, enchantments, type, true, language), "潜行门控变化失效");
		assertNotEquals(base, new EnchantmentTooltipDescriptions.CacheKey(
			registries, item, enchantments, type, false, new Object()), "语言变化失效");
	}

	@Test
	void planCacheReusesSameKeyAndKeepsBoundedSize() {
		EnchantmentTooltipDescriptions.PlanCache<String> cache =
			new EnchantmentTooltipDescriptions.PlanCache<>();
		AtomicInteger builds = new AtomicInteger();
		Supplier<List<EnchantmentTooltipDescriptions.DescriptionPlan>> builder = () -> {
			builds.incrementAndGet();
			return List.of(new EnchantmentTooltipDescriptions.DescriptionPlan(
				"Sharpness I", Component.literal("  sharp")));
		};

		List<EnchantmentTooltipDescriptions.DescriptionPlan> first = cache.get("key", builder);
		assertSame(first, cache.get("key", builder), "命中时复用同一计划实例");
		assertEquals(1, builds.get());

		cache.get("other", builder);
		assertEquals(2, builds.get(), "不同键分别构建");

		for (int i = 0; i < cache.maxEntries() * 2; i++) {
			cache.get("evict-" + i, builder);
		}
		assertTrue(cache.size() <= cache.maxEntries(), "超出上限时淘汰旧条目");
	}

	@Test
	void appendCachedReusesPlanAndInvalidatesOnKeyChange() {
		List<Component> original = List.of(Component.literal("Sword"), Component.literal("Sharpness I"));
		AtomicInteger builds = new AtomicInteger();
		Supplier<List<EnchantmentTooltipDescriptions.DescriptionPlan>> builder = () -> {
			builds.incrementAndGet();
			return List.of(new EnchantmentTooltipDescriptions.DescriptionPlan(
				"Sharpness I", Component.literal("  sharp")));
		};
		EnchantmentTooltipDescriptions.CacheKey firstKey =
			new EnchantmentTooltipDescriptions.CacheKey("registries", "item", "enchantments", "type",
				false, "language");
		EnchantmentTooltipDescriptions.CacheKey sneakKey =
			new EnchantmentTooltipDescriptions.CacheKey("registries", "item", "enchantments", "type",
				true, "language");

		List<Component> first = EnchantmentTooltipDescriptions.appendCached(original, firstKey, builder);
		List<Component> second = EnchantmentTooltipDescriptions.appendCached(original, firstKey, builder);
		assertEquals(texts(first), texts(second));
		assertEquals(1, builds.get(), "同一键复用描述计划");

		EnchantmentTooltipDescriptions.appendCached(original, sneakKey, builder);
		assertEquals(2, builds.get(), "潜行门控变化后重新构建");
	}

	@Test
	void appendInsertsDescriptionsAfterMatchingLinesAndKeepsTrailingOrder() {
		List<Component> original = List.of(
			Component.literal("Sword"),
			Component.literal("Sharpness I"),
			Component.literal("Lore"));
		List<EnchantmentTooltipDescriptions.DescriptionPlan> plan = List.of(
			new EnchantmentTooltipDescriptions.DescriptionPlan("Sharpness I", Component.literal("  sharp desc")),
			new EnchantmentTooltipDescriptions.DescriptionPlan(
				"Unbreaking I", Component.literal("  unbreaking desc")));

		List<Component> result = EnchantmentTooltipDescriptions.append(original, plan);

		assertEquals(List.of("Sword", "Sharpness I", "  sharp desc", "Lore", "  unbreaking desc"),
			texts(result));
		assertSame(original, EnchantmentTooltipDescriptions.append(original, List.of()),
			"没有描述时不复制原始列表");
	}

	@Test
	void appendKeepsDuplicateEnchantmentLinesPairedInOrder() {
		List<Component> original = List.of(
			Component.literal("Book"),
			Component.literal("Sharpness I"),
			Component.literal("Sharpness I"));
		List<EnchantmentTooltipDescriptions.DescriptionPlan> plan = List.of(
			new EnchantmentTooltipDescriptions.DescriptionPlan("Sharpness I", Component.literal("  first")),
			new EnchantmentTooltipDescriptions.DescriptionPlan("Sharpness I", Component.literal("  second")));

		List<Component> result = EnchantmentTooltipDescriptions.append(original, plan);

		assertEquals(List.of("Book", "Sharpness I", "  first", "Sharpness I", "  second"),
			texts(result));
	}

	// 无缓存等价性：缓存命中路径与直接使用新建计划的路径必须产出相同文本。
	@Test
	void cachedPlanMatchesFreshlyBuiltPlan() {
		List<Component> original = List.of(Component.literal("Sword"), Component.literal("Sharpness I"));
		AtomicInteger builds = new AtomicInteger();
		Supplier<List<EnchantmentTooltipDescriptions.DescriptionPlan>> builder = () -> {
			builds.incrementAndGet();
			return List.of(new EnchantmentTooltipDescriptions.DescriptionPlan(
				"Sharpness I", Component.literal("  sharp")));
		};
		EnchantmentTooltipDescriptions.CacheKey key =
			new EnchantmentTooltipDescriptions.CacheKey("registries", "item", "enchantments", "type",
				false, "language");

		List<Component> cached = EnchantmentTooltipDescriptions.appendCached(original, key, builder);
		List<Component> uncached = EnchantmentTooltipDescriptions.append(original, builder.get());

		assertEquals(texts(uncached), texts(cached), "缓存路径与无缓存构建结果一致");
		assertEquals(2, builds.get());
	}

	private static List<String> texts(List<Component> lines) {
		return lines.stream().map(Component::getString).toList();
	}
}
