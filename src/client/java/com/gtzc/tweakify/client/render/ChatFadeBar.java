package com.gtzc.tweakify.client.render;

import net.minecraft.util.Mth;

/**
 * 聊天消息消失进度条的纯计算。
 *
 * <p>原版聊天消息的淡出由 {@code ChatComponent.AlphaCalculator.timeBased} 决定：
 * 200 tick 后完全不可见，最后 20 tick 快速淡出。进度条按同一条时间线缩短，
 * 颜色与消息透明度同步，避免出现“条还在、字已经没了”的错位。
 */
public final class ChatFadeBar {
	// 与原版 timeBased 的 200 tick 生命周期保持一致。
	public static final int MESSAGE_LIFETIME_TICKS = 200;
	// 进度条高度：贴着消息底边的一条线。
	public static final int BAR_HEIGHT = 1;

	private ChatFadeBar() {
	}

	// 剩余可见比例：1 = 刚出现，0 = 已消失。
	public static float remaining(int ageTicks) {
		return Mth.clamp(1.0F - (float) Math.max(0, ageTicks) / MESSAGE_LIFETIME_TICKS, 0.0F, 1.0F);
	}

	// 与消息文字相同的淡出曲线（原版 clamp((1 - age/200) * 10, 0, 1) 的平方）。
	public static float fadeAlpha(int ageTicks) {
		float value = Mth.clamp(1.0F - (float) Math.max(0, ageTicks) / MESSAGE_LIFETIME_TICKS, 0.0F, 1.0F);
		value = Mth.clamp(value * 10.0F, 0.0F, 1.0F);
		return value * value;
	}

	// 进度条透明度：聊天界面关闭时跟随文字淡出；打开时原版文字恒亮（FULLY_VISIBLE），
	// 进度条也只保留长度倒计时，透明度固定为完全不透明。
	public static float barAlpha(int ageTicks, boolean fullyVisible) {
		return fullyVisible ? 1.0F : fadeAlpha(ageTicks);
	}

	// 进度条右端：从背景左边界起按剩余比例缩短，至少保留 1 像素。
	public static int barRight(int left, int fullWidth, float remaining) {
		int width = Math.max(0, fullWidth);
		if (width == 0 || remaining <= 0.0F) {
			return left;
		}
		int drawn = Math.max(1, Math.round(width * Mth.clamp(remaining, 0.0F, 1.0F)));
		return left + drawn;
	}
}
