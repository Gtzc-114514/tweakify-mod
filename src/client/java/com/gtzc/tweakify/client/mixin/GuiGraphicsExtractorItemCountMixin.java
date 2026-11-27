package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.render.ItemCountScale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 物品数量数字缩放：拦截原版 {@code itemCount}，按配置百分比重画数量文字。
 *
 * <p>锚点、阴影与颜色沿用原版；100% 时不拦截，保持原版绘制路径零开销。
 */
@Mixin(GuiGraphicsExtractor.class)
public class GuiGraphicsExtractorItemCountMixin {
	@Inject(method = "itemCount", at = @At("HEAD"), cancellable = true)
	private void tweakify$scaleItemCount(Font font, ItemStack stack, int x, int y, String countText,
										CallbackInfo ci) {
		if (!ItemCountScale.shouldDraw(stack, countText)) {
			return;
		}
		int percent = ItemCountScale.currentPercent();
		if (percent == 100) {
			return;
		}
		GuiGraphicsExtractor graphics = (GuiGraphicsExtractor) (Object) this;
		if (ItemCountScale.draw(graphics, font, ItemCountScale.textOf(stack, countText), x, y, percent)) {
			ci.cancel();
		}
	}
}
