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

	// 子开关：追加已激活的 Mixin 行为探针列表（见 HookProbe）。
	public volatile boolean debugInfoShowHooks = false;

	// 探针报表的纯快捷键动作：没有同名的值字段，只有快捷键字段。
	public volatile String hotkeyDebugProbeReport = "";
}
