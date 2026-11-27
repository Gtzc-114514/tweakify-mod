package com.gtzc.tweakify.client.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * MC-61489：关闭时不移动；开启后按剩余高度除以 3 下移，给底部控件留空间。
 */
class BookScreenLayoutTest {

	@Test
	void disabledKeepsVanillaPosition() {
		assertEquals(0, BookScreenLayout.verticalOffset(false, 480, 192));
		assertEquals(0, BookScreenLayout.verticalOffset(false, 200, 192));
	}

	@Test
	void enabledCentersBookWithRoomForBottomButtons() {
		assertEquals(96, BookScreenLayout.verticalOffset(true, 480, 192));
		assertEquals(69, BookScreenLayout.verticalOffset(true, 400, 192));
		assertEquals(296, BookScreenLayout.verticalOffset(true, 1080, 192));
	}

	@Test
	void exactImageHeightProducesNoOffset() {
		assertEquals(0, BookScreenLayout.verticalOffset(true, 192, 192));
		assertEquals(2, BookScreenLayout.verticalOffset(true, 200, 192));
	}
}
