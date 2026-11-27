package com.gtzc.tweakify.client.ui;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.mojang.blaze3d.platform.InputConstants;

/**
 * 快速滚动（通用标签）：按住 Shift 滚动时把屏幕滚轮增量放大。
 *
 * <p>放大点选在 {@code MouseHandler.onScroll} 分发给 {@code Screen.mouseScrolled} 的那一个
 * 参数上，因此只影响界面滚动；快捷栏切换、旁观者菜单等不经过 Screen 的滚轮分支保持原版。
 *
 * <p>同时作用于本模组的平滑滚动与未开启平滑滚动的原版界面，两者互不依赖。
 */
public final class FastScroll {
	private FastScroll() {
	}

	// 纯逻辑入口：开关、倍率与 Shift 状态都由调用方给出，便于单测直接验证。
	public static double scaleAmount(double amount, boolean enabled, int multiplier, boolean shiftDown) {
		if (!enabled || !shiftDown || amount == 0.0D) {
			return amount;
		}
		int clamped = Math.max(TweakifyModConfig.MIN_FAST_SCROLL_MULTIPLIER,
			Math.min(TweakifyModConfig.MAX_FAST_SCROLL_MULTIPLIER, multiplier));
		return amount * clamped;
	}

	// 直接读物理左右 Shift：与功能名称一致，不受潜行键重绑影响。
	public static boolean isShiftDown() {
		return InputConstants.isKeyDown(InputConstants.KEY_LSHIFT)
			|| InputConstants.isKeyDown(InputConstants.KEY_RSHIFT);
	}

	// 运行时入口：从配置与键盘状态取参数。
	public static double scaleCurrent(double amount) {
		TweakifyModConfig config = TweakifyMod.config();
		return scaleAmount(amount, config.general.fastScroll,
			config.general.fastScrollMultiplier, isShiftDown());
	}
}
