package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.ExperienceBar;
import net.minecraft.client.gui.contextualbar.LocatorBar;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 经验条与定位栏同时显示（参考 {@code Haage001/locator-heads} 的 alwaysShowXP）：
 * 经验条画完之后再补一层定位栏标记——只画标记、不画定位栏底图，否则会盖住经验条；
 * 头像替换依旧走 {@code LocatorBar} 自己的圆点绘制。
 */
@Mixin(ExperienceBar.class)
public abstract class ExperienceBarAlwaysShowXpMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "extractRenderState", at = @At("RETURN"))
	private void tweakify$overlayLocatorMarkers(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker,
												CallbackInfo ci) {
		TweakifyModConfig config = TweakifyMod.config();
		if (!config.functions.locatorBarHeads
			|| !config.functions.locatorBarAlwaysShowXp) {
			return;
		}
		if (this.minecraft.level == null || this.minecraft.getCameraEntity() == null) {
			return;
		}
		new LocatorBar(this.minecraft).extractRenderState(graphics, deltaTracker);
	}
}
