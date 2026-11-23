package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MC-143474：开启时恢复旧快捷栏槽位；关闭时不修改新玩家。
 */
class RespawnHotbarStateTest {

	@Test
	void disabledReturnsNoChange() {
		assertEquals(-1, RespawnHotbarState.slotToRestore(false, 4));
	}

	@Test
	void enabledRestoresSelectedSlot() {
		assertEquals(0, RespawnHotbarState.slotToRestore(true, 0));
		assertEquals(4, RespawnHotbarState.slotToRestore(true, 4));
		assertEquals(8, RespawnHotbarState.slotToRestore(true, 8));
	}

	@Test
	void dimensionChangeCanBeExcluded() {
		assertTrue(RespawnHotbarState.shouldRestore(true, true, true));
		assertTrue(RespawnHotbarState.shouldRestore(true, true, false));
		assertFalse(RespawnHotbarState.shouldRestore(true, false, true));
		assertTrue(RespawnHotbarState.shouldRestore(true, false, false));
		assertFalse(RespawnHotbarState.shouldRestore(false, true, false));
	}
}
