package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 禁用疾跑 FOV 变化：原版冲刺会附加 +30% 移速修饰符，
 * getFieldOfViewModifier 用移速比计算视野，于是冲刺时 FOV 被抬高 0.15。
 * 这里在返回值上扣掉这一部分，保留速度药水、飞行、拉弓等其它视野效果。
 */
@Mixin(AbstractClientPlayer.class)
public class AbstractClientPlayerFovMixin {
	private static final float SPRINT_FOV_BONUS = 0.15f;

	@Inject(method = "getFieldOfViewModifier", at = @At("RETURN"), cancellable = true)
	private void tweakify$disableSprintFov(boolean firstPerson, float effectScale,
										   CallbackInfoReturnable<Float> cir) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
		if (config == null || !config.disableSprintFov || !self.isSprinting() || self.isScoping()) {
			return;
		}
		if (self.isUsingItem() && self.getUseItem().is(Items.BOW)) {
			return;
		}
		cir.setReturnValue(Math.max(0.1f, cir.getReturnValueF() - effectScale * SPRINT_FOV_BONUS));
	}
}
