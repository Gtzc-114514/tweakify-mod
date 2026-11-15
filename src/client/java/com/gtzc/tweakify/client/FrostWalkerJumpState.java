package com.gtzc.tweakify.client;

/**
 * 冰霜行者跳跃修复的放宽条件：只处理主动跳跃（可选）且未骑乘、未落地的玩家。
 */
public final class FrostWalkerJumpState {
	private FrostWalkerJumpState() {
	}

	public static boolean shouldRelax(boolean enabled, boolean jumpOnly, boolean passenger,
									  boolean onGround, double verticalSpeed) {
		if (!enabled || passenger || onGround) {
			return false;
		}
		return !jumpOnly || verticalSpeed > 0.0;
	}
}
