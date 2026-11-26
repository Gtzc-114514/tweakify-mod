package com.gtzc.tweakify.client.mixin;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.server.packs.resources.IoSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 允许在 F9 高清截图后重新应用全屏/窗口模式，并保存/恢复窗口化几何。
 * 另提供自定义图标入口：直接调用私有的 setIcon(List)。
 */
@Mixin(Window.class)
public interface WindowInvoker {
	@Invoker("setMode")
	void tweakify$setMode();

	@Invoker("setIcon")
	void tweakify$setIcon(List<IoSupplier<InputStream>> icons) throws IOException;

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
