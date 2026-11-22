package com.gtzc.tweakify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 优化分区：性能相关的可选改进。
 *
 * <p>布尔字段由 facade 的按类型清洗统一兜底；数值字段需要登记字段级校验器。
 */
public final class OptimizationConfig extends ConfigSection {
	private static final Logger LOGGER = LoggerFactory.getLogger("tweakify-mod-config");

	// 校验器绑定到当前实例：方法引用需要接收者，静态字段无法绑定。
	private final transient List<FieldSanitizer> sanitizers;

	OptimizationConfig() {
		this.sanitizers = List.of(
			FieldSanitizer.of("miniHudRefreshIntervalMs", this::sanitizeMiniHudInterval));
		assertSanitizerCoverage(fieldNamesOf(this.sanitizers));
	}

	// MiniHUD 掉帧优化：放宽 HUD 行刷新间隔，减少重复重算造成的掉帧。
	public volatile boolean miniHudFpsOptimization = false;

	// 告示牌文本优化：缓存 hasMessage 结果并跳过客户端告示牌 tick。
	public volatile boolean signTextOptimization = false;

	// HUD 行刷新间隔（毫秒，50 ~ 1000，默认 150）。
	public volatile int miniHudRefreshIntervalMs = TweakifyModConfig.DEFAULT_MINIHUD_REFRESH_INTERVAL_MS;

	@Override
	boolean sanitize() {
		return runSanitizers(this.sanitizers);
	}

	@Override
	List<String> sanitizedFieldNames() {
		return fieldNamesOf(this.sanitizers);
	}

	// 修正刷新间隔，手工改坏配置时夹回允许范围。
	private boolean sanitizeMiniHudInterval() {
		int clamped = Math.max(TweakifyModConfig.MIN_MINIHUD_REFRESH_INTERVAL_MS,
			Math.min(TweakifyModConfig.MAX_MINIHUD_REFRESH_INTERVAL_MS, miniHudRefreshIntervalMs));
		if (clamped == miniHudRefreshIntervalMs) {
			return false;
		}
		LOGGER.warn("Clamping miniHudRefreshIntervalMs from {} to {}", miniHudRefreshIntervalMs, clamped);
		miniHudRefreshIntervalMs = clamped;
		return true;
	}
}
