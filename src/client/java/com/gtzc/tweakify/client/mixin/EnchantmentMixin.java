package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 附魔等级阿拉伯数字：原版 getFullname 使用罗马数字（I、II...），
 * 开启后返回“附魔名称 + 阿拉伯数字”。
 */
@Mixin(Enchantment.class)
public class EnchantmentMixin {
	@Inject(method = "getFullname", at = @At("HEAD"), cancellable = true)
	private static void tweakify$arabicLevel(Holder<Enchantment> enchantment, int level,
											 CallbackInfoReturnable<Component> cir) {
		if (TweakifyMod.CONFIG == null || !TweakifyMod.CONFIG.enchantmentLevelArabic || level <= 0) {
			return;
		}
		cir.setReturnValue(enchantment.value().description().copy()
			.append(" ").append(Component.literal(Integer.toString(level))));
	}
}
