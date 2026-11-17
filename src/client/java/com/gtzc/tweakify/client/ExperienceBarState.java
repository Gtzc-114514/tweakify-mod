package com.gtzc.tweakify.client;

/**
 * 经验条高等级修复（MC-79545）的纯函数。
 *
 * <p>原版在等级极高时 {@code getXpNeededForNextLevel()} 会整数溢出为负数，
 * 而 {@code ExperienceBar.extractBackground} 只在返回值大于 0 时绘制经验条，
 * 于是整条经验条消失。这里把非正值提升到 1，保证背景与进度条继续绘制。
 */
public final class ExperienceBarState {
	private ExperienceBarState() {
	}

	// 关闭时原样返回；开启时只修掉“小于等于 0 就整条不画”的分支。
	public static int xpNeededForNextLevel(boolean enabled, int xpNeeded) {
		if (!enabled) {
			return xpNeeded;
		}
		// 不能夹到 0：原版同样会跳过绘制，至少保留 1 才能让经验条可见。
		return Math.max(1, xpNeeded);
	}
}
