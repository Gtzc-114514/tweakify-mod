package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.world.entity.EquipmentSlot;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PiglinArmorSwapModeTest {
	@Test
	void codesMatchSupportedList() {
		assertEquals(TweakifyModConfig.SUPPORTED_PIGLIN_ARMOR_SWAP_MODES,
			Arrays.stream(PiglinArmorSwapMode.values()).map(PiglinArmorSwapMode::code).toList());
	}

	@Test
	void unknownCodeFallsBackToAuto() {
		assertEquals(PiglinArmorSwapMode.AUTO, PiglinArmorSwapMode.fromCode("not-a-mode"));
		assertEquals(PiglinArmorSwapMode.AUTO, PiglinArmorSwapMode.fromCode(null));
	}

	@Test
	void autoAllowsEveryArmorSlot() {
		for (EquipmentSlot slot : PiglinArmorSwapState.ARMOR_SLOTS) {
			assertTrue(PiglinArmorSwapMode.AUTO.allows(slot));
		}
	}

	@Test
	void singleSlotModesOnlyAllowTheirOwnSlot() {
		assertTrue(PiglinArmorSwapMode.HELMET.allows(EquipmentSlot.HEAD));
		assertFalse(PiglinArmorSwapMode.HELMET.allows(EquipmentSlot.CHEST));
		assertTrue(PiglinArmorSwapMode.CHESTPLATE.allows(EquipmentSlot.CHEST));
		assertFalse(PiglinArmorSwapMode.CHESTPLATE.allows(EquipmentSlot.LEGS));
		assertTrue(PiglinArmorSwapMode.LEGGINGS.allows(EquipmentSlot.LEGS));
		assertFalse(PiglinArmorSwapMode.LEGGINGS.allows(EquipmentSlot.FEET));
		assertTrue(PiglinArmorSwapMode.BOOTS.allows(EquipmentSlot.FEET));
		assertFalse(PiglinArmorSwapMode.BOOTS.allows(EquipmentSlot.HEAD));
	}

	@Test
	void nonArmorSlotsAreNeverAllowed() {
		for (PiglinArmorSwapMode mode : PiglinArmorSwapMode.values()) {
			assertFalse(mode.allows(EquipmentSlot.MAINHAND));
			assertFalse(mode.allows(EquipmentSlot.OFFHAND));
		}
	}
}
