package com.gtzc.tweakify.client.mixin;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * 读取聊天当前可见的消息行（最新消息在索引 0、屏幕最下方）。
 */
@Mixin(ChatComponent.class)
public interface ChatComponentAccessor {
	@Accessor("trimmedMessages")
	List<GuiMessage.Line> tweakify$getTrimmedMessages();

	@Accessor("chatScrollbarPos")
	int tweakify$getChatScrollbarPos();
}
