package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.HeldItemInfoRenderer;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 手持物品信息：关闭任意容器界面后请求重建一次。
 *
 * <p>背包快捷右键编辑手持潜影盒、铁砧/砂轮等操作会改变物品内容但保持同一个 ItemStack 实例，
 * 仅靠“物品实例变化”无法触发重建；这里在界面关闭时补一次刷新。
 */
@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenHeldItemInfoMixin {
	@Inject(method = "removed", at = @At("HEAD"))
	private void tweakify$requestHeldItemInfoRebuild(CallbackInfo ci) {
		HeldItemInfoRenderer.requestRebuild();
	}
}
