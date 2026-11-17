package com.gtzc.tweakify.client;

/**
 * 潜行动画重复修复（MC-159163）的纯函数。
 *
 * <p>只有开启修复且数据包目标就是本地玩家时，才需要从同步数据中移除 POSE；
 * 其它实体仍按原版接收姿态更新。
 */
public final class SneakAnimationState {
	private SneakAnimationState() {
	}

	public static boolean shouldStripPose(boolean enabled, boolean localPlayer) {
		return enabled && localPlayer;
	}
}
