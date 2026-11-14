package com.gtzc.tweakify.client;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 基岩顶点缓存的失效条件：markers 列表引用变化才重建，同一引用直接复用。
 */
class BedrockGeometryCacheTest {

	@Test
	void firstBuildCreatesOneSegmentPerExposedFace() {
		BedrockGeometryCache cache = new BedrockGeometryCache();
		List<BedrockHighlightState.Marker> markers = List.of(
			new BedrockHighlightState.Marker(new BlockPos(0, 64, 0), true, true),
			new BedrockHighlightState.Marker(new BlockPos(1, 64, 0), true, false),
			new BedrockHighlightState.Marker(new BlockPos(2, 64, 0), false, false));

		assertTrue(cache.ensure(markers), "首次调用应重建");
		assertEquals(3, cache.segments().size(), "两个面 + 一个面 + 零个面");
		assertEquals(3 * BedrockGeometryCache.VERTICES_PER_BOX, cache.geometry().vertexCount());
	}

	@Test
	void sameListReferenceDoesNotRebuild() {
		BedrockGeometryCache cache = new BedrockGeometryCache();
		List<BedrockHighlightState.Marker> markers =
			List.of(new BedrockHighlightState.Marker(new BlockPos(0, 64, 0), true, false));

		assertTrue(cache.ensure(markers));
		assertFalse(cache.ensure(markers), "同一列表引用不应重建");
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
		assertEquals(2, cache.segments().size(), "重建后只保留新列表的两个标记（各一个可见面）");
	}

	@Test
	void emptyListProducesNoGeometry() {
		BedrockGeometryCache cache = new BedrockGeometryCache();
		assertTrue(cache.ensure(new ArrayList<>()));
		assertEquals(0, cache.segments().size());
		assertEquals(0, cache.geometry().vertexCount());
	}

	@Test
	void topBoxVerticesAreRelativeToBlockOrigin() {
		BedrockGeometryCache cache = new BedrockGeometryCache();
		cache.ensure(List.of(new BedrockHighlightState.Marker(new BlockPos(10, 64, -5), true, false)));

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
}
