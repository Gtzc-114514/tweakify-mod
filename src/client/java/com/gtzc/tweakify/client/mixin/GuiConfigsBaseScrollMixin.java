package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.ScrollMemory;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 滚动列表记忆（MaLiLib 配置界面）：
 * 关闭界面时记下滚动条位置，下次打开同一模组的配置界面时恢复。
 *
 * <p>必须挂在 GuiConfigsBase 上：它重写了 removed() 且不调用 super.removed()，
 * 挂在父类 GuiListBase 上的保存钩子对配置界面永远不会执行。
 *
 * <p>list 控件通过 {@link GuiListBaseAccessor} 取：@Shadow 不会到父类查找方法。
 *
 * <p>只在每个界面实例第一次初始化时恢复：切标签、展开分组都会再次调用 initGui，
 * 这些操作有自己的滚动逻辑，不能被恢复覆盖。
 */
@Mixin(GuiConfigsBase.class)
public abstract class GuiConfigsBaseScrollMixin {
	@Unique
	private boolean tweakify$scrollRestored;

	@Inject(method = "initGui", at = @At("TAIL"))
	private void tweakify$restoreScroll(CallbackInfo ci) {
		if (this.tweakify$scrollRestored) {
			return;
		}
		this.tweakify$scrollRestored = true;
		if (!ScrollMemory.isMaLiLibEnabled()) {
			return;
		}
		Integer saved = ScrollMemory.getMaLiLib(this);
		if (saved == null) {
			return;
		}
		WidgetListBase<?, ?> list = ((GuiListBaseAccessor) this).tweakify$getListWidget();
		if (list != null) {
			// 此时条目已重建、最大值已更新，越界值会被夹回合法范围。
			list.getScrollbar().setValue(saved);
			// 条目控件是按刷新时的滚动位置摆放的，改完滚动值必须再刷一次列表才会真正滚过去。
			list.refreshEntries();
		}
	}

	@Inject(method = "removed", at = @At("HEAD"))
	private void tweakify$saveScroll(CallbackInfo ci) {
		if (!ScrollMemory.isMaLiLibEnabled()) {
			return;
		}
		WidgetListBase<?, ?> list = ((GuiListBaseAccessor) this).tweakify$getListWidget();
		if (list != null) {
			ScrollMemory.putMaLiLib(this, list.getScrollbar().getValue());
			// 界面关闭时立即刷盘：跨重启记忆不必等定时器。
			ScrollMemory.flush();
		}
	}
}
