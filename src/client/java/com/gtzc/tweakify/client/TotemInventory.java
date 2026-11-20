package com.gtzc.tweakify.client;

import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DeathProtection;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.List;

/**
 * 不死图腾的背包查找与触发：原版只检查主手/副手（checkTotemDeathProtection），
 * 这里在双手都没有图腾时从快捷栏与主背包补一个；消耗、统计、进度、音效与
 * 死亡保护效果完全沿用原版实现。
 */
public final class TotemInventory {
	private TotemInventory() {
	}

	// 从背包取图腾并触发不死保护；返回是否触发（触发后由 mixin 取消原方法）。
	public static boolean tryUseInventoryTotem(Player player) {
		Inventory inventory = player.getInventory();
		int slot = findTotemSlot(inventory, Inventory.INVENTORY_SIZE);
		if (slot < 0) {
			return false;
		}
		ItemStack stack = inventory.getItem(slot);
		DeathProtection protection = stack.get(DataComponents.DEATH_PROTECTION);
		if (protection == null) {
			return false;
		}
		ItemStack used = stack.copy();
		stack.shrink(1);
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.awardStat(Stats.ITEM_USED.get(used.getItem()));
			CriteriaTriggers.USED_TOTEM.trigger(serverPlayer, used);
			used.causeUseVibration(player, GameEvent.ITEM_INTERACT_FINISH);
		}
		player.setHealth(1.0f);
		protection.applyEffects(used, player);
		player.level().broadcastEntityEvent(player, (byte) 35);
		return true;
	}

	static int findTotemSlot(Inventory inventory, int limit) {
		int end = Math.min(inventory.getContainerSize(), limit);
		for (int slot = 0; slot < end; slot++) {
			if (hasDeathProtection(inventory.getItem(slot))) {
				return slot;
			}
		}
		return -1;
	}

	// 纯函数：测试直接覆盖槽位遍历与范围限制。
	// 只保留真实谓词（DEATH_PROTECTION 组件判定），避免测试专用重载成为死代码。
	static int findTotemSlot(List<ItemStack> items, int limit) {
		int end = Math.min(items.size(), limit);
		for (int i = 0; i < end; i++) {
			if (hasDeathProtection(items.get(i))) {
				return i;
			}
		}
		return -1;
	}

	private static boolean hasDeathProtection(ItemStack stack) {
		return stack != null && !stack.isEmpty()
			&& stack.get(DataComponents.DEATH_PROTECTION) != null;
	}
}
