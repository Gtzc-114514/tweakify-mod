package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockOutlineStyleStateTest {
	private static final double EPSILON = 1.0e-6;

	@Test
	void widthClampsToConfiguredRange() {
		assertEquals(TweakifyModConfig.MIN_BLOCK_OUTLINE_WIDTH, BlockOutlineStyleState.width(0.0), EPSILON);
		assertEquals(TweakifyModConfig.MAX_BLOCK_OUTLINE_WIDTH, BlockOutlineStyleState.width(99.0), EPSILON);
		assertEquals(4.0f, BlockOutlineStyleState.width(4.0), EPSILON);
		assertEquals(TweakifyModConfig.DEFAULT_BLOCK_OUTLINE_WIDTH,
			BlockOutlineStyleState.width(Double.NaN), EPSILON);
		assertEquals(TweakifyModConfig.DEFAULT_BLOCK_OUTLINE_WIDTH,
			BlockOutlineStyleState.width(Double.POSITIVE_INFINITY), EPSILON);
	}

	@Test
	void widthRoundsToConfiguredStep() {
		assertEquals(2.5f, BlockOutlineStyleState.width(2.3), EPSILON);
		assertEquals(2.0f, BlockOutlineStyleState.width(2.2), EPSILON);
		assertEquals(0.5f, BlockOutlineStyleState.width(0.1), EPSILON);
	}

	@Test
	void highContrastDisablesCustomStyle() {
		assertFalse(BlockOutlineStyleState.shouldApplyCustomStyle(false, false));
		assertTrue(BlockOutlineStyleState.shouldApplyCustomStyle(true, false));
		assertFalse(BlockOutlineStyleState.shouldApplyCustomStyle(true, true));
		assertFalse(BlockOutlineStyleState.shouldApplyCustomStyle(false, true));
	}

	@Test
	void paletteModeUsesPaletteColor() {
		assertEquals(0xFFFF5555,
			BlockOutlineStyleState.color("palette", "red", 100, 0));
		assertEquals(0xFFFFFFFF,
			BlockOutlineStyleState.color("unknown", "unknown", 100, 0),
			"未知模式/调色板应回退到白色单色");
	}

	@Test
	void rainbowChangesOverTimeAndStaysOpaque() {
		int first = BlockOutlineStyleState.color("rainbow", "white", 100, 0);
		int second = BlockOutlineStyleState.color("rainbow", "white", 100, 750);

		assertEquals(0xFF, (first >>> 24) & 0xFF);
		assertNotEquals(first, second, "彩虹颜色应随时间变化");
	}

	@Test
	void pulseChangesBrightnessAndKeepsAlpha() {
		int dim = BlockOutlineStyleState.color("pulse", "red", 100, 0);
		int bright = BlockOutlineStyleState.color("pulse", "red", 100, 500);

		assertEquals(0xFF, (dim >>> 24) & 0xFF);
		assertTrue(((bright >> 16) & 0xFF) > ((dim >> 16) & 0xFF),
			"呼吸模式在半个周期处应更亮");
	}

	@Test
	void animationSpeedScalesPeriods() {
		assertEquals(30000L, BlockOutlineStyleState.rainbowPeriodMillis(10));
		assertEquals(3000L, BlockOutlineStyleState.rainbowPeriodMillis(100));
		assertEquals(1500L, BlockOutlineStyleState.rainbowPeriodMillis(200));
		assertEquals(6000L, BlockOutlineStyleState.rainbowPeriodMillis(50));
		assertEquals(750L, BlockOutlineStyleState.rainbowPeriodMillis(400));
		assertEquals(20000L, BlockOutlineStyleState.pulsePeriodMillis(10));
		assertEquals(2000L, BlockOutlineStyleState.pulsePeriodMillis(100));
		assertEquals(1000L, BlockOutlineStyleState.pulsePeriodMillis(200));
		assertEquals(4000L, BlockOutlineStyleState.pulsePeriodMillis(50));
		assertEquals(500L, BlockOutlineStyleState.pulsePeriodMillis(400));
	}
}
