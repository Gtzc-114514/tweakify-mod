package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 单层基岩判定：自身是基岩且上下都不是基岩才算单层。
 */
class BedrockHighlightStateTest {

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
}
