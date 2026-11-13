package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.BlockOutlineStyleState;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 自定义方块轮廓：在 LevelRenderer 提交轮廓前替换颜色与线宽。
 *
 * <p>submitHitOutline 的参数里 color 是唯一 int、width 是唯一 float，
 * argsOnly + HEAD 精确命中这两个参数；开关关闭时原样返回，不影响原版高对比度颜色。
 * 高对比度选项开启时也完全保留原版轮廓（黑色加粗外框 + HC 主色 + 7.0 线宽）。
 * 注意：DEBUG_SHAPES 下原版不使用 color 参数，颜色在该模式下不生效，线宽仍然生效。
 */
@Mixin(LevelRenderer.class)
public class LevelRendererOutlineStyleMixin {
	@Unique
	private static final AtomicBoolean TWEAKIFY$REPORTED = new AtomicBoolean();

	@ModifyVariable(method = "submitHitOutline", at = @At("HEAD"), argsOnly = true, index = 5, require = 0)
	private int tweakify$customOutlineColor(int original) {
		HookProbe.markOnce(TWEAKIFY$REPORTED, "customBlockOutline");
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !BlockOutlineStyleState.shouldApplyCustomStyle(config.functions.customBlockOutline,
			Minecraft.getInstance().options.highContrastBlockOutline().get())) {
			return original;
		}
		return BlockOutlineStyleState.color(config.functions.blockOutlineColorMode, config.functions.blockOutlinePalette,
			config.functions.blockOutlineAnimationSpeed, System.nanoTime() / 1_000_000L);
	}

	@ModifyVariable(method = "submitHitOutline", at = @At("HEAD"), argsOnly = true, index = 6, require = 0)
	private float tweakify$customOutlineWidth(float original) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !BlockOutlineStyleState.shouldApplyCustomStyle(config.functions.customBlockOutline,
			Minecraft.getInstance().options.highContrastBlockOutline().get())) {
			return original;
		}
		return BlockOutlineStyleState.width(config.functions.blockOutlineWidth);
	}
}
