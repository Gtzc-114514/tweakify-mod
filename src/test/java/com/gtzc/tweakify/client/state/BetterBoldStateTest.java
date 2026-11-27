package com.gtzc.tweakify.client.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 更好的粗体偏移：关闭时保留原值；开启时按百分比换算并夹紧到 10 ~ 100。
 */
class BetterBoldStateTest {

	@Test
	void disabledKeepsOriginal() {
		assertEquals(1.0f, BetterBoldState.offset(false, 50, 1.0f), 1.0e-6f);
	}

	@Test
	void percentMapsToPixels() {
		assertEquals(0.5f, BetterBoldState.offset(true, 50, 1.0f), 1.0e-6f);
		assertEquals(1.0f, BetterBoldState.offset(true, 100, 1.0f), 1.0e-6f);
		assertEquals(0.25f, BetterBoldState.offset(true, 25, 1.0f), 1.0e-6f);
	}

	@Test
	void percentIsClamped() {
		assertEquals(1.0f, BetterBoldState.offset(true, 999, 1.0f), 1.0e-6f);
		assertEquals(0.1f, BetterBoldState.offset(true, -50, 1.0f), 1.0e-6f);
	}
}
