package com.gtzc.tweakify.client.mixin.modmenu;

import com.gtzc.tweakify.client.ScrollMemory;
import com.gtzc.tweakify.util.HookProbe;
import com.terraformersmc.modmenu.gui.ModsScreen;
import com.terraformersmc.modmenu.gui.widget.ModListWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 滚动列表记忆（Mod Menu 模组列表）：记下滚动位置，下次打开 Mod Menu 时恢复。
 *
 * <p>Mod Menu 不是必需依赖，本 Mixin 由 {@link ModMenuMixinPlugin} 在未安装时整体跳过。
 */
@Mixin(ModsScreen.class)
public class ModsScreenScrollMixin {
	@Shadow
	private ModListWidget modList;

	@Unique
	private boolean tweakify$scrollRestored;

	@Inject(method = "init", at = @At("TAIL"))
	private void tweakify$restoreScroll(CallbackInfo ci) {
		if (this.tweakify$scrollRestored) {
			return;
		}
		// init 在窗口缩放时也会执行，只在首次打开时恢复。
		this.tweakify$scrollRestored = true;
		if (!ScrollMemory.isModMenuEnabled() || this.modList == null) {
			return;
		}
		Integer saved = ScrollMemory.getModMenu();
		if (saved != null) {
			// 条目已在 finalizeInit 里建好，越界值会被夹回合法范围。
			this.modList.setScrollAmount(saved);
		}
	}

	// 每帧记录：不论从哪个入口离开界面（完成、Esc、打开模组配置），关闭后都能回到上次位置。
	// 这里只改内存值，写盘由关闭钩子或定时器统一刷出，避免每帧同步 IO。
	@Inject(method = "extractRenderState", at = @At("HEAD"))
	private void tweakify$saveScroll(GuiGraphicsExtractor gui, int mouseX, int mouseY, float delta,
									 CallbackInfo ci) {
		if (ScrollMemory.isModMenuEnabled() && this.modList != null) {
			ScrollMemory.putModMenu((int) this.modList.scrollAmount());
		}
	}

	// 关闭界面时立即刷盘（需开启跨重启记忆才会真正写）。
	@Inject(method = "removed", at = @At("HEAD"), require = 0)
	private void tweakify$flushScroll(CallbackInfo ci) {
		HookProbe.markOnce("modMenuScrollFlush");
		ScrollMemory.flush();
	}
}
