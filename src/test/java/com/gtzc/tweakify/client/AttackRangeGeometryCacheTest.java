package com.gtzc.tweakify.client;

import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 攻击范围顶点缓存的失效条件、LOD 顶点数与后台发布：
 * 条目列表或显示/渲染模式变化才提交构建，同一引用直接复用；
 * 构建完成后一次性发布快照，旧任务不能覆盖更新的代。
 */
class AttackRangeGeometryCacheTest {
	// 后台构建通常亚毫秒级；上限只影响失败场景耗时。
	private static final long WAIT_MILLIS = 5_000L;

	@Test
	void firstBuildCreatesGeometryPerEntryWithAllLods() {
		AttackRangeGeometryCache cache = new AttackRangeGeometryCache();
		AttackRangeState.Entry entry = entry(0.0, 64.0, 1.0f);
		List<AttackRangeState.Entry> entries = List.of(entry);

		assertTrue(cache.ensure(entries, false, false), "首次调用应提交构建");
		assertTrue(cache.awaitPublishedForTest(WAIT_MILLIS), "后台构建应在超时前发布");
		assertEquals(1, cache.cachedSize());
		AttackRangeGeometryCache.EntryGeometry geometry = cache.geometry(entry);
		assertNotNull(geometry);

		// 平面线框：24 段 × 外/内两环 × 2 顶点 = 96；step 2/4 分别减半。
		assertEquals(96, geometry.step1().vertexCount());
		assertEquals(48, geometry.step2().vertexCount());
		assertEquals(24, geometry.step4().vertexCount());
	}

	@Test
	void sameEntriesAndModeDoNotRebuild() {
		AttackRangeGeometryCache cache = new AttackRangeGeometryCache();
		List<AttackRangeState.Entry> entries = List.of(entry(0.0, 64.0, 1.0f));

		assertTrue(cache.ensure(entries, false, false));
		assertTrue(cache.awaitPublishedForTest(WAIT_MILLIS));
		assertFalse(cache.ensure(entries, false, false), "条目与模式都没变时不应提交构建");
	}

	@Test
	void sphereOrFilledChangeRebuilds() {
		AttackRangeGeometryCache cache = new AttackRangeGeometryCache();
		List<AttackRangeState.Entry> entries = List.of(entry(0.0, 64.0, 1.0f));

		assertTrue(cache.ensure(entries, false, false));
		assertTrue(cache.ensure(entries, true, false), "切到球体要重建");
		assertTrue(cache.ensure(entries, true, true), "切到填充要重建");
		assertTrue(cache.ensure(entries, false, true), "切回平面要重建");
		assertTrue(cache.awaitPublishedForTest(WAIT_MILLIS), "最后一次构建应发布");
	}

	@Test
	void newEntriesListRebuilds() {
		AttackRangeGeometryCache cache = new AttackRangeGeometryCache();
		AttackRangeState.Entry first = entry(0.0, 64.0, 1.0f);
		AttackRangeState.Entry second = entry(4.0, 64.0, 1.0f);

		assertTrue(cache.ensure(List.of(first), false, false));
		assertTrue(cache.ensure(List.of(first, second), false, false));
		assertTrue(cache.awaitPublishedForTest(WAIT_MILLIS));
		assertEquals(2, cache.cachedSize());
	}

	@Test
	void sphereHasNoStep4Geometry() {
		AttackRangeGeometryCache cache = new AttackRangeGeometryCache();
		AttackRangeState.Entry entry = entry(0.0, 64.0, 1.0f);
		cache.ensure(List.of(entry), true, false);
		assertTrue(cache.awaitPublishedForTest(WAIT_MILLIS));

		AttackRangeGeometryCache.EntryGeometry geometry = cache.geometry(entry);
		assertEquals(0, geometry.step4().vertexCount(), "球体只缓存 step 1/2");
		// 球体线框 step1：纬度环 9×8 + 经线弧 8×8 = 136 条线 × 2 顶点。
		assertEquals(272, geometry.step1().vertexCount());
		// step2：纬度 5 环×4 经 + 经线 4×4 = 36 条线 × 2 顶点。
		assertEquals(72, geometry.step2().vertexCount());
	}

	@Test
	void filledFlatHasOneQuadPerSegment() {
		AttackRangeGeometryCache cache = new AttackRangeGeometryCache();
		AttackRangeState.Entry entry = entry(0.0, 64.0, 1.0f);
		cache.ensure(List.of(entry), false, true);
		assertTrue(cache.awaitPublishedForTest(WAIT_MILLIS));

		// 填充平面：24 段 × 4 顶点。
		assertEquals(96, cache.geometry(entry).step1().vertexCount());
	}

