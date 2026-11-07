package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 覆盖按住键会话的三种边界：按下瞬间取值、按住期间保持、松开复位，
 * 以及会话内由外部事件置位（挖掉方块 / 使用过物品）。
 */
class KeyHoldSessionTest {

	@Test
	void pressOverridesAndReleaseClears() {
		assertFalse(KeyHoldSession.next(true, false, false, true), "press edge overrides with supplied value");
		assertTrue(KeyHoldSession.next(true, false, true, false), "press edge samples the supplied value");
		assertFalse(KeyHoldSession.next(false, true, true, true), "release clears the session");
	}

	@Test
	void keepsWhileHeld() {
		assertTrue(KeyHoldSession.next(true, true, false, true), "held keeps previous value");
		assertFalse(KeyHoldSession.next(true, true, true, false));
	}

	@Test
	void markDuringHoldSurvivesUntilRelease() {
		KeyHoldSession session = new KeyHoldSession();
		assertFalse(session.update(true, false), "press starts a clean session");
		session.mark();
		assertTrue(session.isActive(), "external event marks the session");
		assertTrue(session.update(true, false), "held keeps the mark");
		assertFalse(session.update(false, true), "release clears the mark");
		session.mark();
		session.reset();
		assertFalse(session.isActive(), "reset clears everything");
	}
}
