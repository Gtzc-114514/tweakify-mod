package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.UseCooldownState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
	@Inject(method = "itemUsed", at = @At("HEAD"))
	private void tweakify$markHandUsed(InteractionHand hand, CallbackInfo ci) {
		UseCooldownState.markHandUsed(hand);
	}

	// 使用物品（进食、拉弓、举盾等）时的移动减速倍率取自物品的 USE_EFFECTS 组件，
	// 在返回处改为 1.0 即完全取消减速，不影响潜行等其他速度修正。
	@Inject(method = "itemUseSpeedMultiplier", at = @At("RETURN"), cancellable = true)
	private void tweakify$disableUseItemSlowdown(CallbackInfoReturnable<Float> cir) {
		if (TweakifyMod.CONFIG != null && TweakifyMod.CONFIG.disableUseItemSlowdown) {
			// 例外列表里的物品保持原版减速。
			LocalPlayer self = (LocalPlayer) (Object) this;
			if (!TweakifyMod.CONFIG.isUseItemSlowdownException(self.getUseItem().getItem())) {
				cir.setReturnValue(1.0F);
			}
		}
	}
}
