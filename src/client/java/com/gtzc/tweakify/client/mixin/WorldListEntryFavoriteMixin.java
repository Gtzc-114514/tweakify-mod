package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.FavoriteStarRenderer;
import com.gtzc.tweakify.config.FavoritesStorage;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 存档列表条目：左侧收藏星渲染与收藏存档的删除拦截。
 *
 * <p>星标画在条目矩形左侧之外（世界图标左边），原版列表不会把这一区域的点击
 * 分发给条目，因此点击由 {@link AbstractContainerWidgetFavoriteMixin} 在容器层处理。
 */
@Mixin(WorldSelectionList.WorldListEntry.class)
public abstract class WorldListEntryFavoriteMixin {
	@Shadow
	@Final
	private WorldSelectionList list;

	@Shadow
	public abstract LevelSummary getLevelSummary();

	@Inject(method = "extractContent", at = @At("TAIL"))
	private void tweakify$drawFavoriteStar(GuiGraphicsExtractor gui, int mouseX, int mouseY,
										   boolean hovered, float partialTicks, CallbackInfo ci) {
		if (!this.tweakify$showsFavoriteStar()) {
			return;
		}
		int x = FavoriteStarRenderer.worldStarX(this.tweakify$entry().getX());
		int y = FavoriteStarRenderer.worldStarY(this.tweakify$entry().getContentY());
		boolean favorite = FavoritesStorage.isWorldFavorite(this.getLevelSummary().getLevelId());
		FavoriteStarRenderer.draw(gui, x, y, favorite,
			FavoriteStarRenderer.isOver(mouseX, mouseY, x, y));
	}

	// 收藏的存档不可删除：删除按钮已置灰，这里再拦截直接调用删除的其它路径。
	@Inject(method = "deleteWorld", at = @At("HEAD"), cancellable = true)
	private void tweakify$blockFavoriteDeletion(CallbackInfo ci) {
		if (FavoriteStarRenderer.isEnabled()
			&& FavoritesStorage.isWorldFavorite(this.getLevelSummary().getLevelId())) {
			ci.cancel();
		}
	}

	// 兜底：确认框回调与 DEBUG 世界重建按钮会直接调用 doDeleteWorld，
	// 绕过 deleteWorld 入口；收藏存档在这里再拦一次，保证不会被物理删除。
	@Inject(method = "doDeleteWorld", at = @At("HEAD"), cancellable = true)
	private void tweakify$blockFavoriteDeletionHard(CallbackInfo ci) {
		if (FavoriteStarRenderer.isEnabled()
			&& FavoritesStorage.isWorldFavorite(this.getLevelSummary().getLevelId())) {
			ci.cancel();
		}
	}

	// 上传世界（Realms）列表不提供收藏星，避免与“单人存档”语义混淆。
	@Unique
	private boolean tweakify$showsFavoriteStar() {
		return FavoriteStarRenderer.isEnabled()
			&& ((WorldSelectionListAccessor) (Object) this.list).tweakify$entryType()
				== WorldSelectionList.EntryType.SINGLEPLAYER;
	}

	// Mixin 类无法直接调用目标类的父类方法：先转成目标类自身（public），再调用继承的坐标方法。
	@Unique
	private WorldSelectionList.WorldListEntry tweakify$entry() {
		return (WorldSelectionList.WorldListEntry) (Object) this;
	}
}
