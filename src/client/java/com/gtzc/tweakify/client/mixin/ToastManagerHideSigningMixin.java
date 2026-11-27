package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.state.ChatSigningHider;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 聊天签名隐藏：进入未验证聊天的服务器时不再弹出“聊天消息无法验证”提示。
 *
 * <p>只拦截 {@code UNSECURE_SERVER_WARNING} 这一个系统提示；
 * 配方、进度等其它提示不受影响。
 */
@Mixin(ToastManager.class)
public class ToastManagerHideSigningMixin {
	@Inject(method = "addToast(Lnet/minecraft/client/gui/components/toasts/Toast;)V",
		at = @At("HEAD"), cancellable = true)
	private void tweakify$hideUnverifiedChatToast(Toast toast, CallbackInfo ci) {
		if (ChatSigningHider.shouldHideToast(toast)) {
			ci.cancel();
		}
	}
}
