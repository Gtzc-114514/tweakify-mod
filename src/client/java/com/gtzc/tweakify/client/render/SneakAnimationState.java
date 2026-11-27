package com.gtzc.tweakify.client.render;

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
		return shouldStripPose(enabled, false, localPlayer, true);
	}

	// 带“仅地面”范围的版本：空中保留原版姿态同步。
	public static boolean shouldStripPose(boolean enabled, boolean onlyOnGround,
			boolean localPlayer, boolean onGround) {
		return shouldStripPose(enabled, onlyOnGround, false, localPlayer, onGround, false);
	}

	// 可再限定只在潜行时移除 POSE。
	public static boolean shouldStripPose(boolean enabled, boolean onlyOnGround, boolean onlyWhenSneaking,
			boolean localPlayer, boolean onGround, boolean sneaking) {
		return enabled && localPlayer && (!onlyOnGround || onGround)
			&& (!onlyWhenSneaking || sneaking);
	}
}
