package com.gtzc.tweakify.client.render;

import com.gtzc.tweakify.client.config.EntityHighlightColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 实体高亮的纯逻辑：闪烁相位与描边颜色映射。
 * 配置命中判定依赖客户端世界，留给实机回归；这里只固定可判别的分档。
 */
class EntityHighlightStateTest {

	@Test
	void blinkPhaseIsHalfOnHalfOff() {
		assertTrue(EntityHighlightState.isBlinkOn(0, 40));
		assertTrue(EntityHighlightState.isBlinkOn(19, 40));
		assertFalse(EntityHighlightState.isBlinkOn(20, 40));
		assertFalse(EntityHighlightState.isBlinkOn(39, 40));
		assertTrue(EntityHighlightState.isBlinkOn(40, 40), "跨周期后重新亮起");
	}

	@Test
	void blinkPeriodIsClamped() {
		assertTrue(EntityHighlightState.isBlinkOn(0, 0), "周期夹紧到最小值 10");
		assertFalse(EntityHighlightState.isBlinkOn(5, 0), "最小周期 10 的前半为 5 tick");
		assertTrue(EntityHighlightState.isBlinkOn(0, 9999), "超大周期夹紧到 200");
	}

	@Test
	void outlineColorsMapFromConfigCodes() {
		assertEquals(0, EntityHighlightColor.argbOf("vanilla"), "vanilla 交给原版队伍色");
		assertEquals(0xFFFF5555, EntityHighlightColor.argbOf("red"));
		assertEquals(0xFFFFFFFF, EntityHighlightColor.argbOf("white"));
		assertEquals(0, EntityHighlightColor.argbOf("unknown"), "未知取值回退 vanilla");
	}
}
