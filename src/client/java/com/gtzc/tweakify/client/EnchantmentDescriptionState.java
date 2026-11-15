package com.gtzc.tweakify.client;

/**
 * 附魔描述显示条件：开启功能后，可再要求按住潜行键才显示。
 */
public final class EnchantmentDescriptionState {
	private EnchantmentDescriptionState() {
	}

	public static boolean shouldAppend(boolean enabled, boolean sneakOnly, boolean sneaking) {
		return enabled && (!sneakOnly || sneaking);
	}
}
