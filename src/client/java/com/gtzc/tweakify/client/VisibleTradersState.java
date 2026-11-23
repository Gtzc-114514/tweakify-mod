package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;

/**
 * 可见交易状态：合并锁定交易时的“原始交易数量”口径，
 * 以及展示用的未来等级数与锁定条数上限。
 * 只显示锁定交易时按 0 计算，客户端会把所有行都当作锁定项并禁用点击。
 */
public final class VisibleTradersState {
	private VisibleTradersState() {
	}

	public static int originalCount(boolean onlyLocked, int originalSize) {
		return onlyLocked ? 0 : Math.max(0, originalSize);
	}

	// 实际展示的未来等级组数：配置值夹到 1~4，再受剩余组数限制。
	public static int futureLevelsToShow(int configured, int available) {
		int limit = Math.max(TweakifyModConfig.MIN_VISIBLE_TRADER_FUTURE_LEVELS,
			Math.min(TweakifyModConfig.MAX_VISIBLE_TRADER_FUTURE_LEVELS, configured));
		return Math.min(limit, Math.max(0, available));
	}

	// 实际展示的锁定交易条数：0 = 不限制；否则夹到 1~64 并受实际条数限制。
	public static int limitLockedOffers(int configured, int size) {
		int safeSize = Math.max(0, size);
		if (configured <= 0) {
			return safeSize;
		}
		int limit = Math.min(TweakifyModConfig.MAX_VISIBLE_TRADER_LOCKED_OFFERS, configured);
		return Math.min(limit, safeSize);
	}
}
