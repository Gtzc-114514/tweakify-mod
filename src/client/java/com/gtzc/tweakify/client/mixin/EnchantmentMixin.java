package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 附魔等级阿拉伯数字：原版 getFullname 使用罗马数字（I、II...），
 * 开启后返回“附魔名称 + 阿拉伯数字”。
 */
@Mixin(Enchantment.class)
public class EnchantmentMixin {
	@ModifyReturnValue(method = "getFullname", at = @At("RETURN"))
	private static Component tweakify$arabicLevel(Component original, Holder<Enchantment> enchantment, int level) {
		if (TweakifyMod.CONFIG == null || !TweakifyMod.CONFIG.modified.enchantmentLevelArabic || level <= 0) {
			return original;
		}
		return enchantment.value().description().copy()
			.append(" ").append(Component.literal(Integer.toString(level)));
	}
}
