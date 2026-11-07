package com.gtzc.tweakify.client;

import com.google.gson.JsonElement;
import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.options.ConfigDouble;

/**
 * 轮廓粗细度滑块：按 BLOCK_OUTLINE_WIDTH_STEP 步进，默认值与范围直接引用 TweakifyModConfig 常量，
 * 避免在配置项、清洗与渲染层之间维护多份范围。
 * MaLiLib 的 ConfigDouble 没有步进参数，这里在写入处统一取整；
 * 取值范围仍由父类按 min/max 夹紧。
 */
public final class BlockOutlineWidthConfig extends ConfigDouble {
	public BlockOutlineWidthConfig(String name) {
		super(name, TweakifyModConfig.DEFAULT_BLOCK_OUTLINE_WIDTH,
			TweakifyModConfig.MIN_BLOCK_OUTLINE_WIDTH,
			TweakifyModConfig.MAX_BLOCK_OUTLINE_WIDTH, true);
	}

	@Override
	public void setDoubleValue(double value) {
		super.setDoubleValue(TweakifyModConfig.roundBlockOutlineWidth(value));
	}

	// 父类从 JSON 读取时直接写字段且会标记 clean；这里保持同样的语义，并补上 0.5 取整。
	@Override
	public void setValueFromJsonElement(JsonElement element) {
		if (element != null && element.isJsonPrimitive()) {
			try {
				setDoubleValue(element.getAsDouble());
				markClean();
				return;
			} catch (NumberFormatException ignored) {
				// 非数字字符串交给父类处理。
			}
		}
		super.setValueFromJsonElement(element);
	}
}
