package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.ui.SmoothScrollAnimator;
import com.gtzc.tweakify.client.ui.SmoothScrolling;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.util.Util;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * 聊天栏平滑滚动。
 *
 * <p>原版一次滚轮直接跳 7 行（按住 Shift 为 1 行）。这里把跳变改成“整行滚动 + 亚行像素偏移”：
 * 行数仍由 {@code scrollChat} 推进，不足一行的部分在渲染时用姿态平移补上，
 * 方向与直观一致——向上滚（查看更早的消息）时内容向下移动。
 *
 * <p>已知取舍：亚行偏移生效的瞬间，点击聊天行/文字选择按原版行位置计算，最多偏差半行（4 像素）。
 */
@Mixin(ChatComponent.class)
public abstract class ChatComponentSmoothScrollMixin {
	// 单帧最大时间步长：卡顿后一次性追上目标会显得突兀。
	@Unique
	private static final double TWEAKIFY$MAX_FRAME_SECONDS = 0.1;

	@Shadow
	private List<GuiMessage.Line> trimmedMessages;

	@Shadow
	private int chatScrollbarPos;

	@Unique
	private final SmoothScrollAnimator tweakify$animator = new SmoothScrollAnimator();

	@Unique
	private long tweakify$lastFrameMillis;

	// 已经通过 scrollChat 应用到原版状态的行数。
	@Unique
	private int tweakify$appliedLines;

	// 本帧要补的亚行像素偏移（正值 = 内容下移）。
	@Unique
	private int tweakify$renderOffset;

	// 动画自己调用 scrollChat 时置位，避免被下面的注入再次拦截。
	@Unique
	private boolean tweakify$applying;

	// 新消息入队期间原版会逐行 scrollChat(1) 做“保持当前位置”的补偿：
	// 这段必须立即生效，否则每收到一条消息，正在阅读的历史会先跳 9 像素再滑回来。
	@Unique
	private boolean tweakify$inMessageQueue;

	@Shadow
	public abstract int getLinesPerPage();

	@Shadow
	public abstract void scrollChat(int amount);

	// 滚轮与翻页都走这里：只累加目标位置，真正的行号推进放到每帧渲染前。
	@Inject(method = "scrollChat", at = @At("HEAD"), cancellable = true)
	private void tweakify$animateScroll(int amount, CallbackInfo ci) {
		if (this.tweakify$applying || this.tweakify$inMessageQueue || amount == 0
			|| !SmoothScrolling.chatEnabled()) {
			return;
		}
		this.tweakify$syncFromVanilla();
		this.tweakify$animator.addDelta(amount * (double) SmoothScrolling.CHAT_LINE_HEIGHT,
			this.tweakify$maxScrollPixels());
		ci.cancel();
	}

	// 新消息入队：期间的 scrollChat 保持原版同步执行（上面已跳过拦截）。
	@Inject(method = "addMessageToDisplayQueue", at = @At("HEAD"))
	private void tweakify$beginMessageQueue(GuiMessage message, CallbackInfo ci) {
		this.tweakify$inMessageQueue = true;
	}

	@Inject(method = "addMessageToDisplayQueue", at = @At("TAIL"))
	private void tweakify$endMessageQueue(GuiMessage message, CallbackInfo ci) {
		this.tweakify$inMessageQueue = false;
	}

	// 关闭聊天/滚回最新消息：动画状态一起复位。
	@Inject(method = "resetChatScroll", at = @At("TAIL"))
	private void tweakify$resetAnimator(CallbackInfo ci) {
		this.tweakify$animator.jumpTo(0.0, 0.0);
		this.tweakify$appliedLines = 0;
		this.tweakify$renderOffset = 0;
	}

