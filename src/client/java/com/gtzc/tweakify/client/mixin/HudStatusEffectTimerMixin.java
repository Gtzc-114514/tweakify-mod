package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.render.StatusEffectTimerRenderer;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
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
 * 更好的药水显示：包裹 {@code Hud.extractEffects}，按配置整体缩放右侧效果图标与时间文字。
 * 时间文字在同一个缩放矩阵内绘制，因此数字会随图标一起变大变小。
 */
@Mixin(Hud.class)
public class HudStatusEffectTimerMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	// try/finally 保证异常时矩阵栈仍能恢复；缩放围绕右上角，避免图标移出屏幕右缘。
	// 时间文字由下方的 TAIL 注入绘制：TAIL 落在 wrapped 方法内部，既在缩放矩阵内，
	// 又保留原版“未显示效果时提前返回”的语义。
	@WrapMethod(method = "extractEffects")
	private void tweakify$scaledEffectDisplay(GuiGraphicsExtractor gui, DeltaTracker delta, Operation<Void> original) {
		float scale = StatusEffectTimerRenderer.effectScale(TweakifyMod.config());
		if (scale == 1.0f) {
			original.call(gui, delta);
			return;
		}
		StatusEffectTimerRenderer.EffectScaleScope scope =
			StatusEffectTimerRenderer.pushAnchoredScale(gui.pose(), gui.guiWidth(), scale);
		try {
			original.call(gui, delta);
		} finally {
			scope.close();
		}
	}

	@Inject(method = "extractEffects", at = @At("TAIL"))
	private void tweakify$renderEffectTimers(GuiGraphicsExtractor gui, DeltaTracker delta, CallbackInfo ci) {
		StatusEffectTimerRenderer.render(gui, this.minecraft);
	}
}
