package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 告示牌文本优化：只改写告示牌渲染读取的文本过滤开关，不影响聊天、书本等其它文本。
 */
@Mixin(AbstractSignRenderer.class)
public class SignTextRenderFilterMixin {
	@ModifyExpressionValue(method = "extractRenderState", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/Minecraft;isTextFilteringEnabled()Z"))
	private boolean tweakify$disableSignTextFiltering(boolean original) {
		return TweakifyMod.config().optimization.signTextOptimization
			&& TweakifyMod.config().optimization.signTextOptimizationSkipFiltering
			? false
			: original;
	}
}
