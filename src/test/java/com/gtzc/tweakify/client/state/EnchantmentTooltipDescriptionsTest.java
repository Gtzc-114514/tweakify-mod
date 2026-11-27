package com.gtzc.tweakify.client.state;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
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
	void cacheKeyTakesEveryFieldFromArguments() {
		Object registries = new Object();
		Object item = new Object();
		Object enchantments = new Object();
		Object type = new Object();
		Object language = new Object();
		EnchantmentTooltipDescriptions.CacheKey key = EnchantmentTooltipDescriptions.cacheKey(
			registries, item, enchantments, type, true, language);

		assertSame(registries, key.registries());
		assertSame(item, key.item());
		assertSame(enchantments, key.enchantments());
		assertSame(type, key.type());
		assertTrue(key.sneaking());
		assertSame(language, key.language());
	}

	@Test
	void cacheKeyChangesWhenAnyInputChanges() {
		Object registries = new Object();
		Object item = new Object();
		Object enchantments = new Object();
		Object type = new Object();
		Object language = new Object();
		EnchantmentTooltipDescriptions.CacheKey base = EnchantmentTooltipDescriptions.cacheKey(
			registries, item, enchantments, type, false, language);

		assertEquals(base, EnchantmentTooltipDescriptions.cacheKey(
			registries, item, enchantments, type, false, language));
		assertNotEquals(base, EnchantmentTooltipDescriptions.cacheKey(
			new Object(), item, enchantments, type, false, language), "注册表变化失效");
		assertNotEquals(base, EnchantmentTooltipDescriptions.cacheKey(
			registries, new Object(), enchantments, type, false, language), "物品变化失效");
		assertNotEquals(base, EnchantmentTooltipDescriptions.cacheKey(
			registries, item, new Object(), type, false, language), "附魔组件变化失效");
		assertNotEquals(base, EnchantmentTooltipDescriptions.cacheKey(
			registries, item, enchantments, new Object(), false, language), "组件类型变化失效");
		assertNotEquals(base, EnchantmentTooltipDescriptions.cacheKey(
			registries, item, enchantments, type, true, language), "潜行门控变化失效");
		assertNotEquals(base, EnchantmentTooltipDescriptions.cacheKey(
			registries, item, enchantments, type, false, new Object()), "语言变化失效");
	}

	@Test
	void planCacheEvictsOldestAndRebuildsIt() {
		EnchantmentTooltipDescriptions.PlanCache<String> cache =
			new EnchantmentTooltipDescriptions.PlanCache<>();
		AtomicInteger builds = new AtomicInteger();
		Supplier<List<EnchantmentTooltipDescriptions.DescriptionPlan>> builder = () -> {
			int call = builds.incrementAndGet();
			return List.of(new EnchantmentTooltipDescriptions.DescriptionPlan(
				"name-" + call, Component.literal("desc-" + call)));
		};

		List<EnchantmentTooltipDescriptions.DescriptionPlan> evicted = cache.get("k1", builder);
		for (String key : List.of("k0", "k2", "k3", "k4", "k5", "k6", "k7")) {
			cache.get(key, builder);
		}
		assertEquals(cache.maxEntries(), cache.sizeForTest());
		assertEquals(8, builds.get());

		List<EnchantmentTooltipDescriptions.DescriptionPlan> k0Hit = cache.get("k0", builder);
		assertSame(k0Hit, cache.get("k0", builder), "命中时复用同一计划实例");
		assertEquals(8, builds.get(), "命中不重建");

		// k1 仍是最旧键；插入 k8 时被淘汰。
		cache.get("k8", builder);
		assertEquals(cache.maxEntries(), cache.sizeForTest());
		assertEquals(9, builds.get());

		List<EnchantmentTooltipDescriptions.DescriptionPlan> rebuilt = cache.get("k1", builder);
		assertEquals(10, builds.get(), "被淘汰的键重新构建");
		assertNotSame(evicted, rebuilt, "淘汰后返回新计划实例");
		assertEquals("name-10", rebuilt.get(0).nameText());
		assertEquals(cache.maxEntries(), cache.sizeForTest());
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
		List<Component> first = EnchantmentTooltipDescriptions.appendCached(original,
			"registries", "item", "enchantments", "type", false, "language", builder);
		List<Component> second = EnchantmentTooltipDescriptions.appendCached(original,
			"registries", "item", "enchantments", "type", false, "language", builder);
		assertEquals(texts(first), texts(second));
		assertEquals(1, builds.get(), "同一键复用描述计划");

		EnchantmentTooltipDescriptions.appendCached(original,
			"registries", "item", "enchantments", "type", true, "language", builder);
		assertEquals(2, builds.get(), "潜行门控变化后重新构建");
	}

	// 断线清理后同一键必须重新构建，验证 PlanCache 不再持有旧计划。
	@Test
	void clearCacheForcesRebuildAfterDisconnect() {
		List<Component> original = List.of(Component.literal("Sword"), Component.literal("Sharpness I"));
		AtomicInteger builds = new AtomicInteger();
		Supplier<List<EnchantmentTooltipDescriptions.DescriptionPlan>> builder = () -> {
			builds.incrementAndGet();
			return List.of(new EnchantmentTooltipDescriptions.DescriptionPlan(
				"Sharpness I", Component.literal("  sharp")));
		};

		EnchantmentTooltipDescriptions.appendCached(original,
			"registries", "item", "enchantments", "type", false, "language", builder);
		assertEquals(1, builds.get());

		EnchantmentTooltipDescriptions.clearCache();
		EnchantmentTooltipDescriptions.appendCached(original,
			"registries", "item", "enchantments", "type", false, "language", builder);

		assertEquals(2, builds.get(), "断线清理后同一键必须重新构建");
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
		List<Component> cached = EnchantmentTooltipDescriptions.appendCached(original,
			"registries", "item", "enchantments", "type", false, "language", builder);
		List<Component> uncached = EnchantmentTooltipDescriptions.append(original, builder.get());

		assertEquals(texts(uncached), texts(cached), "缓存路径与无缓存构建结果一致");
		assertEquals(2, builds.get());
	}

	private static List<String> texts(List<Component> lines) {
		return lines.stream().map(Component::getString).toList();
	}
}
