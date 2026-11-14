package com.gtzc.tweakify.config;

/**
 * 修复分区：只修正原版瑕疵，不新增行为。
 *
 * <p>字段全部是布尔开关，由 facade 的按类型清洗统一兜底，因此不登记字段级校验器。
 */
public final class FixConfig extends ConfigSection {
	FixConfig() {
		// 全部字段都是布尔开关，这里只需要确认没有漏登记的数值/枚举字段。
		assertSanitizerCoverage(java.util.List.of());
	}

	// 草径卡船修复：土径对船的碰撞按满方块处理，消除土径与完整方块之间的 1/16 台阶。
	public volatile boolean fixBoatStuckOnDirtPath = false;

	// 鹦鹉肩膀跳舞：唱片机播放音乐时，站在玩家肩膀上的鹦鹉也会跳舞（与基岩版一致）。
	public volatile boolean parrotDanceOnShoulder = false;

	// 更好的粗体：把粗体偏移从原版 1.0 改为 0.5（与 MCPatcher/OptiFine 一致）。
	public volatile boolean betterBold = false;

	// 冰霜行者跳跃修复：跳跃时（离地状态）也继续在脚下结冰，
	// 修正原版只在站地帧结冰、跳着过水面时会踩空落水的问题。
	public volatile boolean frostWalkerJumpFix = false;
}