	@Test
	void radiusBelowRingWidthProducesSingleRing() {
		AttackRangeGeometryCache cache = new AttackRangeGeometryCache();
		// 半径 0.05 < RING_WIDTH(0.08)，内环被钳到 0，不生成。
		AttackRangeState.Entry entry = entry(0.0, 64.0, 0.05f);
		cache.ensure(List.of(entry), false, false);
		assertTrue(cache.awaitPublishedForTest(WAIT_MILLIS));

		assertEquals(48, cache.geometry(entry).step1().vertexCount());
	}

	@Test
	void emptyEntriesClearPreviousGeometry() {
		AttackRangeGeometryCache cache = new AttackRangeGeometryCache();
		cache.ensure(List.of(entry(0.0, 64.0, 1.0f)), false, false);
		assertTrue(cache.awaitPublishedForTest(WAIT_MILLIS));
		assertEquals(1, cache.cachedSize());

		// 空输入在渲染线程直接发布空快照，不占用后台队列，也不会被在途任务盖回。
		assertTrue(cache.ensure(new ArrayList<>(), false, false));
		assertTrue(cache.awaitPublishedForTest(WAIT_MILLIS));
		assertEquals(0, cache.cachedSize());
	}

	// 调色板变化必须重建顶点，颜色仍按模式的透明度档位拼上。
	@Test
	void paletteChangeRebuildsWithNewRgb() {
		AttackRangeGeometryCache cache = new AttackRangeGeometryCache();
		List<AttackRangeState.Entry> entries = List.of(entry(0.0, 64.0, 1.0f));

		assertTrue(cache.ensure(entries, false, false, 0x3388FF));
		assertTrue(cache.awaitPublishedForTest(WAIT_MILLIS));
		assertEquals(0xAA3388FF, cache.geometry(entries.get(0)).step1().color(0),
			"平面线框使用 0xAA 透明度");
		assertFalse(cache.ensure(entries, false, false, 0x3388FF), "颜色未变不重建");

		assertTrue(cache.ensure(entries, false, false, 0xFF5555), "颜色变化要重建");
		assertTrue(cache.awaitPublishedForTest(WAIT_MILLIS));
		assertEquals(0xAAFF5555, cache.geometry(entries.get(0)).step1().color(0));
	}

	// 发布侧单调换代：旧代任务即使晚到，也不能把更新的快照盖回去。
	@Test
	void stalePublishAfterNewerSnapshotIsRejected() {
		AttackRangeGeometryCache cache = new AttackRangeGeometryCache();
		AttackRangeState.Entry entry = entry(0.0, 64.0, 1.0f);
		IdentityHashMap<AttackRangeState.Entry, AttackRangeGeometryCache.EntryGeometry> newer =
			new IdentityHashMap<>();
		newer.put(entry, new AttackRangeGeometryCache.EntryGeometry(0.0, 64.0, 0.0,
			new CachedGeometry(), new CachedGeometry(), new CachedGeometry()));

		cache.publish(new AttackRangeGeometryCache.Snapshot(2L, newer));
		cache.publish(new AttackRangeGeometryCache.Snapshot(1L, new IdentityHashMap<>()));

		assertEquals(2L, cache.snapshot().generation(), "旧代不能覆盖新代");
		assertEquals(1, cache.cachedSize(), "新代的映射必须保留");
		assertNotNull(cache.geometry(entry));
	}

	// 乱序完成：先执行新任务、再执行旧任务，最终快照必须仍是新代。
	@Test
	void reorderedBuildsKeepNewestSnapshot() {
		ExecutorService original = GeometryBuildWorker.currentExecutorForTest();
		ManualExecutor manual = new ManualExecutor();
		GeometryBuildWorker.installExecutorForTest(manual);
		try {
			AttackRangeGeometryCache cache = new AttackRangeGeometryCache();
			AttackRangeState.Entry first = entry(0.0, 64.0, 1.0f);
			AttackRangeState.Entry second = entry(4.0, 64.0, 1.0f);

			assertTrue(cache.ensure(List.of(first), false, false));
			assertTrue(cache.ensure(List.of(first, second), false, false));
			assertEquals(2, manual.pendingCount(), "两次请求都应排队");

			// 倒序执行：旧任务在新任务之后才运行，必须被丢弃。
			manual.runAt(1);
			manual.runAt(0);

			assertTrue(cache.awaitPublishedForTest(WAIT_MILLIS));
			assertEquals(2L, cache.snapshot().generation(), "旧任务不能覆盖新快照");
			assertEquals(2, cache.cachedSize(), "新代的条目数必须保留");
		} finally {
			GeometryBuildWorker.installExecutorForTest(original);
			manual.shutdownNow();
		}
	}

	private static AttackRangeState.Entry entry(double x, double y, float radius) {
		return new AttackRangeState.Entry(x, y, y + 0.9, 0.0, radius,
			new AABB(x - radius, y - radius, -radius, x + radius, y + radius, radius), 0.0);
	}
}
