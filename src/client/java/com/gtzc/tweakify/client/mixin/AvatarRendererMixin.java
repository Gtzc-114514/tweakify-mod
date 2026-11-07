package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 更好的隐身：玩家渲染器自己重写了 shouldRenderLayers（只判断旁观者），
 * 父类上的注入不会生效，这里单独覆盖玩家路径，让第三人称装备与手持物品一起隐藏。
 */
@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {
	@Inject(method = "shouldRenderLayers(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)Z",
		at = @At("HEAD"), cancellable = true)
	private void tweakify$hideLayersWhenInvisible(AvatarRenderState state,
												  CallbackInfoReturnable<Boolean> cir) {
		if (TweakifyMod.CONFIG != null && TweakifyMod.CONFIG.betterInvisibility
			&& TweakifyMod.CONFIG.betterInvisibilityLayers && state.isInvisible) {
			cir.setReturnValue(false);
		}
	}
}