	// 渲染整段聊天历史时按亚行偏移平移：文字、每行底板与滚动条一起移动。
	@WrapMethod(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V")
	private void tweakify$renderWithSmoothOffset(GuiGraphicsExtractor gui, Font font, int ticks, int mouseX, int mouseY,
												 ChatComponent.DisplayMode mode, boolean focused, Operation<Void> original) {
		int offset = this.tweakify$advanceSmoothScroll();
		// 聊天内容的坐标空间：行步长 = 9*(1+行距)，整体再乘聊天缩放。
		var options = Minecraft.getInstance().options;
		if (offset == 0) {
			original.call(gui, font, ticks, mouseX, mouseY, mode, focused);
			return;
		}
		// 亚行偏移同样按行距与聊天缩放换算，否则改过设置时每进一行会抽一下。
		double lineScale = options.chatScale().get() * (1.0 + options.chatLineSpacing().get());
		float screenOffset = (float) (offset * lineScale);
		Matrix3x2fStack pose = gui.pose();
		pose.pushMatrix();
		pose.translate(0.0f, screenOffset);
		try {
			original.call(gui, font, ticks, mouseX, mouseY, mode, focused);
		} finally {
			pose.popMatrix();
		}
	}

	// 每帧推进动画：同一毫秒内被重复调用（Hud 与聊天界面同时渲染）时复用上次结果。
	@Unique
	private int tweakify$advanceSmoothScroll() {
		long now = Util.getMillis();
		if (now == this.tweakify$lastFrameMillis) {
			return this.tweakify$renderOffset;
		}
		double deltaSeconds = this.tweakify$lastFrameMillis == 0L
			? 0.0
			: Math.min(TWEAKIFY$MAX_FRAME_SECONDS, (now - this.tweakify$lastFrameMillis) / 1000.0);
		this.tweakify$lastFrameMillis = now;

		double maxPixels = this.tweakify$maxScrollPixels();
		if (!SmoothScrolling.chatEnabled()) {
			this.tweakify$animator.jumpTo(this.chatScrollbarPos * (double) SmoothScrolling.CHAT_LINE_HEIGHT, maxPixels);
			this.tweakify$appliedLines = this.chatScrollbarPos;
			this.tweakify$renderOffset = 0;
			return 0;
		}
		this.tweakify$syncFromVanilla();
		this.tweakify$animator.setTimeConstant(SmoothScrolling.timeConstantSeconds());
		this.tweakify$animator.update(deltaSeconds, maxPixels);

		int desired = (int) Math.round(this.tweakify$animator.position() / SmoothScrolling.CHAT_LINE_HEIGHT);
		if (desired != this.tweakify$appliedLines) {
			this.tweakify$applying = true;
			try {
				// 行数交给原版推进：越界夹紧与 newMessageSinceScroll 等状态仍由它维护。
				this.scrollChat(desired - this.tweakify$appliedLines);
			} finally {
				this.tweakify$applying = false;
			}
			// scrollChat 会按内容长度夹紧：以实际生效的行数为准。
			this.tweakify$appliedLines = this.chatScrollbarPos;
		}
		this.tweakify$renderOffset = (int) Math.round(this.tweakify$animator.position()
			- this.tweakify$appliedLines * (double) SmoothScrolling.CHAT_LINE_HEIGHT);
		return this.tweakify$renderOffset;
	}

	// 原版行号被外部改动（新消息自动上滚、其它模组、复位）时，动画位置直接对齐。
	@Unique
	private void tweakify$syncFromVanilla() {
		if (this.chatScrollbarPos != this.tweakify$appliedLines) {
			this.tweakify$appliedLines = this.chatScrollbarPos;
			this.tweakify$animator.jumpTo(this.chatScrollbarPos * (double) SmoothScrolling.CHAT_LINE_HEIGHT,
				this.tweakify$maxScrollPixels());
		}
	}

	@Unique
	private double tweakify$maxScrollPixels() {
		return Math.max(0, this.trimmedMessages.size() - this.getLinesPerPage())
			* (double) SmoothScrolling.CHAT_LINE_HEIGHT;
	}
}
