package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.state.SpearFixes;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 生物长矛刺击/蓄力的附魔来源修复（MC-302732 / MC-304406 / MC-308272）。
 *
 * <p>{@code LivingEntity.stabAttack} 的伤害附魔已经使用武器槽物品，但击退与
 * 命中后效果仍走 {@code getWeaponItem()}（主手）。这里同样改为实际攻击槽位的物品。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntitySpearEnchantMixin {
	@WrapOperation(method = "stabAttack",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/LivingEntity;getKnockback("
				+ "Lnet/minecraft/world/entity/Entity;"
				+ "Lnet/minecraft/world/damagesource/DamageSource;)F"))
	private float tweakify$weaponKnockbackEnchant(LivingEntity instance, Entity target, DamageSource source,
												  Operation<Float> original,
												  @Local(ordinal = 0) ItemStack weaponItem) {
		if (!SpearFixes.knockbackEnchantFromWeapon()) {
			return original.call(instance, target, source);
		}
		return SpearFixes.enchantKnockback(instance, weaponItem, target, source);
	}

	@WrapOperation(method = "stabAttack",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;doPostAttackEffects("
				+ "Lnet/minecraft/server/level/ServerLevel;"
				+ "Lnet/minecraft/world/entity/Entity;"
				+ "Lnet/minecraft/world/damagesource/DamageSource;)V"))
	private void tweakify$weaponPostEffects(ServerLevel level, Entity victim, DamageSource source,
											Operation<Void> original,
											@Local(ordinal = 0) ItemStack weaponItem) {
		if (!SpearFixes.postEffectsFromWeapon()) {
			original.call(level, victim, source);
			return;
		}
		EnchantmentHelper.doPostAttackEffectsWithItemSource(level, victim, source, weaponItem);
	}
}
