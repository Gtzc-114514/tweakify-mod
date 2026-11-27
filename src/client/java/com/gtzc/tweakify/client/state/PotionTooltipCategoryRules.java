package com.gtzc.tweakify.client.state;

import net.minecraft.world.effect.MobEffectCategory;

/**
 * 药水效果分类修复（MC-84661 / MC-170462）的纯函数。
 *
 * <p>26.3 的「发光」与「不祥之兆」仍是 NEUTRAL，而 NEUTRAL 的 tooltip 颜色是蓝色，
 * 看起来像正面效果。只在药水类物品的 tooltip 中把它们改为 HARMFUL（红色），
 * 不改动效果本身的全局分类。
 */
public final class PotionTooltipCategoryRules {
	private PotionTooltipCategoryRules() {
	}

	public static MobEffectCategory tooltipCategory(boolean enabled, boolean glowing, boolean badOmen,
			MobEffectCategory original) {
		return tooltipCategory(enabled, glowing, badOmen, true, true, original);
	}

	// 带子开关的版本：只修正启用的效果类型。
	public static MobEffectCategory tooltipCategory(boolean enabled, boolean glowing, boolean badOmen,
			boolean fixGlowing, boolean fixBadOmen, MobEffectCategory original) {
		boolean fix = (glowing && fixGlowing) || (badOmen && fixBadOmen);
		if (!enabled || !fix) {
			return original;
		}
		return MobEffectCategory.HARMFUL;
	}
}
