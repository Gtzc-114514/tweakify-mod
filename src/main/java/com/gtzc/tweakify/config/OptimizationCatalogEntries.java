package com.gtzc.tweakify.config;

import java.util.List;

import static com.gtzc.tweakify.config.ConfigCatalog.*;

/**
 * 「优化」标签的配置目录条目。
 *
 * <p>条目顺序即界面顺序：调用方 {@link ConfigCatalog#buildEntries()} 按原表顺序
 * 逐段调用 addPartN。新增条目请追加到对应标签的最后一段，不要跨段搬动。
 */
final class OptimizationCatalogEntries {
	private OptimizationCatalogEntries() {
	}

	// 原表中的第 1 段连续条目。
	static void addPart1(List<Entry> entries) {
		// MiniHUD 掉帧优化（优化标签）
		entries.add(optimization("miniHudFpsOptimization", true));
		entries.add(child("miniHudFpsOptimization", "miniHudRefreshIntervalMs"));
		// 告示牌文本优化（优化标签）
		entries.add(optimization("signTextOptimization", true));
		entries.add(child("signTextOptimization", "signTextOptimizationCachedText"));
		entries.add(child("signTextOptimization", "signTextOptimizationSkipTicking"));
		entries.add(child("signTextOptimization", "signTextOptimizationSkipFiltering"));
	}
}
