package com.gtzc.tweakify.client.mixin;

import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 读取并写回原版进度标签的滚动位置与内容尺寸。
 *
 * <p>平滑滚动由 {@link AdvancementsScreenSmoothScrollMixin} 驱动：
 * 每帧把缓动后的位置写回 scrollX/scrollY，原版剩下的裁剪与背景平铺逻辑不变。
 */
@Mixin(AdvancementTab.class)
public interface AdvancementTabAccessor {
	@Accessor("scrollX")
	double tweakify$getScrollX();

	@Accessor("scrollX")
	void tweakify$setScrollX(double value);

	@Accessor("scrollY")
	double tweakify$getScrollY();

	@Accessor("scrollY")
	void tweakify$setScrollY(double value);

	@Accessor("maxX")
	int tweakify$getMaxX();

	@Accessor("maxY")
	int tweakify$getMaxY();
}
