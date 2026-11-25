package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import com.mojang.serialization.Lifecycle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 禁用实验性功能提示：创建世界时直接跳过实验性生命周期警告。
 *
 * <p>原版在正式版里创建带实验性内容的世界会弹出 ConfirmScreen；
 * 开启后直接执行原任务的 {@code task.run()}，世界仍会按原版标记为实验性。
 */
@Mixin(WorldOpenFlows.class)
public abstract class WorldOpenFlowsExperimentalWarningMixin {
	@Inject(method = "confirmWorldCreation", at = @At("HEAD"), cancellable = true)
	private static void tweakify$skipExperimentalWorldWarning(Minecraft minecraft, CreateWorldScreen parent,
															  Lifecycle lifecycle, Runnable task,
															  boolean skipWarning, CallbackInfo ci) {
		if (skipWarning || lifecycle != Lifecycle.experimental()) {
			return;
		}
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.modified.disableExperimentalWarning
			|| !config.modified.experimentalWarningWorldCreate) {
			return;
		}
		HookProbe.markOnce("disableExperimentalWarning");
		task.run();
		ci.cancel();
	}
}
