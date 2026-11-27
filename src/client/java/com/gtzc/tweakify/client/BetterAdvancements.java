package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;

/**
 * 更好的进度（参考 {@code DaFuqs/PaginatedAdvancements}）的配置读取与分页几何。
 *
 * <p>把纯计算集中在这里，界面只负责绘制与命中判定，单元测试也能直接验证翻页边界。
 * 标签步长 = 原版 28 像素 + 配置间距，允许 0 ~ 8 像素的额外留白。
 */
public final class BetterAdvancements {
	public static final int TAB_WIDTH = 28;
	public static final int TAB_HEIGHT = 32;
	public static final int PINNED_TAB_WIDTH = 32;
	// 固定标签纹理左侧 3 像素透明；相对窗口右边界左移 4 像素，可见部分只压住外框 1 像素。
	public static final int PINNED_TAB_OVERLAP = 4;
	public static final int PINNED_TAB_HEIGHT = 28;
	public static final int NAV_BUTTON_SIZE = 23;
	public static final int PIN_BUTTON_SIZE = 18;
	// 原版进度视图的固定高度：AdvancementWidget.extractHover 用它判断信息框是否向上翻转。
	public static final int VANILLA_VIEW_HEIGHT = 113;
	// 顶部标签行相对窗口顶边的垂直偏移：标签高 32，向下露出 4 像素。
	public static final int TAB_ROW_OFFSET = 4;

	private BetterAdvancements() {
	}

	public static boolean enabled() {
		TweakifyModConfig config = TweakifyMod.config();
		return config != null && config.modified.betterAdvancements;
	}

	static TweakifyModConfig config() {
		return TweakifyMod.config();
	}

	public static int tabSpacing() {
		TweakifyModConfig config = config();
		return config == null ? TweakifyModConfig.DEFAULT_ADVANCEMENT_TAB_SPACING
			: config.modified.betterAdvancementsTabSpacing;
	}

	public static int pinnedTabSpacing() {
		TweakifyModConfig config = config();
		return config == null ? TweakifyModConfig.DEFAULT_ADVANCEMENT_PINNED_TAB_SPACING
			: config.modified.betterAdvancementsPinnedTabSpacing;
	}

	public static boolean pagination() {
		TweakifyModConfig config = config();
		return config == null || config.modified.betterAdvancementsPagination;
	}

	public static boolean pinTabs() {
		TweakifyModConfig config = config();
		return config == null || config.modified.betterAdvancementsPinTabs;
	}

	public static boolean rememberTab() {
		TweakifyModConfig config = config();
		return config == null || config.modified.betterAdvancementsRememberTab;
	}

	public static boolean disableFade() {
		TweakifyModConfig config = config();
		return config != null && config.modified.betterAdvancementsDisableFade;
	}

	public static boolean copyId() {
		TweakifyModConfig config = config();
		return config == null || config.modified.betterAdvancementsCopyId;
	}

	public static int tabStride() {
		return TAB_WIDTH + tabSpacing();
	}

	public static int pinnedTabStride() {
		return PINNED_TAB_HEIGHT + pinnedTabSpacing();
	}

	// 标签是否溢出：至少 3 个标签且一行放不下才进入分页（与参考实现一致）。
	public static boolean isPaginated(int tabCount, int startX, int endXTitle) {
		if (!pagination() || tabCount < 3) {
			return false;
		}
		return (long) endXTitle - startX < (long) tabCount * tabStride();
	}

	// 一页最多显示多少标签：分页时左右各留 23 像素给翻页按钮。
	public static int maxTabsPerPage(int startX, int endXTitle, boolean paginated) {
		int usable = endXTitle - startX - (paginated ? NAV_BUTTON_SIZE * 2 + 8 : 0);
		return Math.max(1, usable / tabStride());
	}

	public static int pageCount(int tabCount, int maxTabsPerPage) {
		if (tabCount <= 0 || maxTabsPerPage <= 0) {
			return 1;
		}
		return (tabCount + maxTabsPerPage - 1) / maxTabsPerPage;
	}

	public static int pageOf(int tabIndex, int maxTabsPerPage) {
		return maxTabsPerPage <= 0 ? 0 : tabIndex / maxTabsPerPage;
	}

	// 分页时位置 0 留给“上一页”按钮，因此页内位置从 1 开始；非分页时直接使用原索引。
	public static int positionOnPage(int tabIndex, int maxTabsPerPage, int currentPage, boolean paginated) {
		if (!paginated) {
			return tabIndex;
		}
		return 1 + tabIndex - maxTabsPerPage * currentPage;
	}

	public static boolean visibleOnPage(int tabIndex, int maxTabsPerPage, int currentPage, boolean paginated) {
		// 关闭分页时也必须限制在一行能容纳的标签数内，避免画到窗口外。
		return paginated ? pageOf(tabIndex, maxTabsPerPage) == currentPage : tabIndex < maxTabsPerPage;
	}

	// 标签左上角 x：分页时第一个位置在翻页按钮右侧。
	public static int tabX(int startX, int displayedPosition, boolean paginated) {
		int base = startX + (paginated ? NAV_BUTTON_SIZE + 4 : 0);
		int slot = paginated ? displayedPosition - 1 : displayedPosition;
		return base + slot * tabStride();
	}

	// 顶部标签的端帽索引：0 = 左端帽，7 = 右端帽，1 = 中间段。
	// 末页未满时最后一个标签的 position 是 visibleCount（分页）或 visibleCount - 1（非分页）。
	public static int tabSpriteIndex(int position, boolean paginated, int visibleCount) {
		int first = paginated ? 1 : 0;
		int last = paginated ? visibleCount : visibleCount - 1;
		return position == first ? 0 : (position == last ? 7 : 1);
	}

	// 固定标签使用 RIGHT 类型的 5 个槽位：0 = 首端帽，4 = 尾端帽，其余中间段。
	public static int pinnedSpriteIndex(int index, int visibleCount) {
		if (visibleCount <= 1) {
			return 0;
		}
		return index == 0 ? 0 : (index >= visibleCount - 1 ? 4 : 1);
	}

	// 将原版写死的 113 高度换算到实际视口：配合 pose 反向平移，信息框仍画在原位。
	public static int tooltipYOffset(int viewHeight) {
		return VANILLA_VIEW_HEIGHT - viewHeight;
	}

	// 背景平铺单元：小窗口保持原版 16 像素；大视口按面积放大单元，
	// 将每帧 blit 数限制在约 1000 次以内（原版固定 170 次）。
	public static int backgroundTileSize(int viewWidth, int viewHeight) {
		final int maxTiles = 1024;
		long area = (long) Math.max(1, viewWidth) * Math.max(1, viewHeight);
		int needed = (int) Math.ceil(Math.sqrt(area / (double) maxTiles));
		int tile = Math.max(16, ((needed + 15) / 16) * 16);
		return Math.min(128, tile);
	}

	// 固定标签最多能放几个：从标签行顶部空出 24 像素给固定按钮标题。
	public static int maxPinnedTabs(int startY, int endY) {
		int usable = endY - startY - 56;
		return Math.max(0, usable / pinnedTabStride());
	}

	public static int pinnedTabY(int startY, int index) {
		return startY + 24 + index * pinnedTabStride();
	}

	// 固定标签左边界：可见部分从窗口外框内侧开始，既不露出缝隙也不压住中间窗口。
	public static int pinnedTabX(int endXWindow) {
		return endXWindow - PINNED_TAB_OVERLAP;
	}
}
