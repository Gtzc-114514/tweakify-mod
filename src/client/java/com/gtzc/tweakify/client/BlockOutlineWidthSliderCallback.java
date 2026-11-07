package com.gtzc.tweakify.client;

import fi.dy.masa.malilib.config.IConfigDouble;
import fi.dy.masa.malilib.config.gui.SliderCallbackDouble;
import fi.dy.masa.malilib.gui.button.ButtonBase;

import java.util.Locale;

/**
 * 轮廓线宽滑块的显示值：保留一位小数，避免 MaLiLib 默认的 "%.4f" 显示成 2.5000。
 * 实际的步进取整仍由 BlockOutlineWidthConfig / TweakifyModConfig 统一处理。
 */
public final class BlockOutlineWidthSliderCallback extends SliderCallbackDouble {
	public BlockOutlineWidthSliderCallback(IConfigDouble config, ButtonBase resetButton) {
		super(config, resetButton);
	}

	@Override
	public String getFormattedDisplayValue() {
		return String.format(Locale.ROOT, "%.1f", this.config.getDoubleValue());
	}
}
