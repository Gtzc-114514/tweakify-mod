package com.gtzc.tweakify.client.state;

/**
 * 经过可攀爬方块不减速：可限定只在潜行时生效。
 */
public final class ClimbablePassState {
	private ClimbablePassState() {
	}

	public static boolean shouldSkipClamp(boolean enabled, boolean sneakOnly, boolean sneaking) {
		return enabled && (!sneakOnly || sneaking);
	}

	// 当前可攀爬方块是否属于启用的类型；藤蔓类指除梯子/活板门以外的可攀爬方块。
	public static boolean allowsBlock(boolean ladder, boolean vine, boolean trapdoor,
									 boolean allowLadders, boolean allowVines, boolean allowTrapdoors) {
		return (ladder && allowLadders) || (vine && allowVines) || (trapdoor && allowTrapdoors);
	}
}
