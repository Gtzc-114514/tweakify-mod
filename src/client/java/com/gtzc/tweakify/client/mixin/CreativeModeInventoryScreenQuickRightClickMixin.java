package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.QuickRightClickService;
import com.gtzc.tweakify.util.HookProbe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 背包快捷右键：创造模式物品栏。
 *
 * <p>创造物品栏覆写了 slotClicked 且不调用 super，因此需要单独注入。
 * 只处理玩家背包区域（真实 Inventory 槽）；上方的创造物品列表是客户端临时容器，
 * 服务端没有对应物品，不能安全地打开/写回。
 */
@Mixin(CreativeModeInventoryScreen.class)
public class CreativeModeInventoryScreenQuickRightClickMixin {
	// slotClicked 可能被连点刷屏，按热路径处理：首次之后只剩一次 volatile 读。
	@Unique
	private static final AtomicBoolean TWEAKIFY$QUICK_RIGHT_CLICK_CREATIVE_PROBE = new AtomicBoolean();

	@Inject(
		method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V",
		at = @At("HEAD"), cancellable = true, require = 0)
	private void tweakify$openBackpackItem(Slot slot, int slotId, int mouseButton, ContainerInput type,
			CallbackInfo ci) {
		HookProbe.markOnce(TWEAKIFY$QUICK_RIGHT_CLICK_CREATIVE_PROBE, "quickRightClickCreativeSlot");
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}
		CreativeModeInventoryScreen screen = (CreativeModeInventoryScreen) (Object) this;
		if (QuickRightClickService.tryOpenFromSlot(slot, mouseButton, type, screen.getMenu(),
			client.player.isShiftKeyDown())) {
			ci.cancel();
		}
	}
}
