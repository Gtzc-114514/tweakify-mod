package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.PotionTooltipCategoryState;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.alchemy.PotionContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MC-84661 / MC-170462：药水类物品 tooltip 把「发光」与「不祥之兆」当成中性效果，
 * 而 26.3 的中性分类颜色是蓝色，看起来像正面效果。
 *
 * <p>只改 addPotionTooltip 里用于着色的 getCategory 返回值，不改 MobEffects 的全局分类，
 * 因此不会影响效果本身的游戏逻辑；配置关闭时返回原分类。
 */
@Mixin(PotionContents.class)
public abstract class PotionContentsTooltipCategoryMixin {
	@Unique
	private static final AtomicBoolean TWEAKIFY$GLOWING_TOOLTIP_PROBE = new AtomicBoolean();
	@Unique
	private static final AtomicBoolean TWEAKIFY$BAD_OMEN_TOOLTIP_PROBE = new AtomicBoolean();

	@ModifyExpressionValue(method = "addPotionTooltip",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/effect/MobEffect;getCategory()"
				+ "Lnet/minecraft/world/effect/MobEffectCategory;"),
		require = 0)
	private static MobEffectCategory tweakify$fixTooltipCategory(
			MobEffectCategory original, @Local MobEffectInstance effect) {
		TweakifyModConfig config = TweakifyMod.config();
		boolean enabled = config != null && config.fix.potionTooltipCategoryFix;
		boolean glowing = effect.is(MobEffects.GLOWING);
		boolean badOmen = effect.is(MobEffects.BAD_OMEN);
		boolean fixGlowing = config == null || config.fix.potionTooltipCategoryGlowing;
		boolean fixBadOmen = config == null || config.fix.potionTooltipCategoryBadOmen;
		if (enabled) {
			if (glowing) {
				HookProbe.markOnce(TWEAKIFY$GLOWING_TOOLTIP_PROBE, "potionTooltipCategoryGlowing");
			}
			if (badOmen) {
				HookProbe.markOnce(TWEAKIFY$BAD_OMEN_TOOLTIP_PROBE, "potionTooltipCategoryBadOmen");
			}
		}
		return PotionTooltipCategoryState.tooltipCategory(enabled, glowing, badOmen,
			fixGlowing, fixBadOmen, original);
	}
}
