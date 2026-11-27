package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.state.TotemInventory;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 不死图腾背包生效：原版 checkTotemDeathProtection 只遍历主手/副手；
 * 双手都没有图腾时，从快捷栏与主背包补一个，效果与消耗沿用原版逻辑。
 */
@Mixin(LivingEntity.class)
public abstract class TotemInventoryMixin {
	@Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true)
	private void tweakify$useInventoryTotem(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
		TweakifyModConfig config = TweakifyMod.config();
		if (!config.modified.totemFromInventory
			|| source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return;
		}
		LivingEntity self = (LivingEntity) (Object) this;
		if (!(self instanceof Player player)) {
			return;
		}
		// 手部有图腾时保持原版优先：原版会先消耗手中的图腾。
		for (InteractionHand hand : InteractionHand.values()) {
			if (self.getItemInHand(hand).get(DataComponents.DEATH_PROTECTION) != null) {
				return;
			}
		}
		if (TotemInventory.tryUseInventoryTotem(player)) {
			cir.setReturnValue(true);
		}
	}
}
