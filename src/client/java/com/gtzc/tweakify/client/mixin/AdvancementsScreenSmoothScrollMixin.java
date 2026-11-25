package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.SmoothScrollAnimator;
import com.gtzc.tweakify.client.SmoothScrolling;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 原版进度（成就）界面的平滑滚动。
 *
 * <p>滚轮入口只累加目标位置，逐帧在渲染前把缓动后的值写回标签订单的
 * scrollX/scrollY；拖动、打开界面时的自动居中与外部改值会立即对齐。
 */
@Mixin(AdvancementsScreen.class)
public abstract class AdvancementsScreenSmoothScrollMixin {
	// 原版滚轮一格 16 像素，与 AdvancementsScreen.SCROLL_SPEED 一致。
	@Unique
	private static final double TWEAKIFY$SCROLL_SPEED = 16.0;
	// 单帧最大时间步长：卡顿后一次性追上目标会显得突兀。
	@Unique
	private static final double TWEAKIFY$MAX_FRAME_SECONDS = 0.1;

	@Shadow
	private AdvancementTab selectedTab;

	// 位置使用 -scroll 的坐标系（0 = 内容左上，越大越往右下），与动画器的 [0, max] 对齐。
	@Unique
	private final SmoothScrollAnimator tweakify$scrollX = new SmoothScrollAnimator();

	@Unique
	private final SmoothScrollAnimator tweakify$scrollY = new SmoothScrollAnimator();

	@Unique
	private AdvancementTab tweakify$lastTab;

	@Unique
	private long tweakify$lastFrameMillis;

	@Unique
	private boolean tweakify$initialized;

	@Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
	private void tweakify$smoothWheel(double mouseX, double mouseY, double scrollX, double scrollY,
									  CallbackInfoReturnable<Boolean> cir) {
		if (!SmoothScrolling.advancementsEnabled() || this.selectedTab == null) {
			return;
		}
		if (scrollX == 0.0 && scrollY == 0.0) {
			return;
		}
		this.tweakify$syncAnimator();
		this.tweakify$scrollX.addDelta(-scrollX * TWEAKIFY$SCROLL_SPEED, this.tweakify$maxX());
		this.tweakify$scrollY.addDelta(-scrollY * TWEAKIFY$SCROLL_SPEED, this.tweakify$maxY());
		cir.setReturnValue(true);
	}

	@Inject(method = "extractRenderState", at = @At("HEAD"))
	private void tweakify$applySmoothScroll(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
											float partialTick, CallbackInfo ci) {
		AdvancementTab tab = this.selectedTab;
		if (!SmoothScrolling.advancementsEnabled() || tab == null) {
			this.tweakify$initialized = false;
			this.tweakify$lastTab = null;
			return;
		}
		if (!this.tweakify$initialized || tab != this.tweakify$lastTab) {
			// 首帧交给原版计算居中位置；下一帧再把动画器对齐到实际值。
			this.tweakify$syncAnimator();
			this.tweakify$lastFrameMillis = Util.getMillis();
			this.tweakify$initialized = true;
			this.tweakify$lastTab = tab;
			return;
		}

		long now = Util.getMillis();
		double deltaSeconds = this.tweakify$lastFrameMillis == 0L
			? 0.0
			: Math.min(TWEAKIFY$MAX_FRAME_SECONDS, (now - this.tweakify$lastFrameMillis) / 1000.0);
		this.tweakify$lastFrameMillis = now;

		this.tweakify$syncAnimator();
		AdvancementTabAccessor accessor = (AdvancementTabAccessor) tab;
		this.tweakify$scrollX.setTimeConstant(SmoothScrolling.timeConstantSeconds());
		this.tweakify$scrollY.setTimeConstant(SmoothScrolling.timeConstantSeconds());
		// 不可滚动的轴必须保留原版居中位置（scrollX/scrollY 可能非零）：
		// 动画器只能夹在 [0, max]，无条件写回会把小树内容推到窗口左上角。
		if (tab.canScrollHorizontally()) {
			this.tweakify$scrollX.update(deltaSeconds, this.tweakify$maxX());
			accessor.tweakify$setScrollX(-this.tweakify$scrollX.position());
		}
		if (tab.canScrollVertically()) {
			this.tweakify$scrollY.update(deltaSeconds, this.tweakify$maxY());
			accessor.tweakify$setScrollY(-this.tweakify$scrollY.position());
		}
	}

	// 拖动、居中或其它模组直接改 scroll 时立即对齐，不做追赶动画。
	@Unique
	private void tweakify$syncAnimator() {
		if (this.selectedTab == null) {
			return;
		}
		AdvancementTabAccessor accessor = (AdvancementTabAccessor) this.selectedTab;
		double scrollX = accessor.tweakify$getScrollX();
		double scrollY = accessor.tweakify$getScrollY();
		if (this.selectedTab != this.tweakify$lastTab
			|| Math.abs(-scrollX - this.tweakify$scrollX.position()) > 1.0e-3
			|| Math.abs(-scrollY - this.tweakify$scrollY.position()) > 1.0e-3) {
			this.tweakify$scrollX.jumpTo(-scrollX, this.tweakify$maxX());
			this.tweakify$scrollY.jumpTo(-scrollY, this.tweakify$maxY());
		}
	}

	@Unique
	private double tweakify$maxX() {
		if (this.selectedTab == null || !this.selectedTab.canScrollHorizontally()) {
			return 0.0;
		}
		// 原版 scrollX 的下界是 -(maxX - 234)。
		return Math.max(0, ((AdvancementTabAccessor) this.selectedTab).tweakify$getMaxX() - 234);
	}

	@Unique
	private double tweakify$maxY() {
		if (this.selectedTab == null || !this.selectedTab.canScrollVertically()) {
			return 0.0;
		}
		// 原版 scrollY 的下界是 -(maxY - 113)。
		return Math.max(0, ((AdvancementTabAccessor) this.selectedTab).tweakify$getMaxY() - 113);
	}
}
