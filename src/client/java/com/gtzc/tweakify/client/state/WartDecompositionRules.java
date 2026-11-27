package com.gtzc.tweakify.client.state;

import com.gtzc.tweakify.config.TweakifyModConfig;

/**
 * 疣块分解产出：数量夹紧到 1 ~ 9。
 */
public final class WartDecompositionRules {
	private WartDecompositionRules() {
	}

	public static int outputCount(int configured) {
		return Math.max(TweakifyModConfig.MIN_WART_DECOMPOSITION_OUTPUT,
			Math.min(TweakifyModConfig.MAX_WART_DECOMPOSITION_OUTPUT, configured));
	}
}
