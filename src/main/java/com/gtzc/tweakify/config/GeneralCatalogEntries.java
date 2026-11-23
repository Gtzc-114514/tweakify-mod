package com.gtzc.tweakify.config;

import java.util.List;

import static com.gtzc.tweakify.config.ConfigCatalog.*;

/**
 * 「通用」标签的配置目录条目。
 *
 * <p>条目顺序即界面顺序：调用方 {@link ConfigCatalog#buildEntries()} 按原表顺序
 * 逐段调用 addPartN。新增条目请追加到对应标签的最后一段，不要跨段搬动。
 */
final class GeneralCatalogEntries {
	private GeneralCatalogEntries() {
	}

	// 原表中的第 1 段连续条目。
	static void addPart1(List<Entry> entries) {
		// 界面语言
		entries.add(root("language"));
		entries.add(child("language", "followSystemLanguage"));
		entries.add(root("openConfigGui"));
		// 标签记忆（通用标签）
		entries.add(root("rememberSelectedTab"));
		entries.add(child("rememberSelectedTab", "rememberSelectedTabFallback"));
	}

	// 原表中的第 2 段连续条目。
	static void addPart2(List<Entry> entries) {
		// 滚动列表记忆（通用标签）
		entries.add(general("scrollListMemory", true));
		entries.add(child("scrollListMemory", "scrollListMemoryPersist"));
		entries.add(child("scrollListMemory", "malilibScrollMemory"));
		entries.add(child("scrollListMemory", "modMenuScrollMemory"));
		// 配置界面交互（通用标签）：搜索关键词高亮、平滑滚动与拖动滚动（同时作用于创造栏/聊天/Mod Menu）
		// 收藏系统父项：收藏星样式与收藏包含子项作为子设置。
		entries.add(general("favoriteSystem", false));
		entries.add(child("favoriteSystem", "favoriteStarStyle"));
		entries.add(child("favoriteSystem", "favoriteIncludeChildren"));
		entries.add(general("searchHighlight", true));
		entries.add(child("searchHighlight", "searchHighlightColor"));
		entries.add(general("smoothScroll", true));
		entries.add(child("smoothScroll", "smoothScrollConfigList"));
		entries.add(child("smoothScroll", "smoothScrollCreative"));
		entries.add(child("smoothScroll", "smoothScrollChat"));
		entries.add(child("smoothScroll", "smoothScrollModMenu"));
		entries.add(child("smoothScroll", "smoothScrollDuration"));
	}
}
