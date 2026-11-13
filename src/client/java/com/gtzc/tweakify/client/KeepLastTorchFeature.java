package com.gtzc.tweakify.client;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 不使用最后一根火把：放置火把时，如果背包里只剩最后一根，就不执行放置，
 * 避免把最后的火把用掉。仅生存/冒险模式生效（创造模式火把不消耗）。
 */
public final class KeepLastTorchFeature {
	private KeepLastTorchFeature() {
	}

	// 所有火把类物品（含 26.3 新增的铜火把）。
	static boolean isTorch(Item item) {
		return item == Items.TORCH || item == Items.SOUL_TORCH
			|| item == Items.COPPER_TORCH || item == Items.REDSTONE_TORCH;
	}

	// 当前是否应该阻止放置：手里是火把且背包里总共不超过一根。
	public static boolean shouldBlockPlacement(Item held, int totalCount) {
		return isTorch(held) && totalCount <= 1;
	}

	// 统计背包（主背包 + 快捷栏 + 副手）里某种火把的总数量。
	public static int countTorches(Player player, Item torch) {
		Inventory inventory = player.getInventory();
		int total = 0;
		for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.getItem() == torch) {
				total += stack.getCount();
			}
		}
		ItemStack offhand = player.getOffhandItem();
		if (offhand.getItem() == torch) {
			total += offhand.getCount();
		}
		return total;
	}
}
