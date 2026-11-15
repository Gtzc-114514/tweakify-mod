package com.gtzc.tweakify.client;

/**
 * 受伤变红状态：关闭红色 overlay 时决定是否保留受伤闪白。
 * 原版 getOverlayCoords 用 whiteOverlayProgress 表示闪白强度，progress 归零即为不显示任何 overlay。
 */
public final class HurtTintState {
	private HurtTintState() {
	}

	public static float whiteFlashProgress(float whiteOverlayProgress, boolean keepWhiteFlash) {
		return keepWhiteFlash ? whiteOverlayProgress : 0.0f;
	}
}
