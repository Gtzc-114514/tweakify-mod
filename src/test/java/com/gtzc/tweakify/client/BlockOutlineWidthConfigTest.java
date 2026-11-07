package com.gtzc.tweakify.client;

import com.google.gson.JsonPrimitive;
import com.gtzc.tweakify.config.TweakifyModConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockOutlineWidthConfigTest {
	private static final double EPSILON = 1.0e-6;

	@Test
	void rangeAndDefaultMatchConfigConstants() {
		// 只验证构造器仍接线到常量；真实选项实例的范围由 TweakifyModConfigOptionsTest 守护。
		BlockOutlineWidthConfig config = new BlockOutlineWidthConfig(TweakifyModConfig.BLOCK_OUTLINE_WIDTH_FIELD);

		assertEquals(TweakifyModConfig.DEFAULT_BLOCK_OUTLINE_WIDTH, config.getDefaultDoubleValue(), EPSILON);
		assertEquals(TweakifyModConfig.MIN_BLOCK_OUTLINE_WIDTH, config.getMinDoubleValue(), EPSILON);
		assertEquals(TweakifyModConfig.MAX_BLOCK_OUTLINE_WIDTH, config.getMaxDoubleValue(), EPSILON);
	}

	@Test
	void roundsToConfiguredStep() {
		BlockOutlineWidthConfig config = new BlockOutlineWidthConfig(TweakifyModConfig.BLOCK_OUTLINE_WIDTH_FIELD);

		config.setDoubleValue(2.3);
		assertEquals(2.5, config.getDoubleValue(), EPSILON);

		config.setDoubleValue(2.2);
		assertEquals(TweakifyModConfig.DEFAULT_BLOCK_OUTLINE_WIDTH, config.getDoubleValue(), EPSILON);
	}

	@Test
	void clampsToConfiguredRangeAfterRounding() {
		BlockOutlineWidthConfig config = new BlockOutlineWidthConfig(TweakifyModConfig.BLOCK_OUTLINE_WIDTH_FIELD);

		config.setDoubleValue(99.0);
		assertEquals(TweakifyModConfig.MAX_BLOCK_OUTLINE_WIDTH, config.getDoubleValue(), EPSILON);

		config.setDoubleValue(-3.0);
		assertEquals(TweakifyModConfig.MIN_BLOCK_OUTLINE_WIDTH, config.getDoubleValue(), EPSILON);

		config.setDoubleValue(Double.NaN);
		assertEquals(TweakifyModConfig.DEFAULT_BLOCK_OUTLINE_WIDTH, config.getDoubleValue(), EPSILON);

		config.setDoubleValue(Double.POSITIVE_INFINITY);
		assertEquals(TweakifyModConfig.DEFAULT_BLOCK_OUTLINE_WIDTH, config.getDoubleValue(), EPSILON);
	}

	@Test
	void textInputRoundsToConfiguredStep() {
		BlockOutlineWidthConfig config = new BlockOutlineWidthConfig(TweakifyModConfig.BLOCK_OUTLINE_WIDTH_FIELD);

		config.setValueFromString("3.7");
		assertEquals(3.5, config.getDoubleValue(), EPSILON);

		double before = config.getDoubleValue();
		config.setValueFromString("not-a-number");
		assertEquals(before, config.getDoubleValue(), EPSILON);
	}

	@Test
	void jsonElementAlsoRoundsToConfiguredStep() {
		BlockOutlineWidthConfig config = new BlockOutlineWidthConfig(TweakifyModConfig.BLOCK_OUTLINE_WIDTH_FIELD);

		config.setValueFromJsonElement(new JsonPrimitive(2.3));
		assertEquals(2.5, config.getDoubleValue(), EPSILON);

		config.setValueFromJsonElement(new JsonPrimitive("2.3"));
		assertEquals(2.5, config.getDoubleValue(), EPSILON);
	}
}
