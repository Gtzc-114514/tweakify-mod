package com.gtzc.tweakify.client;

/**
 * 可见交易状态：合并锁定交易时的“原始交易数量”口径。
 * 只显示锁定交易时按 0 计算，客户端会把所有行都当作锁定项并禁用点击。
 */
public final class VisibleTradersState {
	private VisibleTradersState() {
	}

	public static int originalCount(boolean onlyLocked, int originalSize) {
		return onlyLocked ? 0 : Math.max(0, originalSize);
	}
}
