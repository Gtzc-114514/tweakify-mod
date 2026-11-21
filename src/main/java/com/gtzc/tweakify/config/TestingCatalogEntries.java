package com.gtzc.tweakify.config;

import java.util.List;

import static com.gtzc.tweakify.config.ConfigCatalog.*;

/**
 * 「测试」标签的配置目录条目。
 *
 * <p>条目顺序即界面顺序：调用方 {@link ConfigCatalog#buildEntries()} 按原表顺序
 * 逐段调用 addPartN。新增条目请追加到对应标签的最后一段，不要跨段搬动。
 */
final class TestingCatalogEntries {
	private TestingCatalogEntries() {
	}

	// 原表中的第 1 段连续条目。
	static void addPart1(List<Entry> entries) {
		// 调试信息叠加层（测试标签）
		entries.add(testing("debugInfoOverlay", true));
		entries.add(child("debugInfoOverlay", "debugInfoShowPerformance"));
		entries.add(child("debugInfoOverlay", "debugInfoShowTarget"));
		entries.add(child("debugInfoOverlay", "debugInfoShowBiome"));
		entries.add(child("debugInfoOverlay", "debugInfoShowSpeed"));
		entries.add(child("debugInfoOverlay", "debugInfoShowEntities"));
		entries.add(child("debugInfoOverlay", "debugInfoShowConfigStats"));
		entries.add(child("debugInfoOverlay", "debugInfoShowEffects"));
		entries.add(child("debugInfoOverlay", "debugInfoShowHooks"));
		// 探针报表（测试标签，纯快捷键动作：把已激活的 Mixin 钩子打到聊天栏或剪贴板）
		entries.add(testing("debugProbeReport", false));
		entries.add(child("debugProbeReport", "debugProbeReportCopy"));
		// 已启用功能报表（测试标签，纯快捷键动作）
		entries.add(testing("debugEnabledFeaturesReport", false));
		entries.add(child("debugEnabledFeaturesReport", "debugEnabledFeaturesReportCopy"));
	}
}
