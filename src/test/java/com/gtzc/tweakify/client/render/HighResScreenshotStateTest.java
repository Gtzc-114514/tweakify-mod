package com.gtzc.tweakify.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * F9 4K 状态机：抓取必须等待目标就绪与两个完成帧，超时/取消会作废旧截图回调。
 */
class HighResScreenshotStateTest {
	@Test
	void waitsForTwoRenderedFramesAndReadyTarget() {
		HighResScreenshotState state = new HighResScreenshotState();
		state.beginRequest(10);

		state.onFrameRendered();
		assertEquals(HighResScreenshotState.TickResult.WAIT, state.tick(true),
			"one rendered frame is not enough");

		state.onFrameRendered();
		assertEquals(HighResScreenshotState.TickResult.READY, state.tick(true));
		int attempt = state.beginCapture();
		assertTrue(attempt > 0);
		assertTrue(state.onCaptureCallback(attempt));
		assertTrue(state.isRestorePending());
		assertEquals(HighResScreenshotState.TickResult.RESTORE, state.tick(true));
	}

	@Test
	void timeoutBeforeCaptureLeavesRecoverableState() {
		HighResScreenshotState state = new HighResScreenshotState();
		state.beginRequest(1);

		assertEquals(HighResScreenshotState.TickResult.TIMEOUT, state.tick(false));
		assertTrue(state.isActive(), "timeout must be followed by a restore");
		state.beginRestore();
		state.completeRestore();
		assertFalse(state.isActive());
	}

	@Test
	void timeoutAfterCaptureRejectsLateCallback() {
		HighResScreenshotState state = new HighResScreenshotState();
		state.beginRequest(2);
		state.onFrameRendered();
		state.onFrameRendered();
		assertEquals(HighResScreenshotState.TickResult.READY, state.tick(true));

		int attempt = state.beginCapture();
		assertTrue(attempt > 0);
		assertEquals(HighResScreenshotState.TickResult.TIMEOUT, state.tick(true));

		state.beginRestore();
		assertFalse(state.onCaptureCallback(attempt),
			"a callback from a timed-out capture must be ignored");
		state.completeRestore();
		assertFalse(state.isActive());
	}

	@Test
	void cancelInvalidatesLateCallback() {
		HighResScreenshotState state = new HighResScreenshotState();
		state.beginRequest(10);
		state.onFrameRendered();
		state.onFrameRendered();
		assertEquals(HighResScreenshotState.TickResult.READY, state.tick(true));
		int attempt = state.beginCapture();

		state.beginRestore();
		state.completeRestore();

		assertFalse(state.onCaptureCallback(attempt),
			"a callback arriving after cancel must not save or restore again");
		assertFalse(state.isActive());
	}

	@Test
	void callbackIsAcceptedOnlyOnce() {
		HighResScreenshotState state = new HighResScreenshotState();
		state.beginRequest(10);
		state.onFrameRendered();
		state.onFrameRendered();
		assertEquals(HighResScreenshotState.TickResult.READY, state.tick(true));
		int attempt = state.beginCapture();

		assertTrue(state.onCaptureCallback(attempt));
		assertFalse(state.onCaptureCallback(attempt));
	}

	@Test
	void restoreIsIdempotentAndCanStartNextCapture() {
		HighResScreenshotState state = new HighResScreenshotState();
		state.beginRequest(10);
		state.onFrameRendered();
		state.onFrameRendered();
		assertEquals(HighResScreenshotState.TickResult.READY, state.tick(true));
		int attempt = state.beginCapture();
		assertTrue(state.onCaptureCallback(attempt));

		state.beginRestore();
		state.beginRestore();
		state.completeRestore();
		state.completeRestore();
		assertFalse(state.isActive());

		state.beginRequest(2);
		assertTrue(state.isPending());
	}

	@Test
	void captureCannotStartWithoutPendingRequestOrTwice() {
		HighResScreenshotState state = new HighResScreenshotState();
		assertEquals(-1, state.beginCapture());
		state.beginRequest(10);
		assertEquals(-1, state.beginCapture(), "capture requires two rendered frames");

		state.onFrameRendered();
		state.onFrameRendered();
		assertTrue(state.beginCapture() > 0);
		assertEquals(-1, state.beginCapture(), "capture must not start twice");
	}

	@Test
	void rejectsNonPositiveTimeout() {
		assertThrows(IllegalArgumentException.class,
			() -> new HighResScreenshotState().beginRequest(0));
	}
}
