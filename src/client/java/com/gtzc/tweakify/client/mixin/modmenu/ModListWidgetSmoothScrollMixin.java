package com.gtzc.tweakify.client.mixin.modmenu;

import com.gtzc.tweakify.client.ui.SmoothScrolling;
import com.terraformersmc.modmenu.gui.widget.ModListWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mod Menu 列表的逐帧滚动推进（滚轮入口在 AbstractScrollAreaSmoothScrollMixin）。
 *
 * <p>列表条目按整数像素重新摆位（原生就支持 1 像素步进），所以这里只负责按缓动
 * 每帧写回滚动值即可得到像素级平滑；滚动记忆恢复、键盘翻页等外部改值会立即对齐。
 */
@Mixin(ModListWidget.class)
public abstract class ModListWidgetSmoothScrollMixin {
	@Inject(method = "extractListItems", at = @At("HEAD"))
	private void tweakify$updateSmoothScroll(GuiGraphicsExtractor gui, int mouseX, int mouseY, float delta,
											 CallbackInfo ci) {
		AbstractSelectionList<?> list = (AbstractSelectionList<?>) (Object) this;
		SmoothScrolling.updateModMenu(list.scrollAmount(), list.maxScrollAmount(), list::setScrollAmount);
	}
}
