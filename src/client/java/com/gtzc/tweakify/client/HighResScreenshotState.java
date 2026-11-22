package com.gtzc.tweakify.client;

/**
 * F9 4K 截图的状态机：只负责 pending、等待渲染帧、抓取代次与超时/恢复转换，
 * 不依赖 Minecraft，便于对超时、取消和迟到回调做单元测试。
 */
final class HighResScreenshotState {
	enum TickResult {
		IDLE,
		WAIT,
		READY,
		TIMEOUT,
		RESTORE
	}

	private boolean pending;
	private boolean captureStarted;
	private boolean restorePending;
	private int renderedFrames;
	private int timeoutTicks;
	private int attempt;

	void beginRequest(int timeoutTicks) {
		if (timeoutTicks <= 0) {
			throw new IllegalArgumentException("timeoutTicks must be positive");
		}
		pending = true;
		captureStarted = false;
		restorePending = false;
		renderedFrames = 0;
		this.timeoutTicks = timeoutTicks;
		attempt++;
	}

	boolean isActive() {
		return pending || restorePending;
	}

	boolean isPending() {
		return pending;
	}

	boolean isCaptureStarted() {
		return captureStarted;
	}

	boolean isRestorePending() {
		return restorePending;
	}

	void onFrameRendered() {
		if (pending && !captureStarted && !restorePending) {
			renderedFrames++;
		}
	}

	TickResult tick(boolean targetReady) {
		if (!pending && !restorePending) {
			return TickResult.IDLE;
		}
		if (restorePending) {
			return TickResult.RESTORE;
		}
		if (--timeoutTicks <= 0) {
			return TickResult.TIMEOUT;
		}
		if (captureStarted) {
			return TickResult.WAIT;
		}
		if (!targetReady || renderedFrames < 2) {
			return TickResult.WAIT;
		}
		return TickResult.READY;
	}

	/**
	 * 标记开始抓取并返回本次抓取代次；不在 pending、渲染帧不足或已经开始抓取时返回 -1。
	 */
	int beginCapture() {
		if (!pending || captureStarted || restorePending || renderedFrames < 2) {
			return -1;
		}
		captureStarted = true;
		return ++attempt;
	}

	/**
	 * 接受截图回调；取消、超时或恢复之后到达的旧代次回调返回 false。
	 */
	boolean onCaptureCallback(int callbackAttempt) {
		if (!pending || !captureStarted || restorePending || callbackAttempt != attempt) {
			return false;
		}
		captureStarted = false;
		restorePending = true;
		return true;
	}

	void beginRestore() {
		if (!isActive()) {
			return;
		}
		attempt++;
		pending = true;
		captureStarted = false;
		restorePending = true;
	}

	void completeRestore() {
		pending = false;
		captureStarted = false;
		restorePending = false;
		renderedFrames = 0;
		timeoutTicks = 0;
	}
}
