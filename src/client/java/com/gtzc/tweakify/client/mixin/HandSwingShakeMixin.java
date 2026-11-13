package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.CameraMotion;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.SwingAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 本地玩家挥手时记录一次轻微震动，并打断静止晃动的淡入。
 */
@Mixin(LivingEntity.class)
public class HandSwingShakeMixin {
	// 用返回值替换读取最终结果，避免其它模组包装 swing 时读到中间值或取消回调。
	@ModifyReturnValue(
		method = "swing(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/component/SwingAnimation;Z)Z",
		at = @At("RETURN")
	)
	private boolean tweakify$addHandSwingShake(boolean original, InteractionHand hand,
			SwingAnimation animation, boolean resetAttackStrength) {
		if (!original || !((Object) this instanceof LocalPlayer player)
			|| !player.level().isClientSide()) {
			return original;
		}
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.functions.cameraMotion) {
			return original;
		}
		CameraMotion.notifyPlayerAction();
		if (config.functions.cameraMotionScreenShake) {
			CameraMotion.addHandSwingShake();
		}
		return original;
	}
}
