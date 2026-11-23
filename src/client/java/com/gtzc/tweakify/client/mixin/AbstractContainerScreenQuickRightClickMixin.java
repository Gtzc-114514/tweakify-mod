package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.QuickRightClickService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 背包快捷右键（客户端）：光标为空时右键点击背包里的功能方块/潜影盒/末影箱，
 * 发送打开请求并取消原版“拿起一半物品”的点击。
 *
 * <p>判定与发送统一走 {@link QuickRightClickService#tryOpenFromSlot}，
 * 创造模式物品栏由单独的同名注入复用同一份逻辑。
 * 注入用 require = 0：界面类可能被其它模组改写，冲突时功能失效即可。
 */
@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenQuickRightClickMixin {
	@Inject(
		method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V",
		at = @At("HEAD"), cancellable = true, require = 0)
	private void tweakify$openBackpackItem(Slot slot, int slotId, int mouseButton, ContainerInput type,
			CallbackInfo ci) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
		if (QuickRightClickService.tryOpenFromSlot(slot, mouseButton, type, screen.getMenu(),
			client.player.isShiftKeyDown())) {
			ci.cancel();
		}
	}
}
