package com.gtzc.tweakify.client;

/**
 * 经过可攀爬方块不减速：可限定只在潜行时生效。
 */
public final class ClimbablePassState {
	private ClimbablePassState() {
	}

	public static boolean shouldSkipClamp(boolean enabled, boolean sneakOnly, boolean sneaking) {
		return enabled && (!sneakOnly || sneaking);
	}
}
