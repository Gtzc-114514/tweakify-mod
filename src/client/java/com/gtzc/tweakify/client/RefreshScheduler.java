package com.gtzc.tweakify.client;

/**
 * 固定频率刷新调度：把“每秒 N 次”换算成每 tick 的累计额度。
 *
 * <p>20 tick 不能被 6 整除，用取模只能得到 6.67 次/秒（每 3 tick）或 5 次/秒（每 4 tick）；
 * 累计额度在 20 tick 内正好触发 6 次，间隔在 3/4 tick 之间交替，长期不漂移。
 */
final class RefreshScheduler {
	// 原版一秒钟的标准 tick 数。
	static final int TICKS_PER_SECOND = 20;

	private final int timesPerSecond;
	private int budget;

	RefreshScheduler(int timesPerSecond) {
		if (timesPerSecond <= 0 || timesPerSecond > TICKS_PER_SECOND) {
			throw new IllegalArgumentException("timesPerSecond must be in 1.." + TICKS_PER_SECOND);
		}
		this.timesPerSecond = timesPerSecond;
	}

	// 每 tick 调用一次：返回本 tick 是否到达刷新窗口。
	boolean tick() {
		this.budget += this.timesPerSecond;
		if (this.budget < TICKS_PER_SECOND) {
			return false;
		}
		this.budget -= TICKS_PER_SECOND;
		return true;
	}

	// 功能关闭或切换世界时重置：丢掉残留额度，重新开启后不会提前触发。
	void reset() {
		this.budget = 0;
	}
}
