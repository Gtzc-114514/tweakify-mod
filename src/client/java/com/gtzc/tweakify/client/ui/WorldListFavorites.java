package com.gtzc.tweakify.client.ui;

import com.gtzc.tweakify.client.mixin.AbstractSelectionListInvoker;
import com.gtzc.tweakify.client.render.FavoriteStarRenderer;
import com.gtzc.tweakify.config.FavoritesStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

/**
 * 存档收藏的列表级操作：切换收藏、收藏置顶排序与删除按钮刷新。
 *
 * <p>收藏项固定排在列表最前（加载头/提示行仍保持在最上）；同级内部按列表的原始顺序排列，
 * 因此取消收藏后该存档会回到原来所在的位置，而不是停在顶部。
 * 收藏的存档不允许删除：删除按钮会置灰并给出提示，删除入口本身也会被拦截。
 */
public final class WorldListFavorites {
	// 列表的原始顺序：levelId -> 索引。每次 fillLevels 用传入的全部存档顺序刷新；
	// 收藏/取消收藏不会改变这张表，所以取消收藏能恢复到原位置。
	private static final Map<String, Integer> BASE_ORDER = new HashMap<>();

	private WorldListFavorites() {
	}

	// 切换收藏并立即刷新列表顺序与底部按钮状态。
	public static boolean toggle(WorldSelectionList.WorldListEntry entry, WorldSelectionList list) {
		if (!FavoriteStarRenderer.isEnabled()) {
			return false;
		}
		boolean favorite = FavoritesStorage.toggleWorldFavorite(entry.getLevelSummary().getLevelId());
		Minecraft.getInstance().getSoundManager().play(
			SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
		sortFavoritesFirst(list);
		if (favorite) {
			// 收藏项置顶后回到列表顶部，保证置顶效果立刻可见。
			list.setScrollAmount(0.0);
		}
		// 排序保留选中态，但删除按钮要按收藏状态重新刷新。
		if (list.getScreen() instanceof SelectWorldScreen screen) {
			list.getSelectedOpt()
				.map(WorldSelectionList.WorldListEntry::getLevelSummary)
				.ifPresent(screen::updateButtonStatus);
		}
		return favorite;
	}

	// 用列表当前的原始顺序刷新基准：fillLevels 传入的是全部存档（搜索过滤前）的顺序。
	public static void updateBaseOrder(List<String> levelIds) {
		BASE_ORDER.clear();
		for (int i = 0; i < levelIds.size(); i++) {
			BASE_ORDER.putIfAbsent(levelIds.get(i), i);
		}
	}

	// 收藏项置顶：非世界条目（加载头/提示行）保持最前，收藏世界其次，其余按原始顺序。
	// 不能直接改 children()：它是不可变视图；也不能走 replaceEntries：
	// WorldSelectionList.clearEntries 会 close 每个条目（销毁存档图标纹理），
	// 因此调用原版 sort()（内部排序 + 重新定位），不改动条目本身。
	public static void sortFavoritesFirst(WorldSelectionList list) {
		List<WorldSelectionList.Entry> current = list.children();
		List<WorldSelectionList.Entry> sorted = sortByRankAndBase(new ArrayList<>(current),
			WorldListFavorites::sortRank, WorldListFavorites::baseIndex);
		if (sameOrder(current, sorted)) {
			return;
		}
		((AbstractSelectionListInvoker) (Object) list).tweakify$sort(
			Comparator.comparingInt(WorldListFavorites::sortRank)
				.thenComparingInt(WorldListFavorites::baseIndex));
	}

	private static boolean sameOrder(List<WorldSelectionList.Entry> current,
									List<WorldSelectionList.Entry> sorted) {
		if (current.size() != sorted.size()) {
			return false;
		}
		for (int i = 0; i < current.size(); i++) {
			if (current.get(i) != sorted.get(i)) {
				return false;
			}
		}
		return true;
	}

	// 排序键：非世界项（加载头/提示行）=0，收藏世界=1，其余世界=2。
	// 包私有纯函数：测试直接覆盖映射，避免只能通过真实条目实例验证。
	static int rankFor(boolean worldEntry, boolean favorite) {
		if (!worldEntry) {
			return 0;
		}
		return favorite ? 1 : 2;
	}

	private static int sortRank(WorldSelectionList.Entry entry) {
		if (!(entry instanceof WorldSelectionList.WorldListEntry worldEntry)) {
			return rankFor(false, false);
		}
		return rankFor(true, FavoriteStarRenderer.isEnabled()
			&& FavoritesStorage.isWorldFavorite(worldEntry.getLevelSummary().getLevelId()));
	}

	// 原始顺序索引：非世界项固定 0；未记录的存档（例如 Realms 列表）排到最后。
	// 包私有：供单元测试校验基准顺序。
	static int baseIndexOf(String levelId) {
		return BASE_ORDER.getOrDefault(levelId, Integer.MAX_VALUE);
	}

	private static int baseIndex(WorldSelectionList.Entry entry) {
		if (!(entry instanceof WorldSelectionList.WorldListEntry worldEntry)) {
			return 0;
		}
		return baseIndexOf(worldEntry.getLevelSummary().getLevelId());
	}

	// 包私有纯函数：先按 rank、再按原始顺序排序，便于单元测试。
	static <T> List<T> sortByRankAndBase(List<T> entries, ToIntFunction<T> rank,
										 ToIntFunction<T> baseIndex) {
		List<T> sorted = new ArrayList<>(entries);
		sorted.sort(Comparator.comparingInt(rank).thenComparingInt(baseIndex));
		return sorted;
	}
}
