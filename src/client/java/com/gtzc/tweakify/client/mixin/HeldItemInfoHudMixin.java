package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.HeldItemInfoRenderer;
import com.gtzc.tweakify.config.FunctionConfig;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 手持物品信息（参考 held-item-info）：
 * 1. 在 extractSelectedItemName 头部接管选中物品名称的绘制；
 * 2. 物品切换或内容相关配置变化时重建信息，并把淡出倒计时改为按行数计算；
 * 3. 动作栏消息与加高后的信息重叠时自动上移。
 *
 * <p>不重定向方法内部的 textWithBackdrop 调用：held-item-info 等同类模组会重定向同一处，
 * 两个 @Redirect 叠在同一条指令上会导致启动失败。改为 HEAD 注入 + cancel 后，
 * 功能关闭或构建失败时会自然放行原版与其它模组。
 */
@Mixin(Hud.class)
public abstract class HeldItemInfoHudMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Shadow
	private int toolHighlightTimer;

	@Shadow
	private ItemStack lastToolHighlight;

	@Shadow
	private int lastHealth;

	@Shadow
	private int displayHealth;

	@Shadow
	@Nullable
	protected abstract Player getCameraPlayer();

	@Unique
	private ItemStack tweakify$stackBeforeTick = ItemStack.EMPTY;

	// 每帧开始时清掉上一帧的提示位置，动作栏消息只依据本帧绘制结果避让。
	@Inject(method = "extractRenderState", at = @At("HEAD"))
	private void tweakify$beforeRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker,
			CallbackInfo ci) {
		HeldItemInfoRenderer.beforeFrame();
	}

	// 物品切换或内容相关配置变化时重建（参考实现只处理物品切换，这里额外照顾游戏内改配置）。
	@Inject(method = "tick()V", at = @At("HEAD"))
	private void tweakify$captureToolHighlight(CallbackInfo ci) {
		this.tweakify$stackBeforeTick = this.lastToolHighlight;
	}

	@Inject(method = "tick()V", at = @At("RETURN"))
	private void tweakify$rebuildTooltip(CallbackInfo ci) {
		Minecraft client = this.minecraft;
		if (client.player == null) {
			return;
		}
		TweakifyModConfig config = TweakifyMod.config();
		FunctionConfig functions = config.functions;
		boolean enabled = functions.heldItemInfo;
		boolean enabledChanged = HeldItemInfoRenderer.noteEnabled(enabled);
		boolean stackChanged = this.lastToolHighlight != this.tweakify$stackBeforeTick;
		if (!enabled) {
			if (stackChanged) {
				HeldItemInfoRenderer.clear();
			}
			return;
		}
		if (this.lastToolHighlight.isEmpty()) {
			HeldItemInfoRenderer.clear();
			return;
		}
		HeldItemInfoRenderer.refreshFontMetrics(client);
		boolean rebuildRequested = HeldItemInfoRenderer.consumeRebuildRequest();
		if (!stackChanged && !enabledChanged && !rebuildRequested
			&& !HeldItemInfoRenderer.contentSignatureChanged(functions)) {
			return;
		}
		if (HeldItemInfoRenderer.update(client, this.lastToolHighlight)) {
			this.toolHighlightTimer = HeldItemInfoRenderer.fadeTicks(functions);
		}
	}

	// 接管原版选中物品名称：功能关闭或构建失败时放行原版（以及其它模组）的绘制。
	@Inject(method = "extractSelectedItemName", at = @At("HEAD"), cancellable = true)
	private void tweakify$replaceSelectedItemName(GuiGraphicsExtractor graphics, CallbackInfo ci) {
		if (!HeldItemInfoRenderer.isActive()) {
			return;
		}
		// 先取消：即使本次不绘制（计时归零/空手），也不让其它重定向继续画名称。
		ci.cancel();
		if (this.toolHighlightTimer <= 0 || this.lastToolHighlight.isEmpty()) {
			return;
		}
		int alpha = (int) ((float) this.toolHighlightTimer * 256.0F / 10.0F);
		if (alpha > 255) {
			alpha = 255;
		}
		if (alpha <= 0) {
			return;
		}
		int y = graphics.guiHeight() - HeldItemInfoRenderer.vanillaTooltipYOffset();
		HeldItemInfoRenderer.render(graphics, this.minecraft.font, y, ARGB.white(alpha), this.minecraft,
			this.getCameraPlayer(), this.lastHealth, this.displayHealth);
	}

	// 动作栏消息避让：在绘制前平移，偏移量与参考实现一致。
	@Inject(
		method = "extractOverlayMessage",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;textWithBackdrop"
				+ "(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIII)V"))
	private void tweakify$shiftOverlayMessage(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker,
			CallbackInfo ci) {
		HeldItemInfoRenderer.shiftOverlayMessage(graphics);
	}

	// 断线时清空缓存，避免回到标题界面后残留上一次会话的提示。
	@Inject(method = "onDisconnected", at = @At("HEAD"))
	private void tweakify$clearOnDisconnect(CallbackInfo ci) {
		HeldItemInfoRenderer.clear();
		this.tweakify$stackBeforeTick = ItemStack.EMPTY;
	}
}
