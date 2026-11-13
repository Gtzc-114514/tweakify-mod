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
		return enabled && localPlayer && !firstPerson && !hudHidden
			&& !invisible && !spectator;
	}
}
