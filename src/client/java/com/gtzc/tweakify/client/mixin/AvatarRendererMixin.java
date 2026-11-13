package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 更好的隐身：玩家渲染器自己重写了 shouldRenderLayers（只判断旁观者），
 * 父类上的注入不会生效，这里单独覆盖玩家路径，让第三人称装备与手持物品一起隐藏。
 */
@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {
	@ModifyReturnValue(method = "shouldRenderLayers(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)Z",
		at = @At("RETURN"))
	private boolean tweakify$hideLayersWhenInvisible(boolean original, AvatarRenderState state) {
		if (TweakifyMod.CONFIG != null && TweakifyMod.CONFIG.modified.betterInvisibility
			&& TweakifyMod.CONFIG.modified.betterInvisibilityLayers && state.isInvisible) {
			return false;
		}
		return original;
	}
}
