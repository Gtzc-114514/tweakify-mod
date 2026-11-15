package com.gtzc.tweakify.client;

/**
 * 第三人称自身名字的纯判定：配置、本地玩家以及第一人称/隐藏 HUD/隐身/旁观边界。
 */
public final class SelfNameVisibility {
	private SelfNameVisibility() {
	}

	public static boolean shouldShowSelfName(boolean enabled, boolean localPlayer,
											boolean firstPerson, boolean hudHidden,
											boolean invisible, boolean spectator) {
		return shouldShowSelfName(enabled, localPlayer, firstPerson, hudHidden, invisible, spectator,
			false, false);
	}

	// 新增潜行门控：开启“仅潜行时显示”后，未潜行不显示。
	public static boolean shouldShowSelfName(boolean enabled, boolean localPlayer,
											boolean firstPerson, boolean hudHidden,
											boolean invisible, boolean spectator,
											boolean sneakOnly, boolean sneaking) {
		return enabled && localPlayer && !firstPerson && !hudHidden
			&& !invisible && !spectator && (!sneakOnly || sneaking);
	}
}
