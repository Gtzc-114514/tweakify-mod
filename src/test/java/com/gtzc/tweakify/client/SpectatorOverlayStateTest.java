package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MC-217716：只有开启且处于旁观者模式时才隐藏覆盖层。
 */
class SpectatorOverlayStateTest {

	@Test
	void hidesOnlyWhenEnabledAndSpectator() {
		assertTrue(SpectatorOverlayState.shouldHideOverlays(true, true));
	}

	@Test
	void keepsOverlaysWhenDisabled() {
		assertFalse(SpectatorOverlayState.shouldHideOverlays(false, true));
		assertFalse(SpectatorOverlayState.shouldHideOverlays(false, false));
	}

	@Test
	void keepsOverlaysForNonSpectators() {
		assertFalse(SpectatorOverlayState.shouldHideOverlays(true, false));
	}

	@Test
	void intensityIsZeroedOnlyForSpectatorsWithFixEnabled() {
		assertEquals(0.0F, SpectatorOverlayState.intensity(true, true, 0.8F));
		assertEquals(0.8F, SpectatorOverlayState.intensity(false, true, 0.8F), 1.0e-6F);
		assertEquals(0.8F, SpectatorOverlayState.intensity(true, false, 0.8F), 1.0e-6F);
	}
}
