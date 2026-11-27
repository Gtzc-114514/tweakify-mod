package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.BookScreenLayout;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MC-61489：书本查看界面的背景没有垂直居中。
 *
 * <p>原版 backgroundTop 固定返回 2；这里整体下移，页面按钮与底部控件都从
 * backgroundTop/menuControlsTop 派生，因此会一起移动，不需要逐个改坐标。
 */
@Mixin(BookViewScreen.class)
public abstract class BookViewScreenVerticalCenterMixin {
	@Unique
	private static final AtomicBoolean TWEAKIFY$BOOK_VIEW_CENTER_PROBE = new AtomicBoolean();

	@Shadow
	@Final
	protected static int IMAGE_HEIGHT;

	@ModifyReturnValue(method = "backgroundTop", at = @At("RETURN"), require = 0)
	private int tweakify$centerBookBackground(int original) {
		TweakifyModConfig config = TweakifyMod.config();
		boolean enabled = config != null && config.fix.bookGuiVerticalCenterFix
			&& config.fix.bookGuiCenterViewScreen;
		if (enabled) {
			HookProbe.markOnce(TWEAKIFY$BOOK_VIEW_CENTER_PROBE, "bookGuiVerticalCenterView");
		}
		return original + BookScreenLayout.verticalOffset(enabled,
			((Screen) (Object) this).height, IMAGE_HEIGHT);
	}
}
