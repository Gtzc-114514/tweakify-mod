package com.gtzc.tweakify.client.state;

/**
 * 附魔描述显示条件：开启功能后，可再要求按住潜行键才显示。
 */
public final class EnchantmentDescriptionRules {
	private EnchantmentDescriptionRules() {
	}

	public static boolean shouldAppend(boolean enabled, boolean sneakOnly, boolean sneaking) {
		return enabled && (!sneakOnly || sneaking);
	}
}
