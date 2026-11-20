package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.StatusEffectTimerRenderer;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

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
	@WrapMethod(method = "extractEffects")
	private void tweakify$scaledEffectDisplay(GuiGraphicsExtractor gui, DeltaTracker delta, Operation<Void> original) {
		float scale = StatusEffectTimerRenderer.effectScale(TweakifyMod.CONFIG);
		boolean scaled = scale != 1.0f;
		if (scaled) {
			int right = this.minecraft.getWindow().getGuiScaledWidth();
			gui.pose().pushMatrix();
			gui.pose().translate(right, 0.0f);
			gui.pose().scale(scale, scale);
			gui.pose().translate(-right, 0.0f);
		}
		try {
			original.call(gui, delta);
			StatusEffectTimerRenderer.render(gui, this.minecraft);
		} finally {
			if (scaled) {
				gui.pose().popMatrix();
			}
		}
	}
}
