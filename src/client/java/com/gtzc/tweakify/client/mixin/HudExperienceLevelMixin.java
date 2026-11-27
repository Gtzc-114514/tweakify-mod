package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.LocatorCompass;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 指南针开启时把原版经验等级数字整体上移：它的描边底边原本会压住居中的方位字母。
 *
 * <p>用 pose 平移包住原版调用，颜色与四向描边仍由原版自己画；指南针关闭时原版绘制零变化。
 * 指南针本身不再走 mixin，而是用 Fabric 的 {@code HudElementRegistry.addLast} 注册
 * （见 {@code TweakifyModClient}），保证排在原版与其它模组的全部 HUD 元素之后。
 */
@Mixin(Hud.class)
public class HudExperienceLevelMixin {
	@WrapOperation(method = "extractHotbarAndDecorations",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/contextualbar/ContextualBar;extractExperienceLevel"
				+ "(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;I)V"))
	private void tweakify$liftExperienceLevel(GuiGraphicsExtractor graphics, Font font, int level,
											  Operation<Void> original) {
		TweakifyModConfig config = TweakifyMod.config();
		if (config == null || !config.functions.locatorBarCompass) {
			original.call(graphics, font, level);
			return;
		}
		graphics.pose().pushMatrix();
		try {
			graphics.pose().translate(0.0F, -LocatorCompass.LEVEL_TEXT_SHIFT);
			original.call(graphics, font, level);
		} finally {
			// 无论原版绘制是否抛异常都要弹回，否则后续整个 HUD 都会偏移。
			graphics.pose().popMatrix();
		}
	}
}
