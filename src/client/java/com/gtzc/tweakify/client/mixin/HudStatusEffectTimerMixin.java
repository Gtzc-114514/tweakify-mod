package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.StatusEffectTimerRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 药水效果时间显示：在 {@code Hud.extractEffects} 完成图标提交后，
 * 按同一套排列规则在每个图标下方补画剩余时间。
 */
@Mixin(Hud.class)
public class HudStatusEffectTimerMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "extractEffects", at = @At("TAIL"))
	private void tweakify$renderEffectTimers(GuiGraphicsExtractor gui, DeltaTracker delta, CallbackInfo ci) {
		StatusEffectTimerRenderer.render(gui, this.minecraft);
	}
}
