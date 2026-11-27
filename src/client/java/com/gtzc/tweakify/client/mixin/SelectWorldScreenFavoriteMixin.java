package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.render.FavoriteStarRenderer;
import com.gtzc.tweakify.config.FavoritesStorage;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 存档界面的删除按钮：选中的存档被收藏时置灰并显示提示，取消收藏后恢复原状态。
 */
@Mixin(SelectWorldScreen.class)
public abstract class SelectWorldScreenFavoriteMixin {
	@Shadow
	private Button deleteButton;

	@Inject(method = "updateButtonStatus", at = @At("TAIL"))
	private void tweakify$blockFavoriteDeletion(LevelSummary summary, CallbackInfo ci) {
		if (this.deleteButton == null) {
			return;
		}
		if (summary != null && FavoriteStarRenderer.isEnabled()
			&& FavoritesStorage.isWorldFavorite(summary.getLevelId())) {
			this.deleteButton.active = false;
			this.deleteButton.setTooltip(Tooltip.create(
				Component.translatable("tweakify-mod.gui.world_list.favorite_delete_blocked")));
		} else {
			// 原版不给删除按钮设置提示，这里只需清掉收藏时的提示。
			this.deleteButton.setTooltip(null);
		}
	}
}
