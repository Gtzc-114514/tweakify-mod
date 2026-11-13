package com.gtzc.tweakify.client;

import net.minecraft.world.inventory.Slot;

/**
 * 创造模式物品分组对槽位绘制的只读查询接口：由
 * {@code CreativeModeInventoryGroupingMixin} 实现，供槽位背景绘制 Mixin 使用。
 */
public interface CreativeItemGroupAccess {
	boolean tweakify$isExpandedGroupMember(Slot slot);
}
