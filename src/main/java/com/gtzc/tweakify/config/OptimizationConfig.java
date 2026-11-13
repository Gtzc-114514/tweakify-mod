package com.gtzc.tweakify.config;

/**
 * 优化分区：性能相关的可选改进。
 *
 * <p>字段全部是布尔开关，由 facade 的按类型清洗统一兜底，因此不登记字段级校验器。
 */
public final class OptimizationConfig extends ConfigSection {
	OptimizationConfig() {
		// 全部字段都是布尔开关，这里只需要确认没有漏登记的数值/枚举字段。
		assertSanitizerCoverage(java.util.List.of());
	}

	// MiniHUD 掉帧优化：放宽 HUD 行刷新间隔，减少重复重算造成的掉帧。
	public volatile boolean miniHudFpsOptimization = false;
}
