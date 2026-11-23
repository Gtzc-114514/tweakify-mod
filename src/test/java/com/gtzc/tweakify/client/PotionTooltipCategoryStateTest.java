package com.gtzc.tweakify.client;

import net.minecraft.world.effect.MobEffectCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * MC-84661 / MC-170462：只在开启时把发光与不祥之兆改为有害分类；其它效果与关闭状态保持原值。
 */
class PotionTooltipCategoryStateTest {

	@Test
	void disabledKeepsOriginalCategory() {
		assertEquals(MobEffectCategory.NEUTRAL, PotionTooltipCategoryState.tooltipCategory(
			false, true, true, MobEffectCategory.NEUTRAL));
		assertEquals(MobEffectCategory.BENEFICIAL, PotionTooltipCategoryState.tooltipCategory(
			false, false, false, MobEffectCategory.BENEFICIAL));
	}

	@Test
	void enabledMapsGlowingAndBadOmenToHarmful() {
		assertEquals(MobEffectCategory.HARMFUL, PotionTooltipCategoryState.tooltipCategory(
			true, true, false, MobEffectCategory.NEUTRAL));
		assertEquals(MobEffectCategory.HARMFUL, PotionTooltipCategoryState.tooltipCategory(
			true, false, true, MobEffectCategory.NEUTRAL));
	}

	@Test
	void eachEffectCanBeDisabledSeparately() {
		assertEquals(MobEffectCategory.NEUTRAL, PotionTooltipCategoryState.tooltipCategory(
			true, true, false, false, true, MobEffectCategory.NEUTRAL));
		assertEquals(MobEffectCategory.NEUTRAL, PotionTooltipCategoryState.tooltipCategory(
			true, false, true, true, false, MobEffectCategory.NEUTRAL));
		assertEquals(MobEffectCategory.HARMFUL, PotionTooltipCategoryState.tooltipCategory(
			true, true, false, true, false, MobEffectCategory.NEUTRAL));
	}

	@Test
	void enabledLeavesOtherEffectsUnchanged() {
		assertEquals(MobEffectCategory.BENEFICIAL, PotionTooltipCategoryState.tooltipCategory(
			true, false, false, MobEffectCategory.BENEFICIAL));
		assertEquals(MobEffectCategory.HARMFUL, PotionTooltipCategoryState.tooltipCategory(
			true, false, false, MobEffectCategory.HARMFUL));
		assertEquals(MobEffectCategory.NEUTRAL, PotionTooltipCategoryState.tooltipCategory(
			true, false, false, MobEffectCategory.NEUTRAL));
	}
}
