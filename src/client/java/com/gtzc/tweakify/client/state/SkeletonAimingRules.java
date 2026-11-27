package com.gtzc.tweakify.client.state;

/**
 * 骷髅瞄准修复（MC-121706）的纯函数。
 *
 * <p>只有开启修复且当前确实有目标时，才需要补一次带俯仰角的视线更新。
 */
public final class SkeletonAimingRules {
	private SkeletonAimingRules() {
	}

	public static boolean shouldForceLook(boolean enabled, boolean hasTarget) {
		return enabled && hasTarget;
	}
}
