package com.gtzc.tweakify.client.state;

import com.gtzc.tweakify.config.TweakifyModConfig;

import java.util.List;

/**
 * 丢弃确认状态机：第一次命中记录物品与时间，确认窗口内再次命中同一物品才放行。
 *
 * <p>只保存“待确认”一项，不持有 ItemStack：切换物品、关闭功能或超时后重新提示，
 * 避免拿着旧物品时误放行新物品。窗口边界由调用方传入的毫秒时间决定，便于单测。
 */
public final class DropConfirmState {
	private static boolean pending;
	private static String pendingItemId;
	private static long pendingAtMillis;

	private DropConfirmState() {
	}

	public enum Decision {
		/** 不拦截：功能关闭、物品为空或不在名单内。 */
		IGNORE,
		/** 第一次按键：拦截本次丢弃并提示确认。 */
		BLOCK,
		/** 确认窗口内的第二次按键：放行本次丢弃。 */
		ALLOW
	}

	// 物品是否命中名单：空列表表示所有物品。
	public static boolean matchesList(String itemId, List<String> items) {
		return items == null || items.isEmpty() || (itemId != null && items.contains(itemId));
	}

	// 两次按键是否在确认窗口内。
	public static boolean withinWindow(long firstMillis, long nowMillis) {
		long elapsed = nowMillis - firstMillis;
		return elapsed >= 0 && elapsed <= TweakifyModConfig.DROP_CONFIRM_WINDOW_MILLIS;
	}

	// 处理一次丢弃请求；nowMillis 由调用方传入，便于覆盖窗口边界。
	public static Decision evaluate(boolean enabled, List<String> items, String itemId, long nowMillis) {
		if (!enabled) {
			reset();
			return Decision.IGNORE;
		}
		if (itemId == null || itemId.isEmpty() || !matchesList(itemId, items)) {
			// 名单外物品保持原版行为；待确认物品已被换下时清掉状态。
			if (pending && !itemIdEquals(itemId)) {
				reset();
			}
			return Decision.IGNORE;
		}
		if (pending && itemIdEquals(itemId) && withinWindow(pendingAtMillis, nowMillis)) {
			reset();
			return Decision.ALLOW;
		}
		pending = true;
		pendingItemId = itemId;
		pendingAtMillis = nowMillis;
		return Decision.BLOCK;
	}

	public static Decision evaluate(boolean enabled, List<String> items, String itemId) {
		return evaluate(enabled, items, itemId, System.currentTimeMillis());
	}

	public static void reset() {
		pending = false;
		pendingItemId = null;
		pendingAtMillis = 0L;
	}

	// 包私有：测试检查当前是否处于待确认状态。
	static boolean isPendingForTest() {
		return pending;
	}

	private static boolean itemIdEquals(String itemId) {
		return pendingItemId != null && pendingItemId.equals(itemId);
	}
}
