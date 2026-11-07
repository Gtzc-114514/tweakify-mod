package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockOutlineColorModeTest {
	@Test
	void codesMatchSupportedList() {
		assertEquals(TweakifyModConfig.SUPPORTED_BLOCK_OUTLINE_COLOR_MODES,
			Arrays.stream(BlockOutlineColorMode.values()).map(BlockOutlineColorMode::code).toList());
	}

	@Test
	void unknownCodeFallsBackToPalette() {
		assertEquals(BlockOutlineColorMode.PALETTE, BlockOutlineColorMode.fromCode("not-a-mode"));
		assertEquals(BlockOutlineColorMode.PALETTE, BlockOutlineColorMode.fromCode(null));
	}
}
