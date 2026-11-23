package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;
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

	// 当前是否应该阻止放置：兼容旧调用，默认阈值 1（只阻止最后一根）。
	static boolean shouldBlockPlacement(Item held, int totalCount) {
		return shouldBlockPlacement(held, totalCount, TweakifyModConfig.DEFAULT_KEEP_LAST_TORCH_COUNT);
	}

	// 当前是否应该阻止放置：手里是火把且剩余数量不超过阈值（阈值夹紧到 1 ~ 8）。
	public static boolean shouldBlockPlacement(Item held, int totalCount, int minCount) {
		return shouldBlockPlacement(held, totalCount, minCount, false);
	}

	// 副手还有同类火把时视为有备用，允许放置主手最后一根。
	public static boolean shouldBlockPlacement(Item held, int totalCount, int minCount, boolean offhandSpare) {
		if (offhandSpare) {
			return false;
		}
		int threshold = Math.max(TweakifyModConfig.MIN_KEEP_LAST_TORCH_COUNT,
			Math.min(TweakifyModConfig.MAX_KEEP_LAST_TORCH_COUNT, minCount));
		return isTorch(held) && totalCount <= threshold;
	}

	// 统计背包（主背包 + 快捷栏 + 副手）里某种火把的总数量。
	public static int countTorches(Player player, Item torch) {
		return countTorches(player, torch, true);
	}

	// 可忽略副手的版本：副手还有火把时允许放置主手最后一根。
	public static int countTorches(Player player, Item torch, boolean includeOffhand) {
		Inventory inventory = player.getInventory();
		int total = 0;
		for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.getItem() == torch) {
				total += stack.getCount();
			}
		}
		ItemStack offhand = player.getOffhandItem();
		if (includeOffhand && offhand.getItem() == torch) {
			total += offhand.getCount();
		}
		return total;
	}
}
