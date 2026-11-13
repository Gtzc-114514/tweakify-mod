package com.gtzc.tweakify.client;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PiglinArmorSwapStateTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@BeforeEach
	void resetState() {
		PiglinArmorSwapState.resetForTests();
	}

	@Test
	void armorSlotsFollowInventoryMenuOrder() {
		assertEquals(InventoryMenu.ARMOR_SLOT_START, PiglinArmorSwapState.armorMenuSlot(EquipmentSlot.HEAD));
		assertEquals(InventoryMenu.ARMOR_SLOT_START + 1, PiglinArmorSwapState.armorMenuSlot(EquipmentSlot.CHEST));
		assertEquals(InventoryMenu.ARMOR_SLOT_START + 2, PiglinArmorSwapState.armorMenuSlot(EquipmentSlot.LEGS));
		assertEquals(InventoryMenu.ARMOR_SLOT_START + 3, PiglinArmorSwapState.armorMenuSlot(EquipmentSlot.FEET));
		assertEquals(-1, PiglinArmorSwapState.armorMenuSlot(EquipmentSlot.MAINHAND));
	}

	@Test
	void hotbarSlotsAreMappedToTheUseRow() {
		assertEquals(InventoryMenu.USE_ROW_SLOT_START, PiglinArmorSwapState.inventoryMenuSlot(0));
		assertEquals(InventoryMenu.USE_ROW_SLOT_START + 8, PiglinArmorSwapState.inventoryMenuSlot(8));
		assertEquals(InventoryMenu.USE_ROW_SLOT_END - 1, PiglinArmorSwapState.inventoryMenuSlot(8));
	}

	@Test
	void mainInventorySlotsKeepTheirMenuIndex() {
		assertEquals(InventoryMenu.INV_SLOT_START, PiglinArmorSwapState.inventoryMenuSlot(9));
		assertEquals(35, PiglinArmorSwapState.inventoryMenuSlot(35));
		assertEquals(InventoryMenu.INV_SLOT_END - 1, PiglinArmorSwapState.inventoryMenuSlot(35));
	}

	@Test
	void goldItemsMatchTheirArmorSlot() {
		assertEquals(Items.GOLDEN_HELMET, PiglinArmorSwapState.goldItemFor(EquipmentSlot.HEAD));
		assertEquals(Items.GOLDEN_CHESTPLATE, PiglinArmorSwapState.goldItemFor(EquipmentSlot.CHEST));
		assertEquals(Items.GOLDEN_LEGGINGS, PiglinArmorSwapState.goldItemFor(EquipmentSlot.LEGS));
		assertEquals(Items.GOLDEN_BOOTS, PiglinArmorSwapState.goldItemFor(EquipmentSlot.FEET));
		assertEquals(Items.AIR, PiglinArmorSwapState.goldItemFor(EquipmentSlot.MAINHAND));
	}

	@Test
	void armorSlotsAreProcessedTopDown() {
		assertEquals(EquipmentSlot.HEAD, PiglinArmorSwapState.ARMOR_SLOTS.get(0));
		assertEquals(EquipmentSlot.CHEST, PiglinArmorSwapState.ARMOR_SLOTS.get(1));
		assertEquals(EquipmentSlot.LEGS, PiglinArmorSwapState.ARMOR_SLOTS.get(2));
		assertEquals(EquipmentSlot.FEET, PiglinArmorSwapState.ARMOR_SLOTS.get(3));
	}
}
