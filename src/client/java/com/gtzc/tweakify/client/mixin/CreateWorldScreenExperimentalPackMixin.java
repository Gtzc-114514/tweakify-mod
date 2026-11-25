package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 禁用实验性功能提示：应用实验性数据包时跳过确认界面。
 *
 * <p>原版在数据包选择页检测到实验性功能会先打开 ConfirmExperimentalFeaturesScreen；
 * 把这次检测结果改为 false 后会直接走加载分支，相当于自动点击“继续”。
 */
@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenExperimentalPackMixin {
	@Redirect(method = "tryApplyNewDataPacks",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/flag/FeatureFlags;isExperimental("
				+ "Lnet/minecraft/world/flag/FeatureFlagSet;)Z"))
	private static boolean tweakify$skipExperimentalPackConfirm(FeatureFlagSet flags) {
		boolean experimental = FeatureFlags.isExperimental(flags);
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (experimental && config != null && config.modified.disableExperimentalWarning
			&& config.modified.experimentalWarningDataPacks) {
			HookProbe.markOnce("disableExperimentalWarningPacks");
			return false;
		}
		return experimental;
	}
}
