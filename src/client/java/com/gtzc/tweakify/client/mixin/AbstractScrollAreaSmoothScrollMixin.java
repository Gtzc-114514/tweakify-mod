package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.SmoothScrolling;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mod Menu 列表的滚轮平滑化（滚轮入口）。
 *
 * <p>Mod Menu 是可选依赖，不能直接引用它的列表类：这里按类名识别 Mod Menu 的
 * ModListWidget，真正的逐帧推进放在 {@code mixin.modmenu.ModListWidgetSmoothScrollMixin}。
 * 其它滚动区（视频设置、语言列表等）保持原版行为。
 */
@Mixin(AbstractScrollArea.class)
public abstract class AbstractScrollAreaSmoothScrollMixin {
	@Shadow
	protected abstract double scrollRate();

	@Shadow
	public abstract int maxScrollAmount();

	@Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
	private void tweakify$smoothModMenuScroll(double mouseX, double mouseY, double horizontalAmount,
											  double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
		// 与原版一致：不可见时不消费滚轮。
		if (!((AbstractWidget) (Object) this).visible) {
			return;
		}
		if (!SmoothScrolling.modMenuEnabled() || !SmoothScrolling.isModMenuList(this)) {
			return;
		}
		// 逐帧推进没有在运行（Mod Menu 的 mixin 未应用）：不接管，退回原版一次性滚动。
		if (!SmoothScrolling.modMenuFrameAlive()) {
			return;
		}
		// 与原版一致：一格滚轮位移 scrollRate 像素，只是先累加、不立即写回。
		SmoothScrolling.modMenuScrollBy(-verticalAmount * this.scrollRate(), this.maxScrollAmount());
		cir.setReturnValue(true);
	}
}
