package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.QuickBurnFeature;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 便捷焚烧（创造物品栏）：该界面重写了 slotClicked 且不调用父类，
 * 因此需要单独注入；光标完全由客户端管理，直接清空即可。
 */
@Mixin(CreativeModeInventoryScreen.class)
public class CreativeModeInventoryScreenQuickBurnMixin {
	@Unique
	private static final AtomicBoolean TWEAKIFY$CREATIVE_BURN_PROBE = new AtomicBoolean();

	@Inject(method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V",
		at = @At("HEAD"), cancellable = true, require = 0)
	private void tweakify$burnOnRightClick(Slot slot, int slotId, int mouseButton, ContainerInput type,
											CallbackInfo ci) {
		HookProbe.markOnce(TWEAKIFY$CREATIVE_BURN_PROBE, "creativeQuickBurn");
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.functions.quickBurnItem || mouseButton != 1 || type != ContainerInput.PICKUP) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}
		AbstractContainerMenu menu = ((CreativeModeInventoryScreen) (Object) this).getMenu();
		if (QuickBurnFeature.canBurn(menu, slot) && QuickBurnFeature.tryBurnCreative(client, menu)) {
			ci.cancel();
		}
	}
}
