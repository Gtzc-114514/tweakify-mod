package com.gtzc.tweakify.client;

import net.minecraft.world.effect.MobEffectCategory;

/**
 * 药水效果分类修复（MC-84661 / MC-170462）的纯函数。
 *
 * <p>26.3 的「发光」与「不祥之兆」仍是 NEUTRAL，而 NEUTRAL 的 tooltip 颜色是蓝色，
 * 看起来像正面效果。只在药水类物品的 tooltip 中把它们改为 HARMFUL（红色），
 * 不改动效果本身的全局分类。
 */
public final class PotionTooltipCategoryState {
	private PotionTooltipCategoryState() {
	}

	public static MobEffectCategory tooltipCategory(boolean enabled, boolean glowing, boolean badOmen,
			MobEffectCategory original) {
		if (!enabled || (!glowing && !badOmen)) {
			return original;
		}
		return MobEffectCategory.HARMFUL;
	}
}
