package com.gtzc.tweakify.client;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 基岩顶点缓存的失效条件与后台发布：markers 列表引用变化才提交构建，
 * 同一引用直接复用；构建完成后几何与段表成对发布，旧任务不能覆盖更新的代。
 */
class BedrockGeometryCacheTest {
	// 后台构建通常亚毫秒级；上限只影响失败场景耗时。
	private static final long WAIT_MILLIS = 5_000L;

	@Test
	void firstBuildCreatesOneSegmentPerExposedFace() {
		BedrockGeometryCache cache = new BedrockGeometryCache();
		List<BedrockHighlightState.Marker> markers = List.of(
			new BedrockHighlightState.Marker(new BlockPos(0, 64, 0), true, true),
			new BedrockHighlightState.Marker(new BlockPos(1, 64, 0), true, false),
			new BedrockHighlightState.Marker(new BlockPos(2, 64, 0), false, false));

		assertTrue(cache.ensure(markers), "首次调用应提交构建");
		assertTrue(cache.awaitPublishedForTest(WAIT_MILLIS), "后台构建应在超时前发布");
		assertEquals(3, cache.segments().size(), "两个面 + 一个面 + 零个面");
		assertEquals(3 * BedrockGeometryCache.VERTICES_PER_BOX, cache.geometry().vertexCount());
	}

	@Test
	void sameListReferenceDoesNotRebuild() {
		BedrockGeometryCache cache = new BedrockGeometryCache();
		List<BedrockHighlightState.Marker> markers =
			List.of(new BedrockHighlightState.Marker(new BlockPos(0, 64, 0), true, false));

		assertTrue(cache.ensure(markers));
		assertTrue(cache.awaitPublishedForTest(WAIT_MILLIS));
		assertFalse(cache.ensure(markers), "同一列表引用不应提交构建");
		assertEquals(1, cache.segments().size());
	}

	@Test
	void newListReferenceRebuilds() {
		BedrockGeometryCache cache = new BedrockGeometryCache();
		assertTrue(cache.ensure(List.of(
			new BedrockHighlightState.Marker(new BlockPos(0, 64, 0), true, false))));
		assertTrue(cache.ensure(List.of(
			new BedrockHighlightState.Marker(new BlockPos(0, 64, 0), true, false),
			new BedrockHighlightState.Marker(new BlockPos(1, 64, 0), false, true))));
		assertTrue(cache.awaitPublishedForTest(WAIT_MILLIS));
		assertEquals(2, cache.segments().size(), "重建后只保留新列表的两个标记（各一个可见面）");
	}

	@Test
	void emptyListProducesNoGeometry() {
		BedrockGeometryCache cache = new BedrockGeometryCache();
		assertTrue(cache.ensure(new ArrayList<>()));
		assertTrue(cache.awaitPublishedForTest(WAIT_MILLIS));
		assertEquals(0, cache.segments().size());
		assertEquals(0, cache.geometry().vertexCount());
	}

	@Test
	void topBoxVerticesAreRelativeToBlockOrigin() {
		BedrockGeometryCache cache = new BedrockGeometryCache();
		cache.ensure(List.of(new BedrockHighlightState.Marker(new BlockPos(10, 64, -5), true, false)));
		assertTrue(cache.awaitPublishedForTest(WAIT_MILLIS));

		// 顶面小方块：x/z ∈ [0.25, 0.75]，y ∈ [1.002, 1.002 + 1/16]。
		CachedGeometry geometry = cache.geometry();
		for (int vertex = 0; vertex < geometry.vertexCount(); vertex++) {
			assertTrue(geometry.x(vertex) >= 0.25f && geometry.x(vertex) <= 0.75f, "x 相对方块原点");
			assertTrue(geometry.z(vertex) >= 0.25f && geometry.z(vertex) <= 0.75f, "z 相对方块原点");
			assertTrue(geometry.y(vertex) > 1.0f && geometry.y(vertex) < 1.1f, "y 在顶面之上");
		}
		BedrockGeometryCache.Segment segment = cache.segments().get(0);
		assertEquals(10, segment.blockX());
		assertEquals(64, segment.blockY());
		assertEquals(-5, segment.blockZ());
		assertTrue(segment.top());
	}

	// 颜色烘在顶点里：颜色变化必须触发重建，新顶点使用新颜色。
	@Test
	void colorChangeRebuildsVertices() {
		BedrockGeometryCache cache = new BedrockGeometryCache();
		List<BedrockHighlightState.Marker> markers =
			List.of(new BedrockHighlightState.Marker(new BlockPos(0, 64, 0), true, false));

		assertTrue(cache.ensure(markers, 0xFFFFC800));
		assertTrue(cache.awaitPublishedForTest(WAIT_MILLIS));
		int gold = cache.geometry().color(0);
		assertEquals(0xFFFFC800, gold);
		assertFalse(cache.ensure(markers, 0xFFFFC800), "同一颜色与列表引用不重建");

		assertTrue(cache.ensure(markers, 0xFFFF5555), "颜色变化要重建");
		assertTrue(cache.awaitPublishedForTest(WAIT_MILLIS));
		assertEquals(0xFFFF5555, cache.geometry().color(0));
		assertNotEquals(gold, cache.geometry().color(0));
	}

	// 发布侧单调换代：旧代任务即使晚到，也不能把更新的快照盖回去。
	@Test
	void stalePublishAfterNewerSnapshotIsRejected() {
		BedrockGeometryCache cache = new BedrockGeometryCache();
		CachedGeometry newerGeometry = new CachedGeometry();
		cache.publish(new BedrockGeometryCache.Snapshot(2L, newerGeometry, List.of(
			new BedrockGeometryCache.Segment(0, BedrockGeometryCache.VERTICES_PER_BOX, 1, 64, 1, true))));
		cache.publish(new BedrockGeometryCache.Snapshot(1L, new CachedGeometry(), List.of()));

		assertEquals(2L, cache.snapshot().generation(), "旧代不能覆盖新代");
		assertSame(newerGeometry, cache.geometry(), "新代几何必须保留");
		assertEquals(1, cache.segments().size(), "新代段表必须保留");
	}

	// 乱序完成：先执行新任务、再执行旧任务，最终快照必须仍是新代。
	@Test
	void reorderedBuildsKeepNewestSnapshot() {
		ExecutorService original = GeometryBuildWorker.currentExecutorForTest();
		ManualExecutor manual = new ManualExecutor();
		GeometryBuildWorker.installExecutorForTest(manual);
		try {
			BedrockGeometryCache cache = new BedrockGeometryCache();
			List<BedrockHighlightState.Marker> first = List.of(
				new BedrockHighlightState.Marker(new BlockPos(0, 64, 0), true, false));
			List<BedrockHighlightState.Marker> second = List.of(
				new BedrockHighlightState.Marker(new BlockPos(0, 64, 0), true, false),
				new BedrockHighlightState.Marker(new BlockPos(1, 64, 0), false, true));

			assertTrue(cache.ensure(first));
			assertTrue(cache.ensure(second));
			assertEquals(2, manual.pendingCount(), "两次请求都应排队");

			// 倒序执行：旧任务在新任务之后才运行，必须被丢弃。
			manual.runAt(1);
			manual.runAt(0);

			assertTrue(cache.awaitPublishedForTest(WAIT_MILLIS));
			assertEquals(2L, cache.snapshot().generation(), "旧任务不能覆盖新快照");
			assertEquals(2, cache.segments().size(), "新代的段表必须保留");
		} finally {
			GeometryBuildWorker.installExecutorForTest(original);
			manual.shutdownNow();
		}
	}
}
