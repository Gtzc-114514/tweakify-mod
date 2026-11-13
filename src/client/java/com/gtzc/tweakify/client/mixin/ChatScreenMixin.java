package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.ChatInputMemoryState;
import com.gtzc.tweakify.client.ChatSelectionState;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 聊天框文字框选：鼠标拖动记录选择范围，渲染时覆盖半透明高亮，
 * Ctrl+C 复制选中文字；输入框区域的点击与复制仍交给原版处理。
 * 另外负责聊天框内容记忆：关闭时保存输入内容，下次打开恢复，发送消息后不保留。
 */
@Mixin(ChatScreen.class)
public class ChatScreenMixin {
	@Shadow
	protected EditBox input;

	@Shadow
	protected String initial;

	@Shadow
	protected boolean isDraft;

	@Unique
	private boolean tweakify$memoryApplied;

	@Unique
	private String tweakify$prefill;

	// 打开聊天框：把上次关闭时保留的草稿交给原版 initial 通道，避免 setValue 触发补全 responder。
	// init 也会被 resize() 调用，因此只恢复一次；新建界面时上一轮的提交标记必然失效。
	@Inject(method = "init", at = @At("HEAD"))
	private void tweakify$init(CallbackInfo ci) {
		ChatSelectionState.clear();
		if (this.tweakify$memoryApplied) {
			// resize() 也会调用 init：保留发送后仍未关闭界面的提交标记。
			return;
		}
		ChatInputMemoryState.clearSubmitted();
		// isDraft 表示这个 initial 是用户草稿（例如原版保存的聊天草稿），不是 / 之类的预填。
		this.tweakify$prefill = this.isDraft ? null : this.initial;
		this.tweakify$memoryApplied = true;

		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.functions.chatInputMemory
			|| (this.initial != null && !this.initial.isEmpty())) {
			return;
		}
		String saved = ChatInputMemoryState.savedText();
		if (!saved.isEmpty()) {
			this.initial = saved;
			// 与原版恢复草稿一致：让草稿专用按键/保存分支生效。
			this.isDraft = true;
			TweakifyMod.LOGGER.info("Chat input memory restored ({} chars)", saved.length());
		}
	}

	// 输入框内容一旦变化，上一轮的提交标记就失效：避免重打的相同文本被当作“刚发送”。
	@Inject(method = "onEdited", at = @At("TAIL"))
	private void tweakify$invalidateSubmitMarker(String value, CallbackInfo ci) {
		ChatInputMemoryState.clearSubmitted();
	}

	// 同一个包装器处理 Ctrl+C 复制；发送文本的记录放到 handleChatInput，
	// 这样其它模组发送后不关闭聊天界面时也能在关闭时丢弃已发送内容。
	@WrapMethod(method = "keyPressed")
	private boolean tweakify$handleKeyPressed(KeyEvent event, Operation<Boolean> original) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config != null && config.functions.chatTextSelection && event.isCopy()
			&& ChatSelectionState.hasSelection()) {
			Minecraft client = Minecraft.getInstance();
			String text = ChatSelectionState.selectedText(client);
			if (!text.isEmpty()) {
				client.keyboardHandler.setClipboard(text);
				return true;
			}
		}
		return original.call(event);
	}

	// 真正提交聊天/命令时记录文本。放在 handleChatInput 而不是 keyPressed：
	// 聊天/命令被服务器禁用时 keyPressed 直接返回，不会走到这里；实际出站内容都在此记录。
	@WrapMethod(method = "handleChatInput")
	private void tweakify$markSubmitted(String message, boolean addToHistory, Operation<Void> original) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config != null && config.functions.chatInputMemory && ChatInputMemoryState.isSessionActive()
			&& tweakify$matchesInput(message)) {
			// 标记输入框原文，确保 removed() 用原始内容比较时能精确命中。
			ChatInputMemoryState.markSubmitted(this.input.getValue());
		}
		original.call(message, addToHistory);
	}

	@Unique
	private boolean tweakify$matchesInput(String message) {
		if (this.input == null || message == null) {
			return false;
		}
		// 原版发送前会规范化消息；按相同规则比较，避免外部模组传入规范化文本时漏标。
		ChatScreen screen = (ChatScreen) (Object) this;
		return screen.normalizeChatMessage(message)
			.equals(screen.normalizeChatMessage(this.input.getValue()));
	}

	// 关闭聊天框（包括被其他界面替换）：保存输入内容，发送过的消息不保留。
	@Inject(method = "removed", at = @At("TAIL"))
	private void tweakify$rememberInput(CallbackInfo ci) {
		// 世界/连接已拆除时不保存，避免草稿泄漏到下一个服务器或存档。
		Minecraft client = Minecraft.getInstance();
		if (!ChatInputMemoryState.isSessionActive()
			|| client.level == null || client.getConnection() == null) {
			ChatInputMemoryState.endSession();
			return;
		}

		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.functions.chatInputMemory || this.input == null) {
			return;
		}
		String current = this.input.getValue();
		if (ChatInputMemoryState.consumeSubmitted(current)) {
			ChatInputMemoryState.remember("");
			return;
		}
		// 按 / 等方式打开时，未编辑的预填内容不应覆盖已有草稿。
		if (!ChatInputMemoryState.shouldRemember(current, this.tweakify$prefill)) {
			return;
		}
		ChatInputMemoryState.remember(current);
		if (!current.isBlank()) {
			TweakifyMod.LOGGER.info("Chat input memory saved ({} chars)", current.length());
		}
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
		if (config != null && config.functions.chatTextSelection) {
			ChatSelectionState.updateFromMouse(Minecraft.getInstance(), mouseX, mouseY);
		} else {
			ChatSelectionState.clear();
		}
	}

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void tweakify$renderSelection(GuiGraphicsExtractor gui, int mouseX, int mouseY,
										  float partialTick, CallbackInfo ci) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config != null && config.functions.chatTextSelection) {
			ChatSelectionState.render(gui, Minecraft.getInstance());
		}
	}

	@Inject(method = "onClose", at = @At("HEAD"))
	private void tweakify$clearOnClose(CallbackInfo ci) {
		ChatSelectionState.clear();
	}
}
