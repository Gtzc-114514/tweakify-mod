package com.gtzc.tweakify.client;

/**
 * 禁用“附魔过于昂贵”的取走判定：只有开关开启、输出槽确有结果且花费为正时才放行。
 * 原版 mayPickup 的 hasStack 参数不能省略，否则空输出槽也可能被当作可取出。
 */
public final class AnvilPickupState {
	private AnvilPickupState() {
	}

	// 纯判定：便于单测覆盖“空槽 / 零花费 / 开关关闭”三种边界。
	public static boolean shouldAllowPickup(boolean enabled, boolean hasStack, int cost) {
		return enabled && hasStack && cost > 0;
	}
}
