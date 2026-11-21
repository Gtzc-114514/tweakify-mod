package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ScreenshotFormatTest {
	@Test
	void codesMapToExtensionsAndUnknownFallsBackToPng() {
		assertEquals("png", ScreenshotFormat.PNG.extension());
		assertEquals("jpg", ScreenshotFormat.JPG.extension());
		assertEquals("tga", ScreenshotFormat.TGA.extension());
		assertEquals("bmp", ScreenshotFormat.BMP.extension());
		assertSame(ScreenshotFormat.JPG, ScreenshotFormat.fromCode("jpg"));
		assertSame(ScreenshotFormat.PNG, ScreenshotFormat.fromCode("unknown"));
	}
}
