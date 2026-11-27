package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.state.SpearFixes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.KineticWeapon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 长矛蓄力命中修复（MC-303451 / MC-308786 / MC-310858）。
 *
 * <p>原版 {@code KineticWeapon.damageEntities} 的速度投影、击退阈值与接触冷却都在这里；
 * 任一相关子项开启时整体替换为等价实现，未开启的子项保持原版行为。
 */
@Mixin(KineticWeapon.class)
public abstract class KineticWeaponSpearFixesMixin {
	@Inject(method = "damageEntities", at = @At("HEAD"), cancellable = true)
	private void tweakify$applySpearChargeFixes(ItemStack stack, int ticksRemaining, LivingEntity attacker,
												EquipmentSlot slot, CallbackInfo ci) {
		if (!SpearFixes.chargeFixesEnabled()) {
			return;
		}
		SpearFixes.damageEntities((KineticWeapon) (Object) this, stack, ticksRemaining, attacker, slot);
		ci.cancel();
	}
}
