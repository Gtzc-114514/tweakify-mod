package com.gtzc.tweakify.client.mixin;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 读取创造模式物品栏的槽位包装器指向的真实槽位。
 *
 * <p>创造物品栏的玩家背包槽是 {@code SlotWrapper}，它的 {@code index} 是创造菜单下标，
 * 不能直接发给服务端；真实槽位的 {@code index} 才是 {@code InventoryMenu} 下标。
 */
@Mixin(targets = "net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen$SlotWrapper")
public interface CreativeModeInventoryScreenSlotWrapperAccessor {
	@Accessor("target")
	Slot tweakify$target();
}
