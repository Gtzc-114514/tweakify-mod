package com.gtzc.tweakify.client.state;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DeathProtection;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 背包图腾查找：真实 ItemStack 的 DEATH_PROTECTION 组件判定、
 * 槽位顺序与 0–35 范围（快捷栏 + 主背包）。
 */
class TotemInventoryTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		// 26.3 的 ItemStack 构造要求物品 holder 的组件已经绑定。
		BuiltInRegistries.ITEM.freeze();
	}

	@Test
	void findsRealTotemOnlyWithinMainInventory() {
		List<ItemStack> items = new ArrayList<>(
			Collections.nCopies(Inventory.INVENTORY_SIZE, ItemStack.EMPTY));
		items.set(20, stack(Items.TOTEM_OF_UNDYING));

		assertEquals(20, TotemInventory.findTotemSlot(items, Inventory.INVENTORY_SIZE));
		assertEquals(-1, TotemInventory.findTotemSlot(items, 20),
			"限制之外（盔甲/副手）的图腾不参与背包查找");
	}

	@Test
	void findsTotemInHotbarFirst() {
		List<ItemStack> items = List.of(
			ItemStack.EMPTY,
			stack(Items.TOTEM_OF_UNDYING),
			stack(Items.TOTEM_OF_UNDYING));

		assertEquals(1, TotemInventory.findTotemSlot(items, Inventory.INVENTORY_SIZE));
	}

	@Test
	void inventoryFirstPrefersMainInventory() {
		List<ItemStack> items = new ArrayList<>(
			Collections.nCopies(Inventory.INVENTORY_SIZE, ItemStack.EMPTY));
		items.set(2, stack(Items.TOTEM_OF_UNDYING));
		items.set(20, stack(Items.TOTEM_OF_UNDYING));

		assertEquals(20, TotemInventory.findTotemSlot(items, Inventory.INVENTORY_SIZE, true, false));
	}

	@Test
	void canExcludeHotbarFromSearch() {
		List<ItemStack> items = new ArrayList<>(
			Collections.nCopies(Inventory.INVENTORY_SIZE, ItemStack.EMPTY));
		items.set(2, stack(Items.TOTEM_OF_UNDYING));
		items.set(20, stack(Items.TOTEM_OF_UNDYING));

		assertEquals(20, TotemInventory.findTotemSlot(items, Inventory.INVENTORY_SIZE, false, true));
		assertEquals(20, TotemInventory.findTotemSlot(items, Inventory.INVENTORY_SIZE, false, false));
	}

	@Test
	void inventoryFirstFallsBackToHotbar() {
		List<ItemStack> items = new ArrayList<>(
			Collections.nCopies(Inventory.INVENTORY_SIZE, ItemStack.EMPTY));
		items.set(2, stack(Items.TOTEM_OF_UNDYING));

		assertEquals(2, TotemInventory.findTotemSlot(items, Inventory.INVENTORY_SIZE, true, false));
	}

	@Test
	void ignoresPlainItemsAndEmptyStacks() {
		List<ItemStack> items = List.of(
			ItemStack.EMPTY,
			stack(Items.STONE),
			stack(Items.TOTEM_OF_UNDYING));

		assertEquals(2, TotemInventory.findTotemSlot(items, Inventory.INVENTORY_SIZE));
	}

	@Test
	void acceptsStackedTotems() {
		ItemStack stacked = stack(Items.TOTEM_OF_UNDYING, 3);

		assertEquals(1, TotemInventory.findTotemSlot(List.of(ItemStack.EMPTY, stacked),
			Inventory.INVENTORY_SIZE));
	}

	@Test
	void findSlotHandlesEmptyAndLimitBoundaries() {
		ItemStack[] slots = {ItemStack.EMPTY, stack(Items.TOTEM_OF_UNDYING)};

		assertEquals(-1, TotemInventory.findSlot(0, i -> slots[i], 36), "空容器");
		assertEquals(-1, TotemInventory.findSlot(2, i -> slots[i], 0), "limit 为 0");
		assertEquals(-1, TotemInventory.findSlot(2, i -> slots[i], -1), "limit 为负数");
		assertEquals(1, TotemInventory.findSlot(2, i -> slots[i], 36), "limit 超过尺寸");
	}

	@Test
	void findSlotIgnoresNullSlots() {
		ItemStack[] slots = {null, stack(Items.TOTEM_OF_UNDYING)};

		assertEquals(1, TotemInventory.findSlot(2, i -> slots[i], Inventory.INVENTORY_SIZE));
	}

	// 单元测试没有完整数据包加载流程：先绑定空组件表让 ItemStack 可构造，
	// 再给图腾手动设置 DEATH_PROTECTION（空效果列表），验证生产路径的组件判定。
	private static ItemStack stack(Item item) {
		return stack(item, 1);
	}

	private static ItemStack stack(Item item, int count) {
		BuiltInRegistries.ITEM.getResourceKey(item)
			.flatMap(BuiltInRegistries.ITEM::get)
			.ifPresent(holder -> holder.bindComponents(DataComponentMap.EMPTY));
		ItemStack stack = new ItemStack(item, count);
		if (item == Items.TOTEM_OF_UNDYING) {
			stack.set(DataComponents.DEATH_PROTECTION, new DeathProtection(List.of()));
		}
		return stack;
	}
}
