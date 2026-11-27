package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.ui.SmoothScrollAnimator;
import com.gtzc.tweakify.client.ui.SmoothScrolling;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 创造物品栏平滑滚动。
 *
 * <p>原版一次滚轮直接换一行；这里把滚轮累加到目标位置，每帧再把亚行位置写回
 * {@code scrollOffs} 并只在跨行时调用 {@code menu.scrollTo}。物品栏的槽位本身按整行摆放，
 * 因此表现为“滚动条平滑移动 + 物品行依次过渡”，不会打乱槽位与点击判定。
 *
 * <p>与物品分组兼容：分组展开/收起会通过 {@code picker.scrollTo} 恢复顶部行，并同步
 * {@code scrollOffs}（见 CreativeModeInventoryGroupingMixin），这里检测到外部改值就立即对齐，
 * 不会把刚恢复的位置再动画回去。
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventorySmoothScrollMixin {
	// 单帧最大时间步长：卡顿后一次性追上目标会显得突兀。
	@Unique
	private static final double TWEAKIFY$MAX_FRAME_SECONDS = 0.1;

	// 物品格每格 18 像素：亚行偏移按这个比例换算。
	@Unique
	private static final int TWEAKIFY$SLOT_SIZE = 18;

	@Shadow
	private float scrollOffs;

	@Unique
	private final SmoothScrollAnimator tweakify$animator = new SmoothScrollAnimator();

	@Unique
	private long tweakify$lastFrameMillis;

	// 上一次写回 scrollOffs 的值：用于识别拖动滚动条、切页与分组恢复等外部改动。
	@Unique
	private float tweakify$appliedOffs = -1.0f;

	// 已经应用到菜单的顶部行，避免每帧重复调用 scrollTo。
	@Unique
	private int tweakify$appliedRow = -1;

	// 当前亚行像素偏移：槽位整体上移这么多像素后绘制。
	@Unique
	private int tweakify$shiftPixels;

	// 滚轮：在 super（悬停槽位的收纳袋翻页等）与 canScroll 检查之后切入，
	// 只累加目标行，原版的一次性跳行不再执行。
	@Inject(
		method = "mouseScrolled",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen$ItemPickerMenu;subtractInputFromScroll(FD)F"
		),
		cancellable = true
	)
	private void tweakify$smoothMouseScrolled(double mouseX, double mouseY, double horizontalAmount,
											 double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
		int rows = this.tweakify$rowCount();
		if (!SmoothScrolling.creativeEnabled() || rows <= 0) {
			// 不取消：退回原版一次性滚动。
			return;
		}
		// 动画单位是“行”：一格滚轮 = 一行。
		this.tweakify$animator.addDelta(-verticalAmount, rows);
		cir.setReturnValue(true);
	}

	// 每帧推进：渲染前把亚行位置写回 scrollOffs，跨行时更新菜单槽位。
	@Inject(method = "extractRenderState", at = @At("HEAD"))
	private void tweakify$updateSmoothScroll(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick,
											 CallbackInfo ci) {
		long now = Util.getMillis();
		double deltaSeconds = this.tweakify$lastFrameMillis == 0L
			? 0.0
			: Math.min(TWEAKIFY$MAX_FRAME_SECONDS, (now - this.tweakify$lastFrameMillis) / 1000.0);
		this.tweakify$lastFrameMillis = now;

		// 原版在行数为 0 时会往 scrollOffs 写 NaN（0/0），这里先清洗；
		// 否则 NaN 会进入动画器，且下面的“外部改动”比较对 NaN 恒为 false，导致永久不同步。
		if (!Float.isFinite(this.scrollOffs)) {
			this.scrollOffs = 0.0f;
		}
		int rows = this.tweakify$rowCount();
		if (rows <= 0 || !SmoothScrolling.creativeEnabled()) {
			// 没有可滚动行：不碰动画器（单位未定义），只标记下次需要重新对齐。
			this.tweakify$appliedOffs = -1.0f;
			this.tweakify$appliedRow = -1;
			this.tweakify$shiftPixels = 0;
			SmoothScrolling.updateCreativeGridState(0, 0, 0);
			return;
		}
		// 动画单位是“行”：吸附阈值换算成半像素，否则收尾时会一口气跳掉半行（9 像素）。
		this.tweakify$animator.setSnapDistance(0.5 / TWEAKIFY$SLOT_SIZE);
		// 首次推进时先对齐：避免把已有的滚动位置（含分组恢复后的行）拉回顶部。
		// 外部改过滚动比例（拖动滚动条、切页、分组恢复）同样立即对齐，不与动画互相拉扯。
		boolean external = this.tweakify$appliedOffs < 0.0f
			|| Math.abs(this.scrollOffs - this.tweakify$appliedOffs) > 1.0e-4f;
		if (external) {
			this.tweakify$animator.jumpTo(this.scrollOffs * rows, rows);
		}
		this.tweakify$animator.setTimeConstant(SmoothScrolling.timeConstantSeconds());
		this.tweakify$animator.update(deltaSeconds, rows);

		// 连续位置 → 起始行 + 亚行像素：槽位整体上移，跨行时再重填菜单。
		double position = this.tweakify$animator.position();
		SmoothScrolling.CreativeScrollLayout layout =
			SmoothScrolling.creativeLayout(position, TWEAKIFY$SLOT_SIZE);
		// 拖动右侧滑块时原版按四舍五入重填槽位，与这里的 floor 语义可能差一整行：
		// 槽位实际行（scrollTo 上报）与渲染行不一致时必须重填，否则底部预告行会和第 5 行重复。
		if (SmoothScrolling.creativeMenuRow() != layout.row()
			|| layout.row() != this.tweakify$appliedRow) {
			this.tweakify$appliedRow = layout.row();
			CreativeModeInventoryScreen.ItemPickerMenu picker = this.tweakify$picker();
			if (picker != null) {
				// scrollTo 内部按行取整，传当前行的精确比例即可。
				picker.scrollTo(layout.row() / (float) rows);
			}
		}
		this.tweakify$shiftPixels = layout.shiftPixels();

		// 滚动条滑块按连续位置绘制，比整行跳变更平滑。
		float continuous = (float) (position / rows);
		if (Math.abs(continuous - this.scrollOffs) > 1.0e-5f) {
			this.scrollOffs = continuous;
		}
		this.tweakify$appliedOffs = this.scrollOffs;
		SmoothScrolling.updateCreativeGridState(layout.row(), this.tweakify$shiftPixels, rows);
	}

	// 关闭界面：清空网格状态，避免影响下一次打开。
	@Inject(method = "removed", at = @At("TAIL"))
	private void tweakify$resetCreativeGrid(CallbackInfo ci) {
		this.tweakify$shiftPixels = 0;
		SmoothScrolling.resetCreativeGridState();
	}

	// 可滚动行数：与原版 ItemPickerMenu.calculateRowCount 一致（9 列、可视 5 行）。
	@Unique
	private int tweakify$rowCount() {
		CreativeModeInventoryScreen.ItemPickerMenu picker = this.tweakify$picker();
		return picker == null ? 0 : Math.max(0, Mth.positiveCeilDiv(picker.items.size(), 9) - 5);
	}

	@Unique
	private CreativeModeInventoryScreen.ItemPickerMenu tweakify$picker() {
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
		return screen.getMenu() instanceof CreativeModeInventoryScreen.ItemPickerMenu picker ? picker : null;
	}
}
