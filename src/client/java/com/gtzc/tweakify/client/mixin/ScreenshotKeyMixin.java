package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.BetterScreenshotManager;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 接管 F2 截图；启用子项时接管 F9 并输出 4K。
 */
@Mixin(Minecraft.class)
public class ScreenshotKeyMixin {
	@Inject(method = "handleGlobalKeyPress", at = @At("HEAD"), cancellable = true)
	private void tweakify$captureScreenshot(InputConstants.Key key, boolean ctrlDown,
											 CallbackInfoReturnable<Boolean> cir) {
		// 第二个参数是 Ctrl 状态，不是“是否按下”；本方法每次按键只进入一次。
		// Ctrl+F2 交还原版复制到剪贴板。
		if (ctrlDown) {
			return;
		}
		Minecraft client = (Minecraft) (Object) this;
		TweakifyModConfig config = TweakifyMod.config();
		if (!config.functions.betterScreenshot
			|| client.gui.screen() != null || client.options.keyDebugModifier.isDown()) {
			return;
		}
		if (client.options.keyScreenshot.matches(key)) {
			cir.setReturnValue(true);
			BetterScreenshotManager.capture(client, false);
		} else if (config.functions.betterScreenshotF9
			&& key.getValue() == InputConstants.KEY_F9) {
			cir.setReturnValue(true);
			BetterScreenshotManager.capture(client, true);
		}
	}
}
