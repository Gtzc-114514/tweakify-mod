package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.SpearFixes;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MC-305835：生物用长矛的普通刺击命中时不造成击退。
 *
 * <p>长矛伤害类型在 {@code no_knockback} 标签里，原版 {@code hurt} 会跳过头顶击退；
 * 玩家靠冲刺补上额外击退，生物没有这一步。这里在生物近战命中后补回默认 0.4 击退。
 */
@Mixin(Mob.class)
public abstract class MobSpearJabKnockbackMixin {
	@Inject(method = "doHurtTarget", at = @At("RETURN"))
	private void tweakify$spearJabKnockback(ServerLevel level, Entity target,
											CallbackInfoReturnable<Boolean> cir,
											@Local(ordinal = 0) ItemStack weaponItem,
											@Local(ordinal = 0) DamageSource damageSource,
											@Local(ordinal = 0) Vec3 oldMovement,
											@Local(ordinal = 0) float damage) {
		if (!cir.getReturnValueZ() || !SpearFixes.mc305835()) {
			return;
		}
		if (!weaponItem.has(DataComponents.KINETIC_WEAPON)) {
			return;
		}
		((Mob) (Object) this).causeExtraKnockback(target, 0.4F, oldMovement, damageSource, damage, false);
	}
}
