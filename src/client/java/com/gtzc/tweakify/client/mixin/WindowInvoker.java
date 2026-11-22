package com.gtzc.tweakify.client.mixin;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 允许在 F9 高清截图后重新应用全屏/窗口模式，并保存/恢复窗口化几何。
 */
@Mixin(Window.class)
public interface WindowInvoker {
	@Invoker("setMode")
	void tweakify$setMode();

	@Invoker("isWindowFullscreen")
	boolean tweakify$isWindowFullscreen();

	@Accessor("windowedX")
	int tweakify$getWindowedX();

	@Accessor("windowedX")
	void tweakify$setWindowedX(int value);

	@Accessor("windowedY")
	int tweakify$getWindowedY();

	@Accessor("windowedY")
	void tweakify$setWindowedY(int value);

	@Accessor("windowedWidth")
	int tweakify$getWindowedWidth();

	@Accessor("windowedWidth")
	void tweakify$setWindowedWidth(int value);

	@Accessor("windowedHeight")
	int tweakify$getWindowedHeight();

	@Accessor("windowedHeight")
	void tweakify$setWindowedHeight(int value);
}
