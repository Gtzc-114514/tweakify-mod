package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 禁用附魔过于昂贵：功能开启后，铁砧界面不再显示红色的“过于昂贵”，
 * 改为正常显示本次花费。
 */
@Mixin(AnvilScreen.class)
public class AnvilScreenMixin {
	@Redirect(method = "extractLabels", require = 0,
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;hasInfiniteMaterials()Z"))
	private boolean tweakify$hideTooExpensiveText(LocalPlayer player) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		boolean enabled = config != null && config.disableTooExpensive;
		if (enabled) {
			HookProbe.markOnce("anvilScreenLabel");
		}
		return enabled || player.hasInfiniteMaterials();
	}
}
