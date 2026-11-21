package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.ScreenshotChatCards;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 在聊天行绘制截图卡片缩略图；同时覆盖聊天未聚焦的后台通道与聚焦时的前景通道。
 */
@Mixin(targets = {
	"net.minecraft.client.gui.components.ChatComponent$DrawingFocusedGraphicsAccess",
	"net.minecraft.client.gui.components.ChatComponent$DrawingBackgroundGraphicsAccess"
})
public abstract class ChatScreenshotRenderMixin {
	@Shadow
	@Final
	private GuiGraphicsExtractor graphics;

	@Inject(method = "handleMessage", at = @At("HEAD"))
	private void tweakify$renderScreenshot(int y, float alpha, FormattedCharSequence content,
										   CallbackInfoReturnable<Boolean> cir) {
		ScreenshotChatCards.render(this.graphics, y, alpha, content);
	}
}
