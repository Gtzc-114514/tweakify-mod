package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockOutlinePaletteTest {
	@Test
	void codesMatchSupportedList() {
		assertEquals(TweakifyModConfig.SUPPORTED_BLOCK_OUTLINE_PALETTES,
			Arrays.stream(BlockOutlinePalette.values()).map(BlockOutlinePalette::code).toList());
	}

	@Test
	void unknownCodeFallsBackToWhite() {
		assertEquals(BlockOutlinePalette.WHITE, BlockOutlinePalette.fromCode("not-a-color"));
		assertEquals(BlockOutlinePalette.WHITE, BlockOutlinePalette.fromCode(null));
	}

	@Test
	void paletteColorsAreOpaque() {
		for (BlockOutlinePalette palette : BlockOutlinePalette.values()) {
			assertEquals(0xFF, (palette.argb() >>> 24) & 0xFF, "palette must be opaque: " + palette.code());
		}
	}
}
