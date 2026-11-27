package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.ui.SmoothScrolling;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 记录创造栏物品格当前真正被填进槽位的起始行。
 *
 * <p>{@code ItemPickerMenu.scrollTo} 内部按四舍五入取行（原版滚轮、拖动右侧滑块、
 * 窗口缩放、搜索结果刷新、切页都会调用），而自绘网格按 {@code floor} 行 + 亚行偏移渲染。
 * 两者差一行时，底部预告行会和第 5 行画出同样的物品。这里在每次调用后记录实际行，
 * 渲染前由 {@code CreativeModeInventorySmoothScrollMixin} 按记录校正。
 */
@Mixin(CreativeModeInventoryScreen.ItemPickerMenu.class)
public abstract class CreativeItemPickerMenuScrollMixin {
	@Shadow
	protected abstract int getRowIndexForScroll(float scrollOffs);

	@Inject(method = "scrollTo", at = @At("TAIL"))
	private void tweakify$recordMenuRow(float scrollOffs, CallbackInfo ci) {
		SmoothScrolling.setCreativeMenuRow(this.getRowIndexForScroll(scrollOffs));
	}
}
