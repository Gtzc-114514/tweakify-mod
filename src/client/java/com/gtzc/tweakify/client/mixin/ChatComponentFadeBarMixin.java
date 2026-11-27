package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.ChatFadeBar;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

/**
 * 聊天框消失进度条：每条新消息底部绘制一条白色倒计时条。
 *
 * <p>原版聊天消息按 {@code ChatComponent.AlphaCalculator.timeBased} 在 200 tick 内淡出。
 * 这里在原版渲染完成后按同一时间线补画进度条：长度随剩余时间缩短，
 * 透明度与消息文字同步，因此不会出现条还亮着但文字已经消失的情况。
 *
 * <p>26.3 的聊天有两套渲染入口：聊天界面关闭时 HUD 只用 {@code DisplayMode.BACKGROUND}
 * 渲染（文字和背景在同一个访问器里），聊天界面打开时 {@code ChatScreen} 改用
 * {@code DisplayMode.FOREGROUND}。两种模式下每帧都只会调用一次，所以两种模式都要绘制；
 * 前台模式下原版文字恒亮，进度条的透明度也固定为文字不透明度，只有长度继续倒计时。
 */
@Mixin(ChatComponent.class)
public abstract class ChatComponentFadeBarMixin {
	@Shadow
	private List<GuiMessage.Line> trimmedMessages;

	@Shadow
	private int chatScrollbarPos;

	@Shadow
	public abstract int getLinesPerPage();

	@WrapMethod(method = "extractRenderState("
		+ "Lnet/minecraft/client/gui/GuiGraphicsExtractor;"
		+ "Lnet/minecraft/client/gui/Font;"
		+ "III"
		+ "Lnet/minecraft/client/gui/components/ChatComponent$DisplayMode;"
		+ "Z)V")
	private void tweakify$drawFadeBars(GuiGraphicsExtractor graphics, Font font, int ticks, int mouseX,
									   int mouseY, ChatComponent.DisplayMode mode,
									   boolean changeCursorOnInsertions, Operation<Void> original) {
		original.call(graphics, font, ticks, mouseX, mouseY, mode, changeCursorOnInsertions);
		TweakifyModConfig config = TweakifyMod.config();
		if (config == null || !config.functions.chatFadeProgressBar) {
			return;
		}
		if (this.trimmedMessages.isEmpty()) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		double scale = minecraft.options.chatScale().get();
		if (scale <= 0.0) {
			return;
		}
		int maxWidth = Mth.ceil((float) ChatComponent.getWidth(
			minecraft.options.chatWidth().get()) / (float) scale);
		int chatBottom = Mth.floor((float) (graphics.guiHeight() - 40) / (float) scale);
		double lineSpacing = minecraft.options.chatLineSpacing().get();
		int entryHeight = (int) (9.0 * (lineSpacing + 1.0));
		float textOpacity = (float) (minecraft.options.chatOpacity().get() * 0.9 + 0.1);
		int perPage = this.getLinesPerPage();
		int visible = Math.min(this.trimmedMessages.size() - this.chatScrollbarPos, perPage);
		if (visible <= 0) {
			return;
		}
		int guiTicks = minecraft.gui.hud.getGuiTicks();

		graphics.pose().pushMatrix();
		try {
			graphics.pose().scale((float) scale, (float) scale);
			graphics.pose().translate(4.0F, 0.0F);
			for (int index = visible - 1; index >= 0; index--) {
				GuiMessage.Line line = this.trimmedMessages.get(index + this.chatScrollbarPos);
				// 只画整条消息的底边：同一条消息换行时中间不出现进度条。
				if (!line.endOfEntry()) {
					continue;
				}
				int age = guiTicks - line.addedTime();
				float remaining = ChatFadeBar.remaining(age);
				// 聊天界面打开时原版用 FULLY_VISIBLE 渲染文字，进度条也只保留长度倒计时。
				float alpha = ChatFadeBar.barAlpha(age, mode.foreground);
				if (remaining <= 0.0F || alpha <= 1.0e-5F) {
					continue;
				}
				int entryBottom = chatBottom - index * entryHeight;
				int barRight = ChatFadeBar.barRight(-4, maxWidth + 8, remaining);
				// 画在行底边之下 1px：默认行距下文字占满 [entryBottom-8, entryBottom]，
				// 画在底边内侧会盖住字形基线行；底边下方的这一行是行间空白。
				graphics.fill(-4, entryBottom, barRight, entryBottom + ChatFadeBar.BAR_HEIGHT,
					ARGB.white(alpha * textOpacity));
			}
		} finally {
			graphics.pose().popMatrix();
		}
	}
}
