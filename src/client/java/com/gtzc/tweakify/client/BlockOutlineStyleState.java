package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.util.Mth;

/**
 * 自定义方块轮廓的颜色与线宽计算。
 *
 * <p>颜色模式与调色板都是纯函数，便于单测；渲染层每帧把当前时间传入，
 * 由这里统一处理固定颜色、彩虹循环与亮度呼吸。
 */
public final class BlockOutlineStyleState {
	private static final long RAINBOW_PERIOD_MILLIS = 3000L;
	private static final long PULSE_PERIOD_MILLIS = 2000L;
	private static final float PULSE_MIN_BRIGHTNESS = 0.45f;

	private BlockOutlineStyleState() {
	}

	// 线宽：统一走配置步进与范围夹紧工具，并转成渲染层需要的 float。
	public static float width(double configured) {
		return (float) TweakifyModConfig.clampBlockOutlineWidth(configured);
	}

	// 高对比度模式由原版接管颜色与线宽，自定义样式必须整体让位。
	public static boolean shouldApplyCustomStyle(boolean customEnabled, boolean highContrast) {
		return customEnabled && !highContrast;
	}

	// 按彩色模式与调色板计算当前颜色；未知 code 会回退到默认色/单色模式。
	public static int color(String modeCode, String paletteCode, int speedPercent, long timeMillis) {
		BlockOutlineColorMode mode = BlockOutlineColorMode.fromCode(modeCode);
		int base = BlockOutlinePalette.fromCode(paletteCode).argb();
		return switch (mode) {
			case PALETTE -> base;
			case RAINBOW -> rainbowColor(speedPercent, timeMillis);
			case PULSE -> pulseColor(base, speedPercent, timeMillis);
		};
	}

	// 动画周期：速度 100 为基准，200 表示快一倍，50 表示慢一倍。
	static long rainbowPeriodMillis(int speedPercent) {
		return Math.max(1L, RAINBOW_PERIOD_MILLIS * 100L / clampAnimationSpeed(speedPercent));
	}

	static long pulsePeriodMillis(int speedPercent) {
		return Math.max(1L, PULSE_PERIOD_MILLIS * 100L / clampAnimationSpeed(speedPercent));
	}

	static int clampAnimationSpeed(int speedPercent) {
		return Math.max(TweakifyModConfig.MIN_BLOCK_OUTLINE_ANIMATION_SPEED,
			Math.min(TweakifyModConfig.MAX_BLOCK_OUTLINE_ANIMATION_SPEED, speedPercent));
	}

	// 彩虹：色相在周期内完整循环一圈，保持完全不透明。
	static int rainbowColor(int speedPercent, long timeMillis) {
		long period = rainbowPeriodMillis(speedPercent);
		long phase = Math.floorMod(timeMillis, period);
		float hue = phase / (float) period;
		return Mth.hsvToArgb(hue, 1.0f, 1.0f, 255);
	}

	// 呼吸：保留色相与 alpha，只把亮度在 45% ~ 100% 之间周期变化。
	static int pulseColor(int baseArgb, int speedPercent, long timeMillis) {
		long period = pulsePeriodMillis(speedPercent);
		long phase = Math.floorMod(timeMillis, period);
		float wave = (float) Math.sin(phase / (double) period * Math.PI * 2.0);
		float brightness = PULSE_MIN_BRIGHTNESS + (1.0f - PULSE_MIN_BRIGHTNESS) * (0.5f + 0.5f * wave);
		int alpha = (baseArgb >>> 24) & 0xFF;
		int red = Math.round(((baseArgb >>> 16) & 0xFF) * brightness);
		int green = Math.round(((baseArgb >>> 8) & 0xFF) * brightness);
		int blue = Math.round((baseArgb & 0xFF) * brightness);
		return (alpha << 24) | (red << 16) | (green << 8) | blue;
	}
}
