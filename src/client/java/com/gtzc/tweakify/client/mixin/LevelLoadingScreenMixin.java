package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 强制关闭加载屏幕：加载世界时立即关掉“正在加载地形”界面，提前看到世界；
 * 世界仍在后台继续加载。
 *
 * <p>只在界面已经成为当前界面后关闭，不会打断原版自己的加载流程；
 * 原版若再次打开加载界面，下一 tick 会再次关闭。
 */
@Mixin(LevelLoadingScreen.class)
public class LevelLoadingScreenMixin {
	@Inject(method = "tick", at = @At("HEAD"), require = 0)
	private void tweakify$forceCloseLoadingScreen(CallbackInfo ci) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.forceCloseLoadingScreen) {
			return;
		}
		HookProbe.markOnce("forceCloseLoadingScreen");
		Minecraft client = Minecraft.getInstance();
		if (client.gui.screen() == (Object) this) {
			client.gui.setScreen(null);
		}
	}
}
