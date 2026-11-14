package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 调试叠加层的纯文本格式：速度行把 tick 速度换算成格每秒，
 * 水平与垂直分量分别显示，便于确认飞行/攀爬等功能是否生效。
 */
class DebugInfoOverlayTest {
	@Test
	void formatSpeedConvertsTickVelocityToBlocksPerSecond() {
		assertEquals("Speed: 4.32 / 0.00 (h/v)", DebugInfoOverlay.formatSpeed(0.216, 0.0, 0.0));
		assertEquals("Speed: 0.00 / 1.20 (h/v)", DebugInfoOverlay.formatSpeed(0.0, 0.06, 0.0));
	}

	@Test
	void formatSpeedUsesCombinedHorizontalComponent() {
		assertEquals("Speed: 5.00 / -2.00 (h/v)",
			DebugInfoOverlay.formatSpeed(0.15, -0.1, 0.2));
	}
}
