package com.gtzc.tweakify.client.state;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;

/**
 * 聊天签名隐藏：隐藏消息旁的签名颜色条/标记，
 * 以及进入不安全服务器时的“聊天消息无法验证”提示。
 *
 * <p>只在客户端渲染层隐藏，不移除签名本身，也不影响服务端的举报能力。
 */
public final class ChatSigningHider {
	private ChatSigningHider() {
	}

	// 配置缺失或关闭时保持原版显示。
	static boolean isEnabled() {
		TweakifyModConfig config = TweakifyMod.config();
		return config.modified.hideChatSigning;
	}

	// 聊天行渲染使用的标签：开启后一律按“无签名标记”处理。
	public static GuiMessageTag hideTag(GuiMessageTag original) {
		return isEnabled() ? null : original;
	}

	// 只拦截未验证服务器的警告提示，其它系统提示保持原版。
	public static boolean shouldHideToast(Toast toast) {
		return isEnabled() && toast instanceof SystemToast systemToast
			&& systemToast.getToken() == SystemToast.SystemToastId.UNSECURE_SERVER_WARNING;
	}
}
