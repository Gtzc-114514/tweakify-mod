package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.LlamaDecorLayer;
import net.minecraft.client.renderer.entity.state.LlamaRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 更好的隐身：羊驼隐身时，背上的地毯（装饰层）也一起隐藏。
 *
 * <p>羊驼地毯不走 shouldRenderLayers，所以需要单独拦截这一层。
 */
@Mixin(LlamaDecorLayer.class)
public class LlamaDecorLayerMixin {
	@Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;"
		+ "Lnet/minecraft/client/renderer/SubmitNodeCollector;"
		+ "ILnet/minecraft/client/renderer/entity/state/LlamaRenderState;FF)V",
		at = @At("HEAD"), cancellable = true)
	private void tweakify$hideCarpetWhenInvisible(PoseStack pose, SubmitNodeCollector collector, int light,
												  LlamaRenderState state, float limbAngle, float limbDistance,
												  CallbackInfo ci) {
		if (TweakifyMod.CONFIG != null && TweakifyMod.CONFIG.modified.betterInvisibility
			&& TweakifyMod.CONFIG.modified.betterInvisibilityLlamaCarpet && state.isInvisible) {
			ci.cancel();
		}
	}
}
