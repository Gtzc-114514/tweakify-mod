package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.BookScreenLayout;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MC-61489：书与笔编辑界面的背景与文本框没有垂直居中。
 *
 * <p>背景、页面按钮与底部控件都从 backgroundTop/menuControlsTop 派生；
 * 只有 MultiLineEditBox 在 init 里用固定的 y=28，需要单独加上同一个偏移。
 */
@Mixin(BookEditScreen.class)
public abstract class BookEditScreenVerticalCenterMixin {
	@Unique
	private static final AtomicBoolean TWEAKIFY$BOOK_EDIT_CENTER_PROBE = new AtomicBoolean();
	// 背景与编辑框是两个独立注入点，必须分开探针，否则先命中者会让另一个永不打印。
	@Unique
	private static final AtomicBoolean TWEAKIFY$BOOK_EDIT_BACKGROUND_PROBE = new AtomicBoolean();

	@Shadow
	@Final
	public static int IMAGE_HEIGHT;

	@ModifyArg(method = "init",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/components/MultiLineEditBox$Builder;setY(I)"
				+ "Lnet/minecraft/client/gui/components/MultiLineEditBox$Builder;"),
		require = 0)
	private int tweakify$centerEditBox(int original) {
		boolean enabled = bookCenterEnabled();
		if (enabled) {
			HookProbe.markOnce(TWEAKIFY$BOOK_EDIT_CENTER_PROBE, "bookGuiVerticalCenterEdit");
		}
		return original + BookScreenLayout.verticalOffset(enabled,
			((Screen) (Object) this).height, IMAGE_HEIGHT);
	}

	@ModifyReturnValue(method = "backgroundTop", at = @At("RETURN"), require = 0)
	private int tweakify$centerBookBackground(int original) {
		boolean enabled = bookCenterEnabled();
		if (enabled) {
			HookProbe.markOnce(TWEAKIFY$BOOK_EDIT_BACKGROUND_PROBE, "bookGuiVerticalCenterBackground");
		}
		return original + BookScreenLayout.verticalOffset(enabled,
			((Screen) (Object) this).height, IMAGE_HEIGHT);
	}

	@Unique
	private static boolean bookCenterEnabled() {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		return config != null && config.fix.bookGuiVerticalCenterFix
			&& config.fix.bookGuiCenterEditScreen;
	}
}
