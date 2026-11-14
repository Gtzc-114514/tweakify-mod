package com.gtzc.tweakify.client;

import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 攻击范围顶点缓存的失效条件与 LOD 顶点数：
 * 条目列表或显示/渲染模式变化才重建，同一引用直接复用。
 */
class AttackRangeGeometryCacheTest {

	@Test
	void firstBuildCreatesGeometryPerEntryWithAllLods() {
		AttackRangeGeometryCache cache = new AttackRangeGeometryCache();
		AttackRangeState.Entry entry = entry(0.0, 64.0, 1.0f);
		List<AttackRangeState.Entry> entries = List.of(entry);

		assertTrue(cache.ensure(entries, false, false), "首次调用应重建");
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
		assertFalse(cache.ensure(entries, false, false), "条目与模式都没变时不应重建");
	}

	@Test
	void sphereOrFilledChangeRebuilds() {
		AttackRangeGeometryCache cache = new AttackRangeGeometryCache();
		List<AttackRangeState.Entry> entries = List.of(entry(0.0, 64.0, 1.0f));

		assertTrue(cache.ensure(entries, false, false));
		assertTrue(cache.ensure(entries, true, false), "切到球体要重建");
		assertTrue(cache.ensure(entries, true, true), "切到填充要重建");
		assertTrue(cache.ensure(entries, false, true), "切回平面要重建");
	}

	@Test
	void newEntriesListRebuilds() {
		AttackRangeGeometryCache cache = new AttackRangeGeometryCache();
		AttackRangeState.Entry first = entry(0.0, 64.0, 1.0f);
		AttackRangeState.Entry second = entry(4.0, 64.0, 1.0f);

		assertTrue(cache.ensure(List.of(first), false, false));
		assertTrue(cache.ensure(List.of(first, second), false, false));
		assertEquals(2, cache.cachedSize());
	}

	@Test
	void sphereHasNoStep4Geometry() {
		AttackRangeGeometryCache cache = new AttackRangeGeometryCache();
		AttackRangeState.Entry entry = entry(0.0, 64.0, 1.0f);
		cache.ensure(List.of(entry), true, false);

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

		// 填充平面：24 段 × 4 顶点。
		assertEquals(96, cache.geometry(entry).step1().vertexCount());
	}

	@Test
	void radiusBelowRingWidthProducesSingleRing() {
		AttackRangeGeometryCache cache = new AttackRangeGeometryCache();
		// 半径 0.05 < RING_WIDTH(0.08)，内环被钳到 0，不生成。
		AttackRangeState.Entry entry = entry(0.0, 64.0, 0.05f);
		cache.ensure(List.of(entry), false, false);

		assertEquals(48, cache.geometry(entry).step1().vertexCount());
	}

	@Test
	void emptyEntriesClearPreviousGeometry() {
		AttackRangeGeometryCache cache = new AttackRangeGeometryCache();
		cache.ensure(List.of(entry(0.0, 64.0, 1.0f)), false, false);
		assertEquals(1, cache.cachedSize());

		assertTrue(cache.ensure(new ArrayList<>(), false, false));
		assertEquals(0, cache.cachedSize());
	}

	private static AttackRangeState.Entry entry(double x, double y, float radius) {
		return new AttackRangeState.Entry(x, y, y + 0.9, 0.0, radius,
			new AABB(x - radius, y - radius, -radius, x + radius, y + radius, radius), 0.0);
	}
}
