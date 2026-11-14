package com.gtzc.tweakify.config;

/**
 * 测试分区：开发/排查用的辅助功能，正常游玩时全部默认关闭。
 *
 * <p>字段全部是布尔开关，由 facade 的按类型清洗统一兜底，因此不登记字段级校验器。
 */
public final class TestingConfig extends ConfigSection {
	TestingConfig() {
		// 全部字段都是布尔开关，这里只需要确认没有漏登记的数值/枚举字段。
		assertSanitizerCoverage(java.util.List.of());
	}

	// 调试信息叠加层：在 HUD 左下角显示坐标、维度、朝向与脚下方块等运行时信息。
	public volatile boolean debugInfoOverlay = false;

	// 叠加层的切换快捷键。
	public volatile String hotkeyDebugInfoOverlay = "";

	// 子开关：追加帧率与内存占用。
	public volatile boolean debugInfoShowPerformance = true;

	// 子开关：追加准星指向的方块/实体 ID。
	public volatile boolean debugInfoShowTarget = true;

	// 子开关：追加所在生物群系。
	public volatile boolean debugInfoShowBiome = false;

	// 子开关：追加玩家水平/垂直速度（格每秒）。
	public volatile boolean debugInfoShowSpeed = false;

	// 子开关：追加 32 格内实体分类统计（生物/掉落物/其他）。
	public volatile boolean debugInfoShowEntities = false;

	// 子开关：追加已启用功能数与已修改配置项数。
	public volatile boolean debugInfoShowConfigStats = false;

	// 子开关：追加当前药水效果数量与最短剩余时间。
	public volatile boolean debugInfoShowEffects = false;

	// 子开关：追加已激活的 Mixin 行为探针列表（见 HookProbe）。
	public volatile boolean debugInfoShowHooks = false;

	// 探针报表的纯快捷键动作：没有同名的值字段，只有快捷键字段。
	public volatile String hotkeyDebugProbeReport = "";

	// 探针报表输出到剪贴板（关闭时输出到聊天栏）。
	public volatile boolean debugProbeReportCopy = false;

	// 已启用功能报表的纯快捷键动作：把启用的功能根开关列表输出到聊天栏。
	public volatile String hotkeyDebugEnabledFeaturesReport = "";
}
