package com.gtzc.tweakify.client.state;

import com.gtzc.tweakify.config.TweakifyModConfig;

/**
 * 锻造模板保留概率：100% 必定保留（旧行为），0% 完全按原版消耗。
 */
public final class SmithingTemplateRules {
	private SmithingTemplateRules() {
	}

	public static boolean shouldKeepTemplate(boolean enabled, int chancePercent, int roll) {
		if (!enabled) {
			return false;
		}
		int chance = Math.max(TweakifyModConfig.MIN_SMITHING_TEMPLATE_KEEP_CHANCE,
			Math.min(TweakifyModConfig.MAX_SMITHING_TEMPLATE_KEEP_CHANCE, chancePercent));
		if (chance >= TweakifyModConfig.MAX_SMITHING_TEMPLATE_KEEP_CHANCE) {
			return true;
		}
		if (chance <= 0) {
			return false;
		}
		return Math.floorMod(roll, 100) < chance;
	}
}
