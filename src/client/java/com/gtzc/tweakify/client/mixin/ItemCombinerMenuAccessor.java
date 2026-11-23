package com.gtzc.tweakify.client.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ItemCombinerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 铁砧/锻造台共用菜单的玩家访问器：用于按游戏模式或潜行状态收窄修复范围。
 */
@Mixin(ItemCombinerMenu.class)
public interface ItemCombinerMenuAccessor {
	@Accessor("player")
	Player tweakify$getPlayer();
}
