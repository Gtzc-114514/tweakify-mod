package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.state.AnvilPickupRules;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 禁用附魔过于昂贵：功能开启后，铁砧界面不再显示红色的“过于昂贵”，
 * 改为正常显示本次花费；设置费用上限后，超出上限时保留原版提示（仅提示模式也显示）。
 */
@Mixin(AnvilScreen.class)
public class AnvilScreenMixin {
	@WrapOperation(method = "extractLabels", require = 0,
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;hasInfiniteMaterials()Z"))
	private boolean tweakify$hideTooExpensiveText(LocalPlayer player, Operation<Boolean> original) {
		TweakifyModConfig config = TweakifyMod.config();
		if (!config.modified.disableTooExpensive) {
			return original.call(player);
		}
		AnvilMenu menu = ((AnvilScreen) (Object) this).getMenu();
		boolean showLabel = AnvilPickupRules.shouldShowTooExpensiveLabel(true, menu.getCost(),
			config.modified.tooExpensiveMaxLevelCost);
		if (!showLabel) {
			HookProbe.markOnce("anvilScreenLabel");
		}
		return !showLabel;
	}
}
