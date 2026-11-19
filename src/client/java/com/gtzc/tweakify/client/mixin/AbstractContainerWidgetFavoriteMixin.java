package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.FavoriteStarRenderer;
import com.gtzc.tweakify.client.WorldListFavorites;
import fi.dy.masa.malilib.util.input.ScanCodes;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 存档列表容器：星标点击的统一入口。
 *
 * <p>收藏星画在条目矩形左侧之外，原版 {@code getEntryAtPosition} 不会命中条目，
 * 条目自己的 mouseClicked 收不到事件；因此在容器点击入口先检查星标区域，
 * 命中时切换收藏并消费本次点击（不选中/不进入存档）。
 */
@Mixin(AbstractContainerWidget.class)
public abstract class AbstractContainerWidgetFavoriteMixin {
	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void tweakify$clickWorldFavoriteStar(MouseButtonEvent event, boolean doubleClick,
												 CallbackInfoReturnable<Boolean> cir) {
		if (event.input() != ScanCodes.OFFSET_MOUSE_LEFT
			|| !((Object) this instanceof WorldSelectionList list)
			// 只处理列表矩形内的点击：children 里可能还有滚出可视区的条目，
			// 不加这个边界检查会把底部按钮/列表外区域的点击误判成星标点击。
			|| !list.isMouseOver(event.x(), event.y())) {
			return;
		}
		if (((WorldSelectionListAccessor) list).tweakify$entryType()
			!= WorldSelectionList.EntryType.SINGLEPLAYER) {
			return;
		}
		for (WorldSelectionList.Entry entry : list.children()) {
			if (!(entry instanceof WorldSelectionList.WorldListEntry worldEntry)) {
				continue;
			}
			int x = FavoriteStarRenderer.worldStarX(worldEntry.getX());
			int y = FavoriteStarRenderer.worldStarY(worldEntry.getContentY());
			if (FavoriteStarRenderer.isOver((int) event.x(), (int) event.y(), x, y)) {
				WorldListFavorites.toggle(worldEntry, list);
				cir.setReturnValue(true);
				return;
			}
		}
	}
}
