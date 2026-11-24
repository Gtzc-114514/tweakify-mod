package com.gtzc.tweakify.client;

/**
 * MC-19830 修复的伤害换算。
 *
 * <p>原版落在方块上时由 {@code Block.fallOn} 先按方块的下落伤害减免再结算；床的减免为
 * 50%。入睡提前结算时必须应用同一减免，否则会把伤害翻倍。
 */
public final class BedFallDamageFix {
	private BedFallDamageFix() {
	}

	public static float damageDistance(double fallDistance, float fallDistanceReduction) {
		float reduction = Math.max(0.0F, Math.min(1.0F, fallDistanceReduction));
		return (float) (fallDistance * (1.0 - reduction));
	}
}
