package com.gtzc.tweakify.client.mixin;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 物品分组的滚动位置换算：展开/收起会改变行数，需要把“当前顶行”换回滚动比例，
 * 这样点击组图标后视野不会跳走。{@code getScrollForRowIndex} 是 protected，
 * 只能通过访问器调用。
 */
@Mixin(CreativeModeInventoryScreen.ItemPickerMenu.class)
public interface CreativeItemPickerMenuAccessor {
	@Invoker("getScrollForRowIndex")
	float tweakify$getScrollForRowIndex(int rowIndex);
}
