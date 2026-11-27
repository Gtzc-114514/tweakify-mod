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

	// 子开关：缓存 hasMessage 结果。
	public volatile boolean signTextOptimizationCachedText = true;

	// 子开关：跳过客户端告示牌 block entity tick。
	public volatile boolean signTextOptimizationSkipTicking = true;

	// 子开关：告示牌渲染跳过文本过滤（会忽略服务器过滤后的文本）。
	public volatile boolean signTextOptimizationSkipFiltering = true;

	// 消耗品优化：客户端本地完成进食/饮用，不再等服务器往返确认（仅远程服务器生效）。
	public volatile boolean consumableOptimization = false;

	// 子开关：本地立即播放进食音效与粒子，不等服务器反馈。
	public volatile boolean consumableOptimizationInstantFeedback = true;

	// 子开关：吃掉服务器延迟到达的完成事件与重复音效。
	public volatile boolean consumableOptimizationSuppressServerFeedback = true;

	// 子开关：吃完最后一个物品并自动切换时跳过快照式手部动画。
	public volatile boolean consumableOptimizationSkipHandSwapAnimation = true;

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
