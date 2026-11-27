package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.state.ChatSigningHider;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 聊天签名隐藏：聊天行渲染读取签名标签时返回 null，
 * 颜色条与“已修改/未验证”标记都不再绘制。
 *
 * <p>目标是无名内部类（聊天行渲染回调）；标签只影响渲染，
 * 消息内容、签名与聊天记录保持原版。
 */
@Mixin(targets = "net.minecraft.client.gui.components.ChatComponent$1")
public abstract class ChatComponentHideSigningMixin {
	@WrapOperation(method = "accept(Lnet/minecraft/client/multiplayer/chat/GuiMessage$Line;IF)V",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/multiplayer/chat/GuiMessage$Line;"
				+ "tag()Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;"))
	private GuiMessageTag tweakify$hideSigningTag(GuiMessage.Line line, Operation<GuiMessageTag> original) {
		return ChatSigningHider.hideTag(original.call(line));
	}
}
