package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 聊天消息消失进度条的时间线与绘制宽度。
 */
class ChatFadeBarTest {
	@Test
	void remainingShrinksLinearlyToZero() {
		assertEquals(1.0F, ChatFadeBar.remaining(0), 1.0e-5F);
		assertEquals(0.5F, ChatFadeBar.remaining(100), 1.0e-5F);
		assertEquals(0.0F, ChatFadeBar.remaining(200), 1.0e-5F);
		assertEquals(0.0F, ChatFadeBar.remaining(260), 1.0e-5F);
	}

	@Test
	void fadeAlphaMatchesVanillaTimeBasedCurve() {
		// 原版前 180 tick 完全可见，最后 20 tick 快速淡出。
		assertEquals(1.0F, ChatFadeBar.fadeAlpha(0), 1.0e-5F);
		assertEquals(1.0F, ChatFadeBar.fadeAlpha(180), 1.0e-5F);
		assertEquals(0.25F, ChatFadeBar.fadeAlpha(190), 1.0e-5F);
		assertEquals(0.0F, ChatFadeBar.fadeAlpha(200), 1.0e-5F);
	}

	@Test
	void barAlphaFollowsMessageOnlyWhenChatIsClosed() {
		// 聊天界面关闭：文字走 timeBased 淡出，进度条同步。
		assertEquals(ChatFadeBar.fadeAlpha(190), ChatFadeBar.barAlpha(190, false), 1.0e-5F);
		// 聊天界面打开：原版文字 FULLY_VISIBLE，进度条保持完全不透明，只有长度倒计时。
		assertEquals(1.0F, ChatFadeBar.barAlpha(190, true), 1.0e-5F);
		assertEquals(1.0F, ChatFadeBar.barAlpha(0, true), 1.0e-5F);
	}

	@Test
	void barWidthNeverExceedsBackgroundAndKeepsVisiblePixel() {
		assertEquals(-4, ChatFadeBar.barRight(-4, 100, 0.0F));
		assertEquals(-3, ChatFadeBar.barRight(-4, 100, 0.001F));
		assertEquals(46, ChatFadeBar.barRight(-4, 100, 0.5F));
		assertEquals(96, ChatFadeBar.barRight(-4, 100, 1.0F));
		assertTrue(ChatFadeBar.barRight(-4, 100, 1.5F) <= 96);
	}
}
