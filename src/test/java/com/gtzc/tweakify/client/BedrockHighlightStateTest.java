package com.gtzc.tweakify.client;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 单层基岩判定与发布去重：纯函数部分沿用原有覆盖；
 * 内容未变时保留旧列表引用，让 {@link BedrockGeometryCache} 跳过整表重建。
 */
class BedrockHighlightStateTest {
	private static final BlockPos CENTER = new BlockPos(0, 64, 0);

	@BeforeEach
	void reset() {
		BedrockHighlightState.clear();
	}

	// ===== 单层与位面判定 =====

	@Test
	void onlyBedrockWithoutBedrockNeighboursMatches() {
		assertTrue(BedrockHighlightState.isSingleLayer(true, false, false));
	}

	@Test
	void rejectsNonBedrockBlock() {
		assertFalse(BedrockHighlightState.isSingleLayer(false, false, false));
	}

	@Test
	void rejectsThickerBedrockColumns() {
		assertFalse(BedrockHighlightState.isSingleLayer(true, true, false), "bedrock above means a thicker column");
		assertFalse(BedrockHighlightState.isSingleLayer(true, false, true), "bedrock below means a thicker column");
		assertFalse(BedrockHighlightState.isSingleLayer(true, true, true));
	}

	@Test
	void onlyFacesTurnedToTheCameraAreDrawn() {
		// 相机在方块上方：只看到顶面。
		assertTrue(BedrockHighlightState.isFaceVisibleFrom(true, 65.5, 64));
		assertFalse(BedrockHighlightState.isFaceVisibleFrom(false, 65.5, 64));

		// 相机在方块下方：只看到底面。
		assertTrue(BedrockHighlightState.isFaceVisibleFrom(false, 63.5, 64));
		assertFalse(BedrockHighlightState.isFaceVisibleFrom(true, 63.5, 64));
	}

	// ===== 发布去重 =====

	@Test
	void identicalContentKeepsSameListInstance() {
		BedrockHighlightState.setMarkers(new ArrayList<>(List.of(marker(1, true, false))), CENTER, 256);
		List<BedrockHighlightState.Marker> first = BedrockHighlightState.markers();

		BedrockHighlightState.setMarkers(new ArrayList<>(List.of(marker(1, true, false))), CENTER, 256);

		assertSame(first, BedrockHighlightState.markers(), "内容相同应复用旧列表，顶点缓存不重建");
	}

	@Test
	void changedPositionPublishesNewList() {
		BedrockHighlightState.setMarkers(new ArrayList<>(List.of(marker(1, true, false))), CENTER, 256);
		List<BedrockHighlightState.Marker> first = BedrockHighlightState.markers();

		BedrockHighlightState.setMarkers(new ArrayList<>(List.of(marker(2, true, false))), CENTER, 256);

		assertNotSame(first, BedrockHighlightState.markers());
		assertEquals(2, BedrockHighlightState.markers().get(0).pos().getX());
	}

	@Test
	void changedExposedFacePublishesNewList() {
		BedrockHighlightState.setMarkers(new ArrayList<>(List.of(marker(1, true, false))), CENTER, 256);
		List<BedrockHighlightState.Marker> first = BedrockHighlightState.markers();

		BedrockHighlightState.setMarkers(new ArrayList<>(List.of(marker(1, true, true))), CENTER, 256);

		assertNotSame(first, BedrockHighlightState.markers());
	}

	@Test
	void emptyContentIsNotRepublished() {
		BedrockHighlightState.setMarkers(new ArrayList<>(), CENTER, 256);
		List<BedrockHighlightState.Marker> first = BedrockHighlightState.markers();

		BedrockHighlightState.setMarkers(new ArrayList<>(), CENTER, 256);

		assertSame(first, BedrockHighlightState.markers());
		assertTrue(BedrockHighlightState.markers().isEmpty());
	}

	@Test
	void clearAllowsRepublishingSameContent() {
		BedrockHighlightState.setMarkers(new ArrayList<>(List.of(marker(1, true, false))), CENTER, 256);
		BedrockHighlightState.clear();
		assertTrue(BedrockHighlightState.markers().isEmpty());

		BedrockHighlightState.setMarkers(new ArrayList<>(List.of(marker(1, true, false))), CENTER, 256);

		assertEquals(1, BedrockHighlightState.markers().size(), "clear 后重新开启应再次发布");
	}

	@Test
	void truncationKeepsNearestMarkersAndDeduplicates() {
		BedrockHighlightState.setMarkers(new ArrayList<>(List.of(
			marker(10, true, false), marker(1, true, false), marker(20, true, false))), CENTER, 2);
		List<BedrockHighlightState.Marker> first = BedrockHighlightState.markers();
		assertEquals(2, first.size());
		assertEquals(1, first.get(0).pos().getX(), "最近的排在前");
		assertEquals(10, first.get(1).pos().getX());

		// 被截断的远处标记变化不影响前两个：摘要与 equals 都相同，复用旧列表。
		BedrockHighlightState.setMarkers(new ArrayList<>(List.of(
			marker(20, true, true), marker(1, true, false), marker(10, true, false))), CENTER, 2);

		assertSame(first, BedrockHighlightState.markers());
	}

	@Test
	void truncatedNearestSetChangePublishesNewList() {
		BedrockHighlightState.setMarkers(new ArrayList<>(List.of(
			marker(1, true, false), marker(10, true, false), marker(20, true, false))), CENTER, 2);
		List<BedrockHighlightState.Marker> first = BedrockHighlightState.markers();

		// 最近的换成 x=2：截断后的前两个集合变化，必须重发。
		BedrockHighlightState.setMarkers(new ArrayList<>(List.of(
			marker(2, true, false), marker(10, true, false), marker(20, true, false))), CENTER, 2);

		assertNotSame(first, BedrockHighlightState.markers());
		assertEquals(2, BedrockHighlightState.markers().get(0).pos().getX());
	}

	private static BedrockHighlightState.Marker marker(int x, boolean top, boolean bottom) {
		return new BedrockHighlightState.Marker(new BlockPos(x, 64, 0), top, bottom);
	}
}
