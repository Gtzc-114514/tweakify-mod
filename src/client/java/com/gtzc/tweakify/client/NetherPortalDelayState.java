package com.gtzc.tweakify.client;

/**
 * 地狱门传送延迟的纯判定：返回 {@link #NO_OVERRIDE} 表示保持原版规则。
 *
 * <p>原版用 {@code Abilities.invulnerable} 区分创造/旁观与生存/冒险：
 * 前者取创造延迟（默认 0），后者取生存延迟（默认 80 tick）。
 */
public final class NetherPortalDelayState {
	// 不覆盖原版返回值：调用方据此决定是否取消注入。
	public static final int NO_OVERRIDE = -1;

	private NetherPortalDelayState() {
	}

	public static int overrideDelay(boolean masterEnabled, boolean survivalInstant, boolean creativeDelayed,
									boolean isPlayer, boolean invulnerable, int survivalDelay) {
		if (!masterEnabled || !isPlayer) {
			return NO_OVERRIDE;
		}
		if (invulnerable) {
			// 创造模式改为等待生存模式的延迟，不再秒传。
			return creativeDelayed ? Math.max(0, survivalDelay) : NO_OVERRIDE;
		}
		// 生存模式取消等待，像创造模式一样立即传送。
		return survivalInstant ? 0 : NO_OVERRIDE;
	}
}
