package com.gtzc.tweakify.client.state;

/**
 * 举盾受伤音效修复（MC-105068）的纯函数。
 *
 * <p>26.3 的 hurtServer 在完全格挡后仍会调用 playHurtSound，导致攻击者听到
 * 受伤音效叠在盾牌格挡音效上。只在“确实触发格挡且剩余伤害为 0”时静音，
 * 部分格挡仍有伤害时仍按原版播放。
 */
public final class ShieldHurtSoundState {
	private ShieldHurtSoundState() {
	}

	public static boolean shouldPlayHurtSound(boolean enabled, boolean blocked, float remainingDamage) {
		return shouldPlayHurtSound(enabled, false, true, blocked, remainingDamage);
	}

	// 带“仅玩家”范围的版本：范围外保持原版播放。
	public static boolean shouldPlayHurtSound(boolean enabled, boolean playersOnly, boolean isPlayer,
			boolean blocked, float remainingDamage) {
		if (!enabled || (playersOnly && !isPlayer)) {
			return true;
		}
		return !blocked || remainingDamage > 0.0F;
	}
}
