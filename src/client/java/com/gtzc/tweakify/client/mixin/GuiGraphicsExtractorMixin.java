package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.DurabilityOverlayRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 装备耐久显示：在物品装饰（耐久条、冷却、数量）绘制完成后，
 * 往耐久条上方叠加百分比 / 当前与总耐久文字。
 * 物品栏、容器界面与 HUD 快捷栏都会走 itemDecorations，因此三处同时生效。
 */
@Mixin(GuiGraphicsExtractor.class)
public class GuiGraphicsExtractorMixin {
	@Inject(method = "itemDecorations(Lnet/minecraft/client/gui/Font;"
		+ "Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At("TAIL"))
	private void tweakify$renderDurabilityOverlay(Font font, ItemStack stack, int x, int y, String countOverride,
												  CallbackInfo ci) {
		DurabilityOverlayRenderer.render((GuiGraphicsExtractor) (Object) this, font, stack, x, y);
	}
}
