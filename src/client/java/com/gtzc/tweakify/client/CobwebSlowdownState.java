package com.gtzc.tweakify.client;

import net.minecraft.world.phys.Vec3;

/**
 * 蛛网减速的保留模式：只放开水平方向，跳跃减速沿用原版倍率。
 */
public final class CobwebSlowdownState {
	private CobwebSlowdownState() {
	}

	public static Vec3 horizontalFree(Vec3 original) {
		return new Vec3(1.0, original.y, 1.0);
	}
}
