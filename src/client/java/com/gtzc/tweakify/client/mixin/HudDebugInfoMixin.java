package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.DebugInfoOverlay;
import com.gtzc.tweakify.util.HookProbe;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 调试信息叠加层：在原版 HUD 提交完成后追加左下角的调试文字，
 * 因此不会被血条/快捷栏盖住，也不会影响原版 F3 调试屏。
 */
@Mixin(Hud.class)
public class HudDebugInfoMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Unique
	private static final AtomicBoolean TWEAKIFY$DEBUG_OVERLAY_PROBE = new AtomicBoolean();

	@Inject(method = "extractRenderState", at = @At("TAIL"), require = 0)
	private void tweakify$renderDebugInfo(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker,
										  CallbackInfo ci) {
		HookProbe.markOnce(TWEAKIFY$DEBUG_OVERLAY_PROBE, "debugInfoOverlay");
		DebugInfoOverlay.render(graphics, this.minecraft);
	}
}
