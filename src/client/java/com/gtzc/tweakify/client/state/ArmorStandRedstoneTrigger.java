package com.gtzc.tweakify.client.state;

/**
 * 更好的盔甲架：红石上升沿检测（与姿态循环解耦，便于单测）。
 *
 * <p>首个 tick 只播种当前电平：实体重建（区块重载/重进世界）后若线路仍通电，
 * 不能把「初次看到通电」当成脉冲，否则每次进世界都会白跳一个姿态。
 * 之后只在「上一次无信号、这一次有信号」时返回 true，持续通电不会反复切换。
 */
public final class ArmorStandRedstoneTrigger {
	private boolean powered;
	private boolean seeded;

	// 返回本 tick 是否应切换到下一个姿态。
	public boolean update(boolean poweredNow) {
		if (!this.seeded) {
			this.seeded = true;
			this.powered = poweredNow;
			return false;
		}
		boolean rising = poweredNow && !this.powered;
		this.powered = poweredNow;
		return rising;
	}
}
