package com.gtzc.tweakify.client;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 基岩高亮的提交前可见性判断：按相机高度决定顶/底面是否可见，
 * 与 {@link BedrockHighlightRenderer#drawGeometry} 的选段条件一致。
 */
class BedrockHighlightRendererTest {

	@Test
	void emptyMarkersAreNotVisible() {
		assertFalse(BedrockHighlightRenderer.hasVisible(List.of(), 64.0));
	}

	@Test
	void topFaceVisibleFromAbove() {
		assertTrue(BedrockHighlightRenderer.hasVisible(List.of(marker(64, true, false)), 65.5));
	}

	@Test
	void bottomFaceVisibleFromBelow() {
		assertTrue(BedrockHighlightRenderer.hasVisible(List.of(marker(64, false, true)), 63.5));
	}

	@Test
	void bothFacesHiddenInsideBlock() {
		// 相机在方块内部：顶面要求 y > blockY + 1，底面要求 y < blockY，两者都不满足。
		assertFalse(BedrockHighlightRenderer.hasVisible(List.of(marker(64, true, true)), 64.5));
	}

	@Test
	void markerWithoutExposedFaceIsNotVisible() {
		assertFalse(BedrockHighlightRenderer.hasVisible(List.of(marker(64, false, false)), 70.0));
	}

	@Test
	void oneVisibleMarkerIsEnough() {
		assertTrue(BedrockHighlightRenderer.hasVisible(
			List.of(marker(64, false, false), marker(80, true, false)), 82.0));
	}

	private static BedrockHighlightState.Marker marker(int y, boolean top, boolean bottom) {
		return new BedrockHighlightState.Marker(new BlockPos(0, y, 0), top, bottom);
	}
}
