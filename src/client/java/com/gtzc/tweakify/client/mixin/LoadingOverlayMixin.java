package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 资源包加载屏幕加速（对齐 ForceCloseLoadingScreen）：
 * 去掉淡入、立即可淡出、淡出结束后直接移除覆盖层。
 */
@Mixin(LoadingOverlay.class)
public class LoadingOverlayMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Shadow
	private long fadeOutStart;

	// 加载界面的渲染状态每帧提取一次：三处注入点都用 AtomicBoolean 版本，避免热路径上的集合哈希。
	@Unique
	private static final AtomicBoolean TWEAKIFY$REMOVE_OVERLAY_PROBE = new AtomicBoolean();
	@Unique
	private static final AtomicBoolean TWEAKIFY$NO_FADE_IN_PROBE = new AtomicBoolean();
	@Unique
	private static final AtomicBoolean TWEAKIFY$READY_TO_FADE_PROBE = new AtomicBoolean();

	@Inject(method = "extractRenderState", at = @At("TAIL"), require = 0)
	private void tweakify$removeAfterFadeOut(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick,
											 CallbackInfo ci) {
		HookProbe.markOnce(TWEAKIFY$REMOVE_OVERLAY_PROBE, "loadingScreenRemoveOverlay");
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config != null && config.modified.forceCloseLoadingScreenSkipResourcePackFade && this.fadeOutStart != -1) {
			this.minecraft.gui.setOverlay(null);
		}
	}

	@WrapOperation(method = "extractRenderState", require = 0, at = @At(value = "FIELD",
		target = "Lnet/minecraft/client/gui/screens/LoadingOverlay;fadeIn:Z", opcode = Opcodes.GETFIELD))
	private boolean tweakify$noFadeIn(LoadingOverlay instance, Operation<Boolean> original) {
		HookProbe.markOnce(TWEAKIFY$NO_FADE_IN_PROBE, "loadingScreenNoFadeIn");
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config != null && config.modified.forceCloseLoadingScreenSkipResourcePackFade) {
			return false;
		}
		return original.call(instance);
	}

	@ModifyReturnValue(method = "isReadyToFadeOut", at = @At("RETURN"), require = 0)
	private boolean tweakify$readyToFadeOut(boolean original) {
		HookProbe.markOnce(TWEAKIFY$READY_TO_FADE_PROBE, "loadingScreenReadyToFadeOut");
		TweakifyModConfig config = TweakifyMod.CONFIG;
		return config != null && config.modified.forceCloseLoadingScreenSkipResourcePackFade ? true : original;
	}
}
