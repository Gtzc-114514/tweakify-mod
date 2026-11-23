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
			FieldSanitizer.of("betterBoldOffsetPercent", this::sanitizeBoldOffset),
			FieldSanitizer.of("chatLagFixTimeoutSeconds", this::sanitizeChatLagTimeout),
			FieldSanitizer.of("chatLagFixCacheSeconds", this::sanitizeChatLagCache),
			FieldSanitizer.of("experienceBarHighLevelFixMinLevel", this::sanitizeXpFixMinLevel),
			FieldSanitizer.of("cooldownIndicatorFixMinProgress", this::sanitizeCooldownMinProgress),
			FieldSanitizer.of("fishingLineThirdPersonFixOffset", this::sanitizeFishingLineOffset));
		assertSanitizerCoverage(fieldNamesOf(this.sanitizers));
	}

	// MC-19830：睡在床上时不再推迟落地伤害，上床瞬间立即结算（仅单人/局域网生效）。
	public volatile boolean bedFallDamageFix = false;

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

	// 只在疾跑跳跃时放宽。
	public volatile boolean frostWalkerJumpFixSprintOnly = false;

	// MC-577：物品栏里绑定到鼠标键的关闭背包与丢弃物品按键生效。
	public volatile boolean fixMouseButtonInventory = false;

	// 鼠标键关闭背包：允许用绑定的鼠标键关闭容器界面。
	public volatile boolean mouseButtonInventoryClose = true;

	// 鼠标键丢弃物品：允许用绑定的鼠标键丢弃悬停槽位物品。
	public volatile boolean mouseButtonInventoryDrop = true;

	// MC-247973：收到聊天消息时改为后台拉取屏蔽名单，避免主线程同步网络请求卡顿。
	public volatile boolean chatLagFix = false;

	// 后台拉取屏蔽名单的超时时间（秒）。
	public volatile int chatLagFixTimeoutSeconds = TweakifyModConfig.DEFAULT_CHAT_LAG_TIMEOUT_SECONDS;

	// 后台屏蔽名单缓存的刷新间隔（秒，0 = 永不过期）。
	public volatile int chatLagFixCacheSeconds = TweakifyModConfig.DEFAULT_CHAT_LAG_CACHE_SECONDS;

	// MC-79545：等级极高时经验需求整数溢出，经验条仍然正常显示。
	public volatile boolean experienceBarHighLevelFix = false;

	// 只在等级不低于该值时修复（0 = 总是修复）。
	public volatile int experienceBarHighLevelFixMinLevel = TweakifyModConfig.DEFAULT_XP_HIGH_LEVEL_FIX_LEVEL;

	// MC-61489：书本查看/编辑界面整体垂直居中，底部按钮不再贴着屏幕边缘。
	public volatile boolean bookGuiVerticalCenterFix = false;

	// 只对书本查看界面生效。
	public volatile boolean bookGuiCenterViewScreen = true;

	// 只对书与笔编辑界面生效。
	public volatile boolean bookGuiCenterEditScreen = true;

	// MC-84661 / MC-170462：药水类物品提示把发光与不祥之兆按有害效果显示。
	public volatile boolean potionTooltipCategoryFix = false;

	// 子开关：分别修正发光与不祥之兆。
	public volatile boolean potionTooltipCategoryGlowing = true;
	public volatile boolean potionTooltipCategoryBadOmen = true;

	// MC-105068：完全格挡攻击时不再播放玩家受伤音效，保留盾牌格挡/禁用音效。
	public volatile boolean shieldBlockHurtSoundFix = false;

	// 只对玩家生效；关闭时也处理模组生物。
	public volatile boolean shieldBlockHurtSoundFixPlayersOnly = false;

	// MC-217716：旁观者模式下隐藏反胃与下界传送门覆盖层。
	public volatile boolean spectatorOverlayFix = false;

	// 子开关：分别隐藏传送门覆盖层、反胃效果与镜头旋转。
	public volatile boolean spectatorOverlayHidePortal = true;
	public volatile boolean spectatorOverlayHideNausea = true;
	public volatile boolean spectatorOverlayHideSpinning = true;

	// MC-268420：高攻速切换物品时，攻击冷却指示器不再闪一帧。
	public volatile boolean cooldownIndicatorFix = false;

	// 冷却进度低于该像素值时也跳过背景（0 = 只跳过空进度）。
	public volatile int cooldownIndicatorFixMinProgress = TweakifyModConfig.DEFAULT_COOLDOWN_INDICATOR_PROGRESS;

	// MC-4490：潜行时第三人称鱼线仍连接到鱼竿。
	public volatile boolean fishingLineThirdPersonFix = false;

	// 鱼线下移量（格）。
	public volatile double fishingLineThirdPersonFixOffset = TweakifyModConfig.DEFAULT_FISHING_LINE_FIX_OFFSET;

	// MC-143474：重生后保留快捷栏选中的槽位。
	public volatile boolean respawnHotbarFix = false;

	// 维度切换（传送门）时也保留快捷栏槽位；关闭后只在死亡重生时恢复。
	public volatile boolean respawnHotbarKeepOnDimensionChange = true;

	// MC-159163：快速按潜行键时不再重复播放潜行动画。
	public volatile boolean sneakAnimationFix = false;

	// 只在地面时移除本地 POS 回传；空中保留原版姿态同步。
	public volatile boolean sneakAnimationFixOnlyOnGround = false;

	// 只在本地玩家潜行时移除 POS 回传。
	public volatile boolean sneakAnimationFixOnlyWhenSneaking = false;

	// MC-121706：骷髅/幻术师射箭时视线跟随目标上下。
	public volatile boolean skeletonAimingFix = false;

	// 只修正骷髅类（含流浪者/沼骸），不处理幻术师。
	public volatile boolean skeletonAimingOnlySkeletons = false;

	// 只在目标高于施法者时修正俯仰角。
	public volatile boolean skeletonAimingOnlyTargetAbove = false;

	// MC-8187：2×2 云杉/丛林树苗北/西侧有相邻方块时也能生长。
	public volatile boolean saplingGrowthFix = false;

	@Override
	boolean sanitize() {
		return runSanitizers(this.sanitizers);
	}

	@Override
	List<String> sanitizedFieldNames() {
		return fieldNamesOf(this.sanitizers);
	}

	// 修正聊天屏蔽名单拉取超时。
	private boolean sanitizeChatLagTimeout() {
		int clamped = Math.max(TweakifyModConfig.MIN_CHAT_LAG_TIMEOUT_SECONDS,
			Math.min(TweakifyModConfig.MAX_CHAT_LAG_TIMEOUT_SECONDS, chatLagFixTimeoutSeconds));
		if (clamped == chatLagFixTimeoutSeconds) {
			return false;
		}
		LOGGER.warn("Clamping chatLagFixTimeoutSeconds from {} to {}",
			chatLagFixTimeoutSeconds, clamped);
		chatLagFixTimeoutSeconds = clamped;
		return true;
	}

	// 修正聊天屏蔽名单缓存刷新间隔。
	private boolean sanitizeChatLagCache() {
		int clamped = Math.max(TweakifyModConfig.MIN_CHAT_LAG_CACHE_SECONDS,
			Math.min(TweakifyModConfig.MAX_CHAT_LAG_CACHE_SECONDS, chatLagFixCacheSeconds));
		if (clamped == chatLagFixCacheSeconds) {
			return false;
		}
		LOGGER.warn("Clamping chatLagFixCacheSeconds from {} to {}", chatLagFixCacheSeconds, clamped);
		chatLagFixCacheSeconds = clamped;
		return true;
	}

	// 修正经验条修复的等级下限。
	private boolean sanitizeXpFixMinLevel() {
		int clamped = Math.max(TweakifyModConfig.MIN_XP_HIGH_LEVEL_FIX_LEVEL,
			Math.min(TweakifyModConfig.MAX_XP_HIGH_LEVEL_FIX_LEVEL, experienceBarHighLevelFixMinLevel));
		if (clamped == experienceBarHighLevelFixMinLevel) {
			return false;
		}
		LOGGER.warn("Clamping experienceBarHighLevelFixMinLevel from {} to {}",
			experienceBarHighLevelFixMinLevel, clamped);
		experienceBarHighLevelFixMinLevel = clamped;
		return true;
	}

	// 修正冷却指示器最小进度。
	private boolean sanitizeCooldownMinProgress() {
		int clamped = Math.max(TweakifyModConfig.MIN_COOLDOWN_INDICATOR_PROGRESS,
			Math.min(TweakifyModConfig.MAX_COOLDOWN_INDICATOR_PROGRESS, cooldownIndicatorFixMinProgress));
		if (clamped == cooldownIndicatorFixMinProgress) {
			return false;
		}
		LOGGER.warn("Clamping cooldownIndicatorFixMinProgress from {} to {}",
			cooldownIndicatorFixMinProgress, clamped);
		cooldownIndicatorFixMinProgress = clamped;
		return true;
	}

	// 修正鱼线下移量。
	private boolean sanitizeFishingLineOffset() {
		double clamped = Math.max(TweakifyModConfig.MIN_FISHING_LINE_FIX_OFFSET,
			Math.min(TweakifyModConfig.MAX_FISHING_LINE_FIX_OFFSET, fishingLineThirdPersonFixOffset));
		if (Double.compare(clamped, fishingLineThirdPersonFixOffset) == 0) {
			return false;
		}
		LOGGER.warn("Clamping fishingLineThirdPersonFixOffset from {} to {}",
			fishingLineThirdPersonFixOffset, clamped);
		fishingLineThirdPersonFixOffset = clamped;
		return true;
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
