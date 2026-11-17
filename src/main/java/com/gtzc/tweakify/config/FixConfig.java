package com.gtzc.tweakify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 修复分区：只修正原版瑕疵，不新增行为。
 *
 * <p>布尔字段由 facade 的按类型清洗统一兜底；数值字段需要登记字段级校验器。
 */
public final class FixConfig extends ConfigSection {
	private static final Logger LOGGER = LoggerFactory.getLogger("tweakify-mod-config");

	// 校验器绑定到当前实例：方法引用需要接收者，静态字段无法绑定。
	private final transient List<FieldSanitizer> sanitizers;

	FixConfig() {
		this.sanitizers = List.of(
			FieldSanitizer.of("betterBoldOffsetPercent", this::sanitizeBoldOffset));
		assertSanitizerCoverage(fieldNamesOf(this.sanitizers));
	}

	// 草径卡船修复：土径对船的碰撞按满方块处理，消除土径与完整方块之间的 1/16 台阶。
	public volatile boolean fixBoatStuckOnDirtPath = false;

	// 只对载有玩家的船生效（空船与动物船保持原版）。
	public volatile boolean fixBoatStuckOnDirtPathPlayerOnly = false;

	// 鹦鹉肩膀跳舞：唱片机播放音乐时，站在玩家肩膀上的鹦鹉也会跳舞（与基岩版一致）。
	public volatile boolean parrotDanceOnShoulder = false;

	// 只让本地玩家肩膀上的鹦鹉跳舞（其它玩家保持原版姿态）。
	public volatile boolean parrotDanceOnShoulderLocalPlayerOnly = false;

	// 更好的粗体：把粗体偏移从原版 1.0 改为配置百分比（默认 0.5，与 MCPatcher/OptiFine 一致）。
	public volatile boolean betterBold = false;

	// 粗体偏移百分比（10 ~ 100，默认 50 = 0.5 像素）。
	public volatile int betterBoldOffsetPercent = TweakifyModConfig.DEFAULT_BETTER_BOLD_OFFSET_PERCENT;

	// 冰霜行者跳跃修复：跳跃时（离地状态）也继续在脚下结冰，
	// 修正原版只在站地帧结冰、跳着过水面时会踩空落水的问题。
	public volatile boolean frostWalkerJumpFix = false;

	// 只在向上跳跃时放宽（下落过程仍按原版不结冰）。
	public volatile boolean frostWalkerJumpFixJumpOnly = false;

	// MC-577：物品栏里绑定到鼠标键的关闭背包与丢弃物品按键生效。
	public volatile boolean fixMouseButtonInventory = false;

	// MC-247973：收到聊天消息时改为后台拉取屏蔽名单，避免主线程同步网络请求卡顿。
	public volatile boolean chatLagFix = false;

	// MC-79545：等级极高时经验需求整数溢出，经验条仍然正常显示。
	public volatile boolean experienceBarHighLevelFix = false;

	// MC-61489：书本查看/编辑界面整体垂直居中，底部按钮不再贴着屏幕边缘。
	public volatile boolean bookGuiVerticalCenterFix = false;

	// MC-84661 / MC-170462：药水类物品提示把发光与不祥之兆按有害效果显示。
	public volatile boolean potionTooltipCategoryFix = false;

	// MC-105068：完全格挡攻击时不再播放玩家受伤音效，保留盾牌格挡/禁用音效。
	public volatile boolean shieldBlockHurtSoundFix = false;

	// MC-217716：旁观者模式下隐藏反胃与下界传送门覆盖层。
	public volatile boolean spectatorOverlayFix = false;

	// MC-268420：高攻速切换物品时，攻击冷却指示器不再闪一帧。
	public volatile boolean cooldownIndicatorFix = false;

	// MC-4490：潜行时第三人称鱼线仍连接到鱼竿。
	public volatile boolean fishingLineThirdPersonFix = false;

	@Override
	boolean sanitize() {
		return runSanitizers(this.sanitizers);
	}

	@Override
	List<String> sanitizedFieldNames() {
		return fieldNamesOf(this.sanitizers);
	}

	// 修正粗体偏移百分比，手工改坏配置时夹回允许范围。
	private boolean sanitizeBoldOffset() {
		int clamped = Math.max(TweakifyModConfig.MIN_BETTER_BOLD_OFFSET_PERCENT,
			Math.min(TweakifyModConfig.MAX_BETTER_BOLD_OFFSET_PERCENT, betterBoldOffsetPercent));
		if (clamped == betterBoldOffsetPercent) {
			return false;
		}
		LOGGER.warn("Clamping betterBoldOffsetPercent from {} to {}", betterBoldOffsetPercent, clamped);
		betterBoldOffsetPercent = clamped;
		return true;
	}
}
