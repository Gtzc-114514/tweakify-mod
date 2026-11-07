package com.gtzc.tweakify.client.mixin;

import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 暴露 MaLiLib 列表界面的 getListWidget()。
 *
 * <p>@Shadow 不会到父类查找方法，而 getListWidget() 声明在 GuiListBase，
 * 因此从 GuiConfigsBase 的注入里改走这个访问器。
 */
@Mixin(GuiListBase.class)
public interface GuiListBaseAccessor {
	@Invoker("getListWidget")
	WidgetListBase<?, ?> tweakify$getListWidget();
}
