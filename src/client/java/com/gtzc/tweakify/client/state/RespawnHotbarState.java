package com.gtzc.tweakify.client.state;

/**
 * 重生快捷栏修复（MC-143474）的纯函数。
 *
 * <p>原版重生时新玩家默认选中第 0 槽；开启后返回旧玩家的选中槽，
 * 关闭时返回 -1 表示不修改新玩家，保持原版行为。
 */
public final class RespawnHotbarState {
	private static final int NO_CHANGE = -1;

	private RespawnHotbarState() {
	}

	public static int slotToRestore(boolean enabled, int oldSelectedSlot) {
		return enabled ? oldSelectedSlot : NO_CHANGE;
	}

	// 是否在本次重生恢复槽位：可配置维度切换时是否也恢复。
	public static boolean shouldRestore(boolean enabled, boolean keepOnDimensionChange, boolean dimensionChange) {
		return enabled && (keepOnDimensionChange || !dimensionChange);
	}
}
