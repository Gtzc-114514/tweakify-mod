package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 资源包加载屏幕加速（对齐 ForceCloseLoadingScreen）：
 * 去掉淡入、立即可淡出、淡出结束后直接移除覆盖层。
 */
@Mixin(LoadingOverlay.class)
public class LoadingOverlayMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Shadow
	private long fadeOutStart;

	@Shadow
	private boolean fadeIn;

	@Inject(method = "extractRenderState", at = @At("TAIL"), require = 0)
	private void tweakify$removeAfterFadeOut(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick,
											 CallbackInfo ci) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config != null && config.forceCloseLoadingScreenSkipResourcePackFade && this.fadeOutStart != -1) {
			this.minecraft.gui.setOverlay(null);
		}
	}

	@Redirect(method = "extractRenderState", require = 0, at = @At(value = "FIELD",
		target = "Lnet/minecraft/client/gui/screens/LoadingOverlay;fadeIn:Z", opcode = Opcodes.GETFIELD))
	private boolean tweakify$noFadeIn(LoadingOverlay instance) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config != null && config.forceCloseLoadingScreenSkipResourcePackFade) {
			return false;
		}
		return this.fadeIn;
	}

	@ModifyReturnValue(method = "isReadyToFadeOut", at = @At("RETURN"), require = 0)
	private boolean tweakify$readyToFadeOut(boolean original) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		return config != null && config.forceCloseLoadingScreenSkipResourcePackFade ? true : original;
	}
}
