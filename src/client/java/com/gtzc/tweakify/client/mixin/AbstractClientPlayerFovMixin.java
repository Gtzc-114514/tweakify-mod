package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 禁用疾跑 FOV 变化：原版冲刺会附加 +30% 移速修饰符，
 * getFieldOfViewModifier 用移速比计算视野，于是冲刺时 FOV 被抬高 0.15。
 * 这里在返回值上扣掉这一部分，保留速度药水、飞行、拉弓等其它视野效果。
 */
@Mixin(AbstractClientPlayer.class)
public class AbstractClientPlayerFovMixin {
	private static final float SPRINT_FOV_BONUS = 0.15f;

	@ModifyReturnValue(method = "getFieldOfViewModifier", at = @At("RETURN"))
	private float tweakify$disableSprintFov(float original, boolean firstPerson, float effectScale) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
		if (config == null || !config.modified.disableSprintFov || !self.isSprinting() || self.isScoping()) {
			return original;
		}
		if (self.isUsingItem() && self.getUseItem().is(Items.BOW)) {
			return original;
		}
		return Math.max(0.1f, original - effectScale * SPRINT_FOV_BONUS);
	}
}
