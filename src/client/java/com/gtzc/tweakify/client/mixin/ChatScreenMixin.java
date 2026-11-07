package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.ChatSelectionState;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 聊天框文字框选：鼠标拖动记录选择范围，渲染时覆盖半透明高亮，
 * Ctrl+C 复制选中文字；输入框区域的点击与复制仍交给原版处理。
 */
@Mixin(ChatScreen.class)
public class ChatScreenMixin {
	@Inject(method = "init", at = @At("HEAD"))
	private void tweakify$clearSelection(CallbackInfo ci) {
		ChatSelectionState.clear();
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"))
	private void tweakify$beginSelection(MouseButtonEvent event, boolean isRealMouse,
										 CallbackInfoReturnable<Boolean> cir) {
		if (event.isLeft()) {
			ChatSelectionState.begin(event.x(), event.y());
		} else {
			ChatSelectionState.clear();
		}
	}

	@Inject(method = "extractRenderState", at = @At("HEAD"))
	private void tweakify$updateSelection(GuiGraphicsExtractor gui, int mouseX, int mouseY,
										  float partialTick, CallbackInfo ci) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config != null && config.chatTextSelection) {
			ChatSelectionState.updateFromMouse(Minecraft.getInstance(), mouseX, mouseY);
		} else {
			ChatSelectionState.clear();
		}
	}

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void tweakify$renderSelection(GuiGraphicsExtractor gui, int mouseX, int mouseY,
										  float partialTick, CallbackInfo ci) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config != null && config.chatTextSelection) {
			ChatSelectionState.render(gui, Minecraft.getInstance());
		}
	}

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void tweakify$copySelection(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.chatTextSelection || !event.isCopy()
			|| !ChatSelectionState.hasSelection()) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		String text = ChatSelectionState.selectedText(client);
		if (!text.isEmpty()) {
			client.keyboardHandler.setClipboard(text);
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "onClose", at = @At("HEAD"))
	private void tweakify$clearOnClose(CallbackInfo ci) {
		ChatSelectionState.clear();
	}
}
