package com.gtzc.tweakify.client.state;

/**
 * 禁用“附魔过于昂贵”的取走判定：只有开关开启、输出槽确有结果且花费为正时才放行。
 * 原版 mayPickup 的 hasStack 参数不能省略，否则空输出槽也可能被当作可取出。
 */
public final class AnvilPickupRules {
	private AnvilPickupRules() {
	}

	// 纯判定：便于单测覆盖“空槽 / 零花费 / 开关关闭”三种边界（不限制费用）。
	public static boolean shouldAllowPickup(boolean enabled, boolean hasStack, int cost) {
		return shouldAllowPickup(enabled, hasStack, cost, 0, false);
	}

	// 可取出：开关开启、输出槽有结果、花费为正，且未超过费用上限（0 = 不限制）或处于仅提示模式。
	public static boolean shouldAllowPickup(boolean enabled, boolean hasStack, int cost, int maxCost,
			boolean warnOnly) {
		return enabled && hasStack && cost > 0 && shouldAllowResult(enabled, cost, maxCost, warnOnly);
	}

	// createResult 与界面标签共用：是否按旧行为跳过“过于昂贵”限制。
	public static boolean shouldAllowResult(boolean enabled, int cost, int maxCost, boolean warnOnly) {
		if (!enabled) {
			return false;
		}
		return maxCost <= 0 || warnOnly || cost <= maxCost;
	}

	// 超过费用上限且不是“仅提示”时，必须阻止取出（即使玩家等级足够）。
	public static boolean shouldBlockByCost(boolean enabled, int cost, int maxCost, boolean warnOnly) {
		return enabled && !warnOnly && maxCost > 0 && cost > maxCost;
	}

	// 是否显示“过于昂贵”提示：开关开启、设了上限且花费超出；仅提示模式下同样显示。
	public static boolean shouldShowTooExpensiveLabel(boolean enabled, int cost, int maxCost) {
		return enabled && maxCost > 0 && cost > maxCost;
	}
}
