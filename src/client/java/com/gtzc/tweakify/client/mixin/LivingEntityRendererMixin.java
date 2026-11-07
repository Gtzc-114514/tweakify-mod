package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
	// 更好的隐身：隐身时连装备、手持物品、披风等附加层一起隐藏（原版只隐藏身体）。
	@Inject(method = "shouldRenderLayers", at = @At("HEAD"), cancellable = true)
	private void tweakify$hideLayersWhenInvisible(LivingEntityRenderState state,
												  CallbackInfoReturnable<Boolean> cir) {
		if (TweakifyMod.CONFIG != null && TweakifyMod.CONFIG.betterInvisibility
			&& TweakifyMod.CONFIG.betterInvisibilityLayers && state.isInvisible) {
			cir.setReturnValue(false);
		}
	}

	// hasRedOverlay 同时覆盖受伤（hurtTime > 0）与死亡（deathTime > 0），
	// 这里只去掉受伤部分：死亡后的红色 overlay 保持原版。
	@Inject(method = "getOverlayCoords", at = @At("HEAD"), cancellable = true)
	private static void tweakify$disableHurtTint(LivingEntityRenderState state, float whiteOverlayProgress,
												CallbackInfoReturnable<Integer> cir) {
		if (TweakifyMod.CONFIG == null || !TweakifyMod.CONFIG.disableHurtTint
			|| !state.hasRedOverlay || state.deathTime > 0.0f) {
			return;
		}
		// 保留白色 overlay 进度（受伤闪白等），仅关闭红色 overlay。
		cir.setReturnValue(OverlayTexture.pack(whiteOverlayProgress, false));
	}
}
