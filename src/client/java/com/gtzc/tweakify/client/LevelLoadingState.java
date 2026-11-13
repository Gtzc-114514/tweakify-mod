package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;

/**
 * 强制关闭加载屏幕的状态：对齐 ForceCloseLoadingScreen 的行为。
 *
 * <p>默认只跳过加载界面的文字/进度渲染（背景与世界仍在后台加载，原版加载完成后正常关屏）；
 * “保留首次加载界面”“完全关闭”和“资源包加载加速”都是独立子设置。
 */
public final class LevelLoadingState {
	private LevelLoadingState() {
	}

	// 是否跳过加载界面的渲染：功能开启且没有勾选“保留加载界面”。
	public static boolean shouldHide() {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		return config != null && config.forceCloseLoadingScreen && !config.forceCloseLoadingScreenKeepFirst;
	}
}
