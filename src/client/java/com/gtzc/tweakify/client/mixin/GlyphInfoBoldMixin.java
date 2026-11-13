package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.font.GlyphInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 更好的粗体：原版粗体通过把字形向右偏移 1.0 像素实现，容易显得过粗/粘连。
 * 与 MCPatcher/OptiFine 一致改为 0.5 像素。
 */
@Mixin(GlyphInfo.class)
public interface GlyphInfoBoldMixin {
	@ModifyReturnValue(method = "getBoldOffset", at = @At("RETURN"))
	default float tweakify$betterBold(float original) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		return config != null && config.fix.betterBold ? 0.5F : original;
	}
}
