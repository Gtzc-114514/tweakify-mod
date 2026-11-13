package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.LevelLoadingState;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 强制关闭加载屏幕（对齐 ForceCloseLoadingScreen）：
 * 默认只跳过加载界面的文字/进度渲染（背景与世界仍在后台加载，原版加载完成后正常关屏）；
 * “完全关闭”子设置则在世界就绪后直接关闭界面。
 */
@Mixin(LevelLoadingScreen.class)
public class LevelLoadingScreenMixin {
	@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true, require = 0)
	private void tweakify$skipRender(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick,
									 CallbackInfo ci) {
		if (LevelLoadingState.shouldHide()) {
			HookProbe.markOnce("forceCloseLoadingScreen");
			ci.cancel();
		}
	}

	@Inject(method = "tick", at = @At("HEAD"), require = 0)
	private void tweakify$instantClose(CallbackInfo ci) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.modified.forceCloseLoadingScreen || !config.modified.forceCloseLoadingScreenInstant) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		// 世界还没就绪时关屏会闪出标题界面/短暂掉入虚空，等 level 建立后再关。
		if (client.level == null) {
			return;
		}
		if (client.gui.screen() == (Object) this) {
			HookProbe.markOnce("forceCloseLoadingScreenInstant");
			client.gui.setScreen(null);
		}
	}
}
