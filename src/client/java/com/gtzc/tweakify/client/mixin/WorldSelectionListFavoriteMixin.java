package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.WorldListFavorites;
import com.gtzc.tweakify.config.FavoritesStorage;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * 存档列表每次重新填充（打开界面、搜索过滤）后，把收藏存档排到最前。
 * 加载头与“无存档”提示行保持原位；收藏项之间维持原顺序。
 */
@Mixin(WorldSelectionList.class)
public abstract class WorldSelectionListFavoriteMixin {
	@Inject(method = "fillLevels", at = @At("TAIL"))
	private void tweakify$sortFavoritesFirst(String filter, List<LevelSummary> levels, CallbackInfo ci) {
		WorldSelectionList list = (WorldSelectionList) (Object) this;
		if (((WorldSelectionListAccessor) list).tweakify$entryType()
			!= WorldSelectionList.EntryType.SINGLEPLAYER) {
			return;
		}
		if (!levels.isEmpty()) {
			// 外部删档/改名后清理悬空收藏；列表为空时不动，避免读取失败误删收藏。
			FavoritesStorage.retainWorldFavorites(
				levels.stream().map(LevelSummary::getLevelId).toList());
			// 记录列表的原始顺序：取消收藏后存档按它回到原位置。
			WorldListFavorites.updateBaseOrder(
				levels.stream().map(LevelSummary::getLevelId).toList());
		}
		WorldListFavorites.sortFavoritesFirst(list);
	}
}
