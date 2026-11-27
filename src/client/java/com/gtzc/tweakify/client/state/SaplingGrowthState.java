package com.gtzc.tweakify.client.state;

/**
 * 2×2 树苗生长修复（MC-8187）的纯函数。
 *
 * <p>原版在基础尺寸为 1 时把扫描偏移设为 -1，北/西侧相邻方块会被误判为阻挡；
 * 开启后把该偏移改为 0，其它尺寸与关闭状态保持原值。
 */
public final class SaplingGrowthState {
	private SaplingGrowthState() {
	}

	public static int fixedOffset(boolean enabled, int offset, int baseSize) {
		if (enabled && offset == -1 && baseSize == 1) {
			return 0;
		}
		return offset;
	}
}
