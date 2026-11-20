package com.gtzc.tweakify.client;

import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 背包图腾查找的槽位遍历：按顺序返回第一个命中槽位，遵守范围限制。
 *
 * <p>单测环境无法构造非空 ItemStack（物品组件未绑定），这里用谓词重载验证
 * 遍历与范围逻辑；真实物品的组件判定由 {@link TotemInventory} 的生产路径使用。
 */
class TotemInventoryTest {
	@Test
	void findsFirstMatchingSlotInOrder() {
		List<ItemStack> items = Arrays.asList(ItemStack.EMPTY, null, ItemStack.EMPTY);
		Predicate<ItemStack> isTotem = Objects::isNull;

		assertEquals(1, TotemInventory.findTotemSlot(items, TotemInventory.MAIN_INVENTORY_SIZE, isTotem));
	}

	@Test
	void respectsInventoryLimit() {
		List<ItemStack> items = Arrays.asList(ItemStack.EMPTY, null, ItemStack.EMPTY);
		Predicate<ItemStack> isTotem = Objects::isNull;

		assertEquals(-1, TotemInventory.findTotemSlot(items, 1, isTotem),
			"超出限制的槽位（如盔甲/副手）不参与背包查找");
	}

	@Test
	void returnsMinusOneWhenNothingMatches() {
		List<ItemStack> items = List.of(ItemStack.EMPTY, ItemStack.EMPTY);

		assertEquals(-1, TotemInventory.findTotemSlot(items, 36, stack -> false));
		assertEquals(-1, TotemInventory.findTotemSlot(Collections.emptyList(), 36, stack -> true));
	}
}
