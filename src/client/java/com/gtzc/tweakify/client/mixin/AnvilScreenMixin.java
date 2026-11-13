package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 禁用附魔过于昂贵：功能开启后，铁砧界面不再显示红色的“过于昂贵”，
 * 改为正常显示本次花费。
 */
@Mixin(AnvilScreen.class)
public class AnvilScreenMixin {
	@WrapOperation(method = "extractLabels", require = 0,
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;hasInfiniteMaterials()Z"))
	private boolean tweakify$hideTooExpensiveText(LocalPlayer player, Operation<Boolean> original) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		boolean enabled = config != null && config.modified.disableTooExpensive;
		if (enabled) {
			HookProbe.markOnce("anvilScreenLabel");
		}
		return enabled || original.call(player);
	}
}
