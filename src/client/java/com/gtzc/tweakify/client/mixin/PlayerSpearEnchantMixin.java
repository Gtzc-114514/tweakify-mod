package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.state.SpearFixes;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 玩家长矛蓄力攻击的附魔来源修复（MC-302732 / MC-304406 / MC-308270）。
 *
 * <p>原版 {@code Player.stabAttack} 通过 {@code getEnchantedDamage} / {@code getKnockback}
 * 读取主手物品的附魔，副手持矛时会套用主手武器。这里改成使用实际攻击槽位的长矛。
 */
@Mixin(Player.class)
public abstract class PlayerSpearEnchantMixin {
	@WrapOperation(method = "stabAttack",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/player/Player;getEnchantedDamage("
				+ "Lnet/minecraft/world/entity/Entity;F"
				+ "Lnet/minecraft/world/damagesource/DamageSource;)F"))
	private float tweakify$weaponDamageEnchant(Player instance, Entity target, float baseDamage,
											   DamageSource source, Operation<Float> original,
											   @Local(ordinal = 0) ItemStack weaponItem) {
		if (!SpearFixes.damageEnchantFromWeapon()) {
			return original.call(instance, target, baseDamage, source);
		}
		return SpearFixes.enchantDamage(instance, weaponItem, target, source, baseDamage);
	}

	@WrapOperation(method = "stabAttack",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/player/Player;getKnockback("
				+ "Lnet/minecraft/world/entity/Entity;"
				+ "Lnet/minecraft/world/damagesource/DamageSource;)F"))
	private float tweakify$weaponKnockbackEnchant(Player instance, Entity target, DamageSource source,
												  Operation<Float> original,
												  @Local(ordinal = 0) ItemStack weaponItem) {
		if (!SpearFixes.knockbackEnchantFromWeapon()) {
			return original.call(instance, target, source);
		}
		return SpearFixes.enchantKnockback(instance, weaponItem, target, source);
	}
}
