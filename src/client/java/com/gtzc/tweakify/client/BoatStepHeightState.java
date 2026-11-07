package com.gtzc.tweakify.client;

/**
 * 草径卡船修复的步高计算：只在开启时把船的步高抬到 1/16，
 * 其它模组或原版已经设置了更大步高时保留较大值，避免把船“压”回台阶下。
 */
public final class BoatStepHeightState {
	// 土径碰撞高度为 15/16，补上这 1/16 的差值即可让船驶上完整方块。
	public static final float DIRT_PATH_STEP = 0.0625f;

	private BoatStepHeightState() {
	}

	// 纯函数：便于单测覆盖关闭、原值为 0 与原值更大的情况。
	public static float stepHeight(boolean enabled, float original) {
		return enabled ? Math.max(original, DIRT_PATH_STEP) : original;
	}
}
