package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.ui.FastScroll;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 快速滚动：在 {@code MouseHandler.onScroll} 的竖直滚轮参数上按倍率放大。
 *
 * <p>只在打开了界面（{@code gui.screen() != null}）时放大：同一份滚轮增量在无界面分支
 * 会交给快捷栏切换，那里的倍率保持原版；旁观者菜单等不经过 Screen 的分支也不受影响。
 */
@Mixin(MouseHandler.class)
public class MouseHandlerFastScrollMixin {
	@ModifyVariable(method = "onScroll", at = @At("HEAD"), argsOnly = true, index = 5)
	private double tweakify$fastScroll(double verticalAmount) {
		Minecraft client = Minecraft.getInstance();
		if (client.gui == null || client.gui.screen() == null) {
			return verticalAmount;
		}
		return FastScroll.scaleCurrent(verticalAmount);
	}
}
