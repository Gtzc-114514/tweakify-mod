package com.gtzc.tweakify.client;

/**
 * 第三人称鱼线修复（MC-4490）的纯函数。
 *
 * <p>潜行时原版用 -0.1875 作为手部高度偏移，第三人称鱼线会悬在鱼竿上方；
 * 开启后在原值基础上再下移 0.1，关闭时完全保持原值。
 */
public final class FishingLineState {
	private FishingLineState() {
	}

	public static float crouchOffset(boolean enabled, float original) {
		return enabled ? original - 0.1F : original;
	}
}
