package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockOutlineWidthSliderCallbackTest {
	@Test
	void formatsWithOneDecimal() {
		BlockOutlineWidthConfig config = new BlockOutlineWidthConfig(
			com.gtzc.tweakify.config.TweakifyModConfig.BLOCK_OUTLINE_WIDTH_FIELD);
		config.setDoubleValue(2.5);

		BlockOutlineWidthSliderCallback callback = new BlockOutlineWidthSliderCallback(config, null);

		assertEquals("2.5", callback.getFormattedDisplayValue());

		config.setDoubleValue(0.5);
		assertEquals("0.5", callback.getFormattedDisplayValue());

		config.setDoubleValue(8.0);
		assertEquals("8.0", callback.getFormattedDisplayValue());
	}
}
