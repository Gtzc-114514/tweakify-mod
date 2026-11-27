package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.state.ResourcePackCompatibilityState;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.packs.TransferableSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;

/**
 * 资源包“仅警告”：不兼容标记仍然显示，但玩家点击启用时不再弹确认框。
 *
 * <p>原版在 {@code PackEntry.handlePackSelection} 里根据兼容性决定直接 select 还是先打开
 * ConfirmScreen；这里只拦截不兼容分支，保留列表里的图标、名称与说明。
 * 目录判断沿用 ResourcePackCompatibilityMixin，确保数据包不会被误放行。
 */
@Mixin(TransferableSelectionList.PackEntry.class)
public class ResourcePackWarnOnlyMixin {
	@Shadow
	private PackSelectionModel.Entry pack;

	@Inject(method = "handlePackSelection", at = @At("HEAD"), cancellable = true, require = 0)
	private void tweakify$skipIncompatibleConfirm(CallbackInfo ci) {
		TweakifyModConfig config = TweakifyMod.config();
		if (!config.modified.disableResourcePackCompatibilityCheck
			|| !config.modified.resourcePackCompatibilityWarnOnly) {
			return;
		}
		if (this.pack.getCompatibility().isCompatible()) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (!(client.gui.screen() instanceof PackSelectionScreen screen)) {
			return;
		}
		Path packDirectory = ((PackSelectionScreenAccessor) screen).tweakify$getPackDir();
		Path resourcePackDirectory = client.getResourcePackDirectory();
		if (!ResourcePackCompatibilityState.shouldHideCompatibility(true,
			config.modified.resourcePackCompatibilityAlsoDataPacks,
			packDirectory, resourcePackDirectory)) {
			return;
		}
		this.pack.select();
		HookProbe.markOnce("resourcePackCompatibilityWarnOnly");
		ci.cancel();
	}
}
