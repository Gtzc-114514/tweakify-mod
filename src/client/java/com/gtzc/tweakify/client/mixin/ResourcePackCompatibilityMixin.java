package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.ResourcePackCompatibilityState;
import com.gtzc.tweakify.client.ResourcePackCompatibilityTargets;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.server.packs.repository.PackCompatibility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 关闭资源包兼容性提示：资源包界面把不兼容状态视为兼容，
 * 因此列表不再显示不兼容标记，启用时也不会再弹 ConfirmScreen。
 * 通过当前界面目录与 Minecraft.getResourcePackDirectory() 比较，数据包保持原版行为。
 */
@Mixin(PackSelectionModel.EntryBase.class)
public abstract class ResourcePackCompatibilityMixin {
	@Unique
	private static final AtomicBoolean TWEAKIFY$REPORTED = new AtomicBoolean();

	// 用 require = 0 避免单个界面功能拖垮启动；目标缺失由启动自检输出 ERROR，
	// 实际注入是否生效由 HookProbe 的 resourcePackCompatibility 日志确认。
	@Inject(method = ResourcePackCompatibilityTargets.ENTRY_COMPATIBILITY_METHOD,
		at = @At("RETURN"), cancellable = true, require = 0)
	private void tweakify$hideIncompatible(CallbackInfoReturnable<PackCompatibility> cir) {
		// 入口探针：即使开关关闭或资源包本身兼容，也能确认注入点活着。
		HookProbe.markOnce(TWEAKIFY$REPORTED, "resourcePackCompatibility");
		PackCompatibility original = cir.getReturnValue();
		if (original == null || original == PackCompatibility.COMPATIBLE) {
			return;
		}
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null) {
			return;
		}
		if (!(Minecraft.getInstance().gui.screen() instanceof PackSelectionScreen screen)) {
			return;
		}
		Path packDirectory = ((PackSelectionScreenAccessor) screen).tweakify$getPackDir();
		Path resourcePackDirectory = Minecraft.getInstance().getResourcePackDirectory();
		if (!ResourcePackCompatibilityState.shouldHideCompatibility(
			config.disableResourcePackCompatibilityCheck, packDirectory, resourcePackDirectory)) {
			return;
		}
		cir.setReturnValue(PackCompatibility.COMPATIBLE);
	}
}
