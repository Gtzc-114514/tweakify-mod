package com.gtzc.tweakify.client.mixin;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 允许在 F9 高清截图后重新应用全屏/窗口模式。
 */
@Mixin(Window.class)
public interface WindowInvoker {
	@Invoker("setMode")
	void tweakify$setMode();
}
