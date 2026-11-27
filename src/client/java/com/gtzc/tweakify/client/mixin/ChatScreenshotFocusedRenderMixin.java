package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.render.ScreenshotChatCards;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 聊天聚焦时（前景通道）在聊天行绘制截图卡片缩略图。
 * <p>目标内部类是包级私有，只能按名字混入；Mixin 禁止多目标 Mixin 携带可重映射的 @Shadow，
 * 因此前景通道与背景通道各用一个单目标 Mixin。
 */
@Mixin(targets = "net.minecraft.client.gui.components.ChatComponent$DrawingFocusedGraphicsAccess")
public abstract class ChatScreenshotFocusedRenderMixin {
	@Shadow
	@Final
	private GuiGraphicsExtractor graphics;

	@Inject(method = "handleMessage", at = @At("HEAD"))
	private void tweakify$renderScreenshot(int y, float alpha, FormattedCharSequence content,
										   CallbackInfoReturnable<Boolean> cir) {
		ScreenshotChatCards.render(this.graphics, y, alpha, content);
	}
}
