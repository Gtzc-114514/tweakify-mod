package com.gtzc.tweakify.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 定位栏指南针的纯几何：角度归一化、可视范围截断、边缘透明度与界面门控。
 * 经验等级上移的像素带回归见 {@code LocatorBarHeadsTest}。
 */
class LocatorCompassTest {

	@Test
	void angleDifferenceNormalizesIntoHalfTurn() {
		assertEquals(0.0, LocatorCompass.angleDifference(0, 0), 1.0e-9);
		assertEquals(10.0, LocatorCompass.angleDifference(350, 0), 1.0e-9);
		assertEquals(10.0, LocatorCompass.angleDifference(-10, 0), 1.0e-9);
		assertEquals(-10.0, LocatorCompass.angleDifference(0, 350), 1.0e-9);
		assertEquals(180.0, LocatorCompass.angleDifference(0, 180), 1.0e-9);
		assertEquals(-180.0, LocatorCompass.angleDifference(0, -180), 1.0e-9);
		assertEquals(-90.0, LocatorCompass.angleDifference(810, 0), 1.0e-9);
		assertEquals(45.0, LocatorCompass.angleDifference(720, 45), 1.0e-9);
	}

	@Test
	void labelXClampsOutsideFieldOfView() {
		assertEquals(100, LocatorCompass.labelX(100, 0));
		assertEquals(145, LocatorCompass.labelX(100, 30));
		assertEquals(55, LocatorCompass.labelX(100, -30));
		assertEquals(191, LocatorCompass.labelX(100, 60));
		assertEquals(9, LocatorCompass.labelX(100, -60));
		assertNull(LocatorCompass.labelX(100, 60.0001));
		assertNull(LocatorCompass.labelX(100, -60.0001));
	}

	@Test
	void labelAlphaFadesFromCenterToEdge() {
		assertEquals(255, LocatorCompass.labelAlpha(0));
		assertEquals(191, LocatorCompass.labelAlpha(30));
		assertEquals(191, LocatorCompass.labelAlpha(-30));
		assertEquals(127, LocatorCompass.labelAlpha(60));
		assertEquals(127, LocatorCompass.labelAlpha(-60));
		assertEquals(127, LocatorCompass.labelAlpha(90), "超出视场后夹紧到边界透明度");
	}

	@Test
	void compassHidesOutsideChatAndContainerScreens() {
		assertFalse(LocatorCompass.hidesCompass(null), "无界面时显示");
		// 非 null 分支依赖 Screen 实例（构造会访问 Minecraft.getInstance()），
		// 聊天/容器例外与普通界面的隐藏交由实机回归覆盖。
	}

	@Test
	void flashbackLookupIsSafeWithoutTheMod() {
		assertFalse(LocatorCompass.isInFlashbackReplay(), "未安装 Flashback 时返回 false");
	}
}
