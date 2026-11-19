package com.gtzc.tweakify.client.mixin;

import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Comparator;

/**
 * 暴露列表条目的内部排序方法。
 *
 * <p>收藏置顶必须直接排序内部 children 后重新定位，不能走 replaceEntries：
 * {@code WorldSelectionList.clearEntries} 会 close 每个条目（销毁存档图标纹理），
 * 重新添加同一条目也不会重建图标，导致存档图标全部消失。
 *
 * <p>{@code children()} 返回的是不可变视图，也不能在外部直接修改。
 */
@Mixin(AbstractSelectionList.class)
public interface AbstractSelectionListInvoker {
	@Invoker("sort")
	void tweakify$sort(Comparator<?> comparator);
}
