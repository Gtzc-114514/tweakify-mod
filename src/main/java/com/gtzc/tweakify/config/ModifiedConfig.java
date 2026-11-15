package com.gtzc.tweakify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 修改分区：调整原版规则，行为上更接近基岩版或常见工具链的做法。
 *
 * <p>字段按原声明顺序排列，注释与默认值保持与拆分前一致；
 * 数值与枚举字段的清洗随字段一起搬到这里。
 */
public final class ModifiedConfig extends ConfigSection {
	private static final Logger LOGGER = LoggerFactory.getLogger("tweakify-mod-config");

	// 校验器绑定到当前实例：方法引用需要接收者，静态字段无法绑定。
	// transient：字段索引会跳过它，既不能出现在 JSON 里，也不是可配置项。
	private final transient List<FieldSanitizer> sanitizers;

	ModifiedConfig() {
		this.sanitizers = List.of(
			FieldSanitizer.of("itemDespawnTime", this::sanitizeItemDespawnTime),
			FieldSanitizer.of("itemDespawnTimeXpOrbs", this::sanitizeXpOrbDespawnTime),
			FieldSanitizer.of("nightVisionFadeOutDuration", this::sanitizeNightVisionFadeDuration),
			FieldSanitizer.of("nightVisionFadeOutCurve", this::sanitizeNightVisionCurve),
			FieldSanitizer.of("sprintFovIntensity", this::sanitizeSprintFovIntensity),
			FieldSanitizer.of("keepLastTorchMinCount", this::sanitizeKeepLastTorchMinCount),
			FieldSanitizer.of("keepMiningProgressMaxDistance", this::sanitizeMiningProgressDistance),
			FieldSanitizer.of("tooExpensiveMaxLevelCost", this::sanitizeTooExpensiveMaxCost),
			FieldSanitizer.of("wartBlockDecompositionOutput", this::sanitizeWartOutput),
			FieldSanitizer.of("curseRemovalScope", this::sanitizeCurseRemovalScope),
			FieldSanitizer.of("underwaterMiningPenaltyKeep", this::sanitizeUnderwaterPenaltyKeep),
			FieldSanitizer.of("airMiningPenaltyKeep", this::sanitizeAirPenaltyKeep),
			FieldSanitizer.of("bedTooFarMaxDistance", this::sanitizeBedDistance),
			FieldSanitizer.of("enchantmentLevelArabicStyle", this::sanitizeEnchantmentLevelStyle),
			FieldSanitizer.of("smithingTemplateKeepChance", this::sanitizeSmithingTemplateChance),
			FieldSanitizer.of("useItemSlowdownKeepPercent", this::sanitizeUseItemSlowdownKeep),
			FieldSanitizer.of("ironGolemRideMinecartMaxDistance", this::sanitizeIronGolemPickupDistance));
		// 启动期校验：新增数值/枚举字段忘记登记校验器时直接失败。
		assertSanitizerCoverage(fieldNamesOf(this.sanitizers));
	}

	// 掉落物消失时间（tick）：默认 6000 = 原版 5 分钟。超出范围的配置在读取时会被夹紧。
	public volatile int itemDespawnTime = TweakifyModConfig.DEFAULT_ITEM_DESPAWN_TICKS;

	// 仅对玩家丢弃的掉落物使用上面的时间；其它来源保持原版 6000。
	public volatile boolean itemDespawnPlayerDroppedOnly = false;

	// 经验球消失时间（tick），默认 6000 = 原版 5 分钟。
	public volatile int itemDespawnTimeXpOrbs = TweakifyModConfig.DEFAULT_ITEM_DESPAWN_TICKS;

	// 禁用使用物品减速：进食、拉弓、举盾等使用物品期间不再降低移动速度。
	public volatile boolean disableUseItemSlowdown = false;

	// 保留原版减速的百分比（0 ~ 100）；0 = 完全取消（默认），100 = 与原版相同。
	public volatile int useItemSlowdownKeepPercent = TweakifyModConfig.DEFAULT_USE_ITEM_SLOWDOWN_KEEP_PERCENT;

	// 这些物品仍保留原版的使用减速（物品 ID 列表）。
	volatile List<String> useItemSlowdownExceptions = new ArrayList<>();

	// 禁用受伤抖动：受到伤害时不再产生视角抖动。
	public volatile boolean disableHurtShake = false;

	// 关闭受伤抖动时保留死亡镜头倾斜（不清掉死亡倒下时的视角角度）。
	public volatile boolean hurtShakeKeepDeathTilt = false;

	// 禁用受伤变红：受伤时不再为生物叠加红色 overlay（死亡变红保留）。
	public volatile boolean disableHurtTint = false;

	// 禁用受伤变红时保留受伤闪白（默认开启，与原行为一致）。
	public volatile boolean hurtTintKeepWhiteFlash = true;

	// 禁用水下挖掘惩罚：水下挖掘不再按 SUBMERGED_MINING_SPEED 属性减速。
	public volatile boolean disableUnderwaterMiningPenalty = false;

	// 保留的水下挖掘惩罚百分比（0 = 完全去掉，100 = 原版）。
	public volatile int underwaterMiningPenaltyKeep = TweakifyModConfig.DEFAULT_MINING_PENALTY_KEEP_PERCENT;

	// 禁用空中挖掘惩罚：离地挖掘不再额外降低速度。
	public volatile boolean disableAirMiningPenalty = false;

	// 保留的空中挖掘惩罚百分比（0 = 完全去掉，100 = 原版）。
	public volatile int airMiningPenaltyKeep = TweakifyModConfig.DEFAULT_MINING_PENALTY_KEEP_PERCENT;

	// 诅咒附魔可移除：砂轮可以去除诅咒附魔（原版会保留诅咒）。
	public volatile boolean disableCurseEnchantmentRestriction = false;

	// 移除范围：all（全部诅咒，默认）/ binding_only / vanishing_only。
	public volatile String curseRemovalScope = TweakifyModConfig.CURSE_SCOPE_ALL;

	// 禁用附魔过于昂贵：铁砧花费达到 40 级时仍可取出结果（仅单人/局域网生效）。
	public volatile boolean disableTooExpensive = false;

	// 可承受的最大经验花费（0 = 不限制）；超过后按原版阻止或仅提示，由下一项控制。
	public volatile int tooExpensiveMaxLevelCost = TweakifyModConfig.DEFAULT_TOO_EXPENSIVE_MAX_LEVEL_COST;

	// 超过费用上限时只提示“过于昂贵”，仍然允许取出。
	public volatile boolean tooExpensiveWarnOnly = false;

	// 移除附魔冲突：铁砧合并装备时忽略附魔互斥限制，互斥附魔可以合到同一件装备上。
	public volatile boolean removeEnchantmentConflicts = false;

	// 仅同类附魔：只解除同一互斥组（同一 exclusive_set 标签）内部的限制。
	public volatile boolean removeEnchantmentConflictsSameGroupOnly = false;

	// 附魔描述：在物品提示中为每个附魔追加一行描述。
	public volatile boolean enchantmentDescriptions = false;

	// 只在按住潜行键时追加附魔描述。
	public volatile boolean enchantmentDescriptionsSneakOnly = false;

	// 第三人称自身名字：第三/第二人称显示玩家自己的名字。
	public volatile boolean thirdPersonPlayerName = false;

	// 只在按住潜行键时显示自己的名字。
	public volatile boolean thirdPersonPlayerNameSneakOnly = false;

	// 雪傀儡不受群系影响：温暖/干燥群系不再让雪傀儡因群系属性融化。
	// 雨、水和熔岩伤害仍按原版；判定在服务端执行，仅单人/局域网生效。
	public volatile boolean snowGolemBiomeImmunity = false;

	// 只在主世界生效（下界/末地仍按原版群系属性融化）。
	public volatile boolean snowGolemBiomeImmunityOverworldOnly = false;

	// 超平坦无史莱姆：超平坦世界中不再生成史莱姆。
	public volatile boolean disableFlatWorldSlimeSpawn = false;

	// 对所有世界类型生效（不再只限超平坦）。
	public volatile boolean flatWorldSlimeSpawnAllWorlds = false;

	// 更好的隐身：隐身时隐藏药水粒子、装备与第三人称手持物品。
	public volatile boolean betterInvisibility = false;

	// 子开关：隐藏隐身实体的药水效果粒子。
	public volatile boolean betterInvisibilityEffectParticles = true;

	// 子开关：隐藏隐身实体的装备、披风与手持物品。
	public volatile boolean betterInvisibilityLayers = true;

	// 子开关：羊驼隐身时，背上的地毯也一起隐藏。
	public volatile boolean betterInvisibilityLlamaCarpet = true;

	// 锻造模板不消耗：锻造台取出成品时保留模板槽物品。
	public volatile boolean disableSmithingTemplateConsumption = false;

	// 保留概率（0 ~ 100，默认 100 = 必定保留）。
	public volatile int smithingTemplateKeepChance = TweakifyModConfig.DEFAULT_SMITHING_TEMPLATE_KEEP_CHANCE;

	// 夜视淡出：夜视效果结束前平滑淡出，不再闪烁。
	public volatile boolean nightVisionFadeOut = false;

	// 夜视淡出曲线：smooth（平滑）/ linear（线性）。
	public volatile String nightVisionFadeOutCurve = TweakifyModConfig.NIGHT_VISION_CURVE_SMOOTH;

	// 夜视淡出时长（tick）：剩余时间进入该窗口后开始淡出。
	public volatile int nightVisionFadeOutDuration = TweakifyModConfig.DEFAULT_NIGHT_VISION_FADE_TICKS;

	// 附魔等级使用阿拉伯数字（1、2、3...）代替罗马数字。
	public volatile boolean enchantmentLevelArabic = false;

	// 数字样式：plain（锋利 3）/ parentheses（锋利 (3)）。
	public volatile String enchantmentLevelArabicStyle = TweakifyModConfig.ENCHANTMENT_LEVEL_STYLE_PLAIN;

	// 疣块分解：合成格里单独放下界疣块时，结果槽产出 9 个下界疣（仅单人/局域网生效）。
	public volatile boolean wartBlockDecomposition = false;

	// 产出数量（1 ~ 9，默认 9）。
	public volatile int wartBlockDecompositionOutput = TweakifyModConfig.DEFAULT_WART_DECOMPOSITION_OUTPUT;

	// 鞘翅水中飞行：滑翔时不再被流体移动逻辑接管，可在水中保持滑翔。
	public volatile boolean elytraWaterFlight = false;

	// 只在水中保持滑翔；岩浆中仍按原版下沉。
	public volatile boolean elytraWaterFlightOnlyWater = false;

	// 禁用地狱门特效：进入下界传送门时不再显示紫色覆盖层、镜头旋转与传送门音效。
	public volatile boolean disableNetherPortalEffect = false;

	// 子开关：隐藏紫色传送门覆盖层。
	public volatile boolean disableNetherPortalOverlay = true;

	// 子开关：关闭传送门的镜头旋转。
	public volatile boolean disableNetherPortalCamera = true;

	// 子开关：静音传送门的呼啸音效。
	public volatile boolean disableNetherPortalSound = true;

	// 禁用床太远：离床太远时也能直接入睡（仅单人/局域网生效）。
	public volatile boolean disableBedTooFar = false;

	// 允许入睡的最大距离（0 ~ 128 格，0 = 不限制）。
	public volatile int bedTooFarMaxDistance = TweakifyModConfig.DEFAULT_BED_TOO_FAR_DISTANCE;

	// 铁傀儡进矿车：空矿车碰到铁傀儡时让铁傀儡坐进矿车（仅单人/局域网生效）。
	// 只限普通矿车：漏斗/运输/动力/TNT 等特种矿车不拉铁傀儡。
	public volatile boolean ironGolemRideMinecart = false;

	// 只在玩家附近自动上车（0 = 不限制，默认）。
	public volatile int ironGolemRideMinecartMaxDistance = TweakifyModConfig.DEFAULT_IRON_GOLEM_PICKUP_DISTANCE;

	// 穿牌打开容器：准星对准告示牌时，直接打开它背后的容器（潜行时仍走告示牌原版逻辑）。
	public volatile boolean signOpenContainer = false;

	// 潜行时保持原版：开启时潜行交给告示牌自身，关闭后潜行也能穿牌开容器。
	public volatile boolean signOpenContainerSneakBypass = true;

	// 禁用蛛网减速：蜘蛛网不再把实体粘住并限制移动。
	public volatile boolean disableCobwebSlowdown = false;

	// 子开关：除玩家以外的实体也一起解除蛛网减速。
	public volatile boolean cobwebSlowdownAllEntities = false;

	// 禁用液体推动：水流与岩浆流不再对实体施加水平推力（垂直浮力保留）。
	public volatile boolean disableFluidPush = false;

	// 子开关：仅解除水流推力。
	public volatile boolean disableWaterPush = true;

	// 子开关：仅解除岩浆流推力。
	public volatile boolean disableLavaPush = true;

	// 禁用粘液块减速：站在粘液块上不再被乘算降低水平速度。
	public volatile boolean disableSlimeBlockSlowdown = false;

	// 子开关：同时取消粘液块的弹跳。
	public volatile boolean disableSlimeBlockBounce = true;

	// 禁用蜂蜜块减速：蜂蜜块的 0.4 速度系数不再影响移动速度。
	public volatile boolean disableHoneyBlockSlowdown = false;

	// 子开关：同时取消蜂蜜块的黏着与下滑限制。
	public volatile boolean disableHoneyBlockStick = true;

	// 连接方块轮廓：准星对准门、床、双箱等连接方块时显示整体轮廓（类似基岩版）。
	public volatile boolean connectedBlockOutline = false;

	// 门使用 2 格视觉高度（关闭时门保持原版轮廓）。
	public volatile boolean connectedBlockOutlineDoorFullHeight = true;

	// 禁用屏幕覆盖层（细雪/岩浆/水下）的总开关。
	public volatile boolean disableOverlayEffects = false;

	// 子开关：禁用细雪冰冻的白色描边覆盖层。
	public volatile boolean disablePowderSnowOverlay = true;

	// 子开关：禁用水下雾（26.3 的雾是独立系统，不是覆盖层）。
	public volatile boolean disableWaterFog = true;

	// 子开关：禁用岩浆雾。
	public volatile boolean disableLavaFog = true;

	// 子开关：禁用细雪雾。
	public volatile boolean disablePowderSnowFog = true;

	// 子开关：禁用岩浆的橙色覆盖层。
	public volatile boolean disableLavaOverlay = true;

	// 子开关：禁用水下的蓝色覆盖层。
	public volatile boolean disableWaterOverlay = true;

	// 禁用疾跑 FOV 变化：疾跑时不再拉大视野。
	public volatile boolean disableSprintFov = false;

	// 保留原版疾跑视野强度的百分比（0 = 完全禁用，100 = 原版）。
	public volatile int sprintFovIntensity = TweakifyModConfig.DEFAULT_SPRINT_FOV_INTENSITY;

	// 禁用耕地踩踏：从高处落到耕地上不会把耕地踩回泥土（仅单人/局域网生效）。
	public volatile boolean disableFarmlandTrampling = false;

	// 只保护不被玩家踩坏；动物与村民仍会踩坏耕地。
	public volatile boolean farmlandTramplingPlayerOnly = false;

	// 切换物品不打断挖掘：挖掘中切换手持物品时保留当前破坏进度（像基岩版）。
	public volatile boolean keepMiningProgressOnItemSwitch = false;

	// 保留进度的最大距离（格，0 = 不限制）；超出后按原版重置进度。
	public volatile int keepMiningProgressMaxDistance = TweakifyModConfig.DEFAULT_KEEP_MINING_PROGRESS_DISTANCE;

	// 不使用最后一根火把：包里只剩一根火把时不允许放置，避免把最后的火把用掉。
	public volatile boolean keepLastTorch = false;

	// 剩余数量阈值（1 ~ 8）：剩余不超过该值时阻止放置。
	public volatile int keepLastTorchMinCount = TweakifyModConfig.DEFAULT_KEEP_LAST_TORCH_COUNT;

	// 经过可攀爬方块不减速：蹭到梯子/藤蔓边缘时不再被原版夹紧水平速度。
	public volatile boolean climbablePassNoSlowdown = false;

	// 只在按住潜行键时生效。
	public volatile boolean climbablePassNoSlowdownSneakOnly = false;

	// 可见交易显示：在村民交易界面显示未来等级（锁定）的交易。
	public volatile boolean visibleTraders = false;

	// 只显示未解锁（锁定）的交易，隐藏当前等级已解锁的交易。
	public volatile boolean visibleTradersOnlyLocked = false;

	// 强制关闭加载屏幕：跳过世界/资源包加载屏幕的渲染，世界仍在后台加载。
	public volatile boolean forceCloseLoadingScreen = false;

	// 子开关：直接关闭加载界面（完全跳过，可能短暂掉入世界）。
	public volatile boolean forceCloseLoadingScreenInstant = false;

	// 子开关：保留加载界面的文字与进度（不跳过渲染）。
	public volatile boolean forceCloseLoadingScreenKeepFirst = false;

	// 子开关：资源包加载屏加速（去淡入、立即淡出、移除覆盖层）。
	public volatile boolean forceCloseLoadingScreenSkipResourcePackFade = true;

	// 关闭资源包兼容性提示：资源包界面不显示不兼容标记，也不弹确认框（仅资源包）。
	public volatile boolean disableResourcePackCompatibilityCheck = false;

	// 同时忽略数据包的兼容性提示（默认只作用于资源包）。
	public volatile boolean resourcePackCompatibilityAlsoDataPacks = false;

	// 「禁用屏幕覆盖层」的切换快捷键。
	public volatile String hotkeyDisableOverlayEffects = "";

	@Override
	boolean sanitize() {
		return runSanitizers(this.sanitizers);
	}

	@Override
	List<String> sanitizedFieldNames() {
		return fieldNamesOf(this.sanitizers);
	}

	// 修正掉落物消失时间：手工改坏配置时夹回允许范围。
	private boolean sanitizeItemDespawnTime() {
		int clamped = Math.max(TweakifyModConfig.MIN_ITEM_DESPAWN_TICKS,
			Math.min(TweakifyModConfig.MAX_ITEM_DESPAWN_TICKS, itemDespawnTime));
		if (clamped == itemDespawnTime) {
			return false;
		}
		LOGGER.warn("Clamping itemDespawnTime from {} to {}", itemDespawnTime, clamped);
		itemDespawnTime = clamped;
		return true;
	}

	// 修正经验球消失时间，手工改坏配置时夹回允许范围。
	private boolean sanitizeXpOrbDespawnTime() {
		int clamped = Math.max(TweakifyModConfig.MIN_ITEM_DESPAWN_TICKS,
			Math.min(TweakifyModConfig.MAX_ITEM_DESPAWN_TICKS, itemDespawnTimeXpOrbs));
		if (clamped == itemDespawnTimeXpOrbs) {
			return false;
		}
		LOGGER.warn("Clamping itemDespawnTimeXpOrbs from {} to {}", itemDespawnTimeXpOrbs, clamped);
		itemDespawnTimeXpOrbs = clamped;
		return true;
	}

	// 修正夜视淡出时长。
	private boolean sanitizeNightVisionFadeDuration() {
		int clamped = Math.max(TweakifyModConfig.MIN_NIGHT_VISION_FADE_TICKS,
			Math.min(TweakifyModConfig.MAX_NIGHT_VISION_FADE_TICKS, nightVisionFadeOutDuration));
		if (clamped == nightVisionFadeOutDuration) {
			return false;
		}
		LOGGER.warn("Clamping nightVisionFadeOutDuration from {} to {}", nightVisionFadeOutDuration, clamped);
		nightVisionFadeOutDuration = clamped;
		return true;
	}

	// 修正夜视淡出曲线。
	private boolean sanitizeNightVisionCurve() {
		if (TweakifyModConfig.isSupportedValue(
			TweakifyModConfig.SUPPORTED_NIGHT_VISION_CURVES, nightVisionFadeOutCurve)) {
			return false;
		}
		LOGGER.warn("Ignoring unsupported night vision curve: {}", nightVisionFadeOutCurve);
		nightVisionFadeOutCurve = TweakifyModConfig.NIGHT_VISION_CURVE_SMOOTH;
		return true;
	}

	// 修正疾跑视野保留强度。
	private boolean sanitizeSprintFovIntensity() {
		int clamped = Math.max(TweakifyModConfig.MIN_SPRINT_FOV_INTENSITY,
			Math.min(TweakifyModConfig.MAX_SPRINT_FOV_INTENSITY, sprintFovIntensity));
		if (clamped == sprintFovIntensity) {
			return false;
		}
		LOGGER.warn("Clamping sprintFovIntensity from {} to {}", sprintFovIntensity, clamped);
		sprintFovIntensity = clamped;
		return true;
	}

	// 修正火把保留数量阈值。
	private boolean sanitizeKeepLastTorchMinCount() {
		int clamped = Math.max(TweakifyModConfig.MIN_KEEP_LAST_TORCH_COUNT,
			Math.min(TweakifyModConfig.MAX_KEEP_LAST_TORCH_COUNT, keepLastTorchMinCount));
		if (clamped == keepLastTorchMinCount) {
			return false;
		}
		LOGGER.warn("Clamping keepLastTorchMinCount from {} to {}", keepLastTorchMinCount, clamped);
		keepLastTorchMinCount = clamped;
		return true;
	}

	// 修正挖掘进度保留距离。
	private boolean sanitizeMiningProgressDistance() {
		int clamped = Math.max(TweakifyModConfig.MIN_KEEP_MINING_PROGRESS_DISTANCE,
			Math.min(TweakifyModConfig.MAX_KEEP_MINING_PROGRESS_DISTANCE, keepMiningProgressMaxDistance));
		if (clamped == keepMiningProgressMaxDistance) {
			return false;
		}
		LOGGER.warn("Clamping keepMiningProgressMaxDistance from {} to {}",
			keepMiningProgressMaxDistance, clamped);
		keepMiningProgressMaxDistance = clamped;
		return true;
	}

	// 修正铁砧费用上限。
	private boolean sanitizeTooExpensiveMaxCost() {
		int clamped = Math.max(TweakifyModConfig.MIN_TOO_EXPENSIVE_MAX_LEVEL_COST,
			Math.min(TweakifyModConfig.MAX_TOO_EXPENSIVE_MAX_LEVEL_COST, tooExpensiveMaxLevelCost));
		if (clamped == tooExpensiveMaxLevelCost) {
			return false;
		}
		LOGGER.warn("Clamping tooExpensiveMaxLevelCost from {} to {}", tooExpensiveMaxLevelCost, clamped);
		tooExpensiveMaxLevelCost = clamped;
		return true;
	}

	// 修正疣块分解产出数量。
	private boolean sanitizeWartOutput() {
		int clamped = Math.max(TweakifyModConfig.MIN_WART_DECOMPOSITION_OUTPUT,
			Math.min(TweakifyModConfig.MAX_WART_DECOMPOSITION_OUTPUT, wartBlockDecompositionOutput));
		if (clamped == wartBlockDecompositionOutput) {
			return false;
		}
		LOGGER.warn("Clamping wartBlockDecompositionOutput from {} to {}",
			wartBlockDecompositionOutput, clamped);
		wartBlockDecompositionOutput = clamped;
		return true;
	}

	// 修正诅咒移除范围，回退到全部诅咒。
	private boolean sanitizeCurseRemovalScope() {
		if (TweakifyModConfig.isSupportedValue(TweakifyModConfig.SUPPORTED_CURSE_SCOPES, curseRemovalScope)) {
			return false;
		}
		LOGGER.warn("Ignoring unsupported curse removal scope: {}", curseRemovalScope);
		curseRemovalScope = TweakifyModConfig.CURSE_SCOPE_ALL;
		return true;
	}

	// 修正水下挖掘惩罚保留百分比。
	private boolean sanitizeUnderwaterPenaltyKeep() {
		int clamped = Math.max(TweakifyModConfig.MIN_MINING_PENALTY_KEEP_PERCENT,
			Math.min(TweakifyModConfig.MAX_MINING_PENALTY_KEEP_PERCENT, underwaterMiningPenaltyKeep));
		if (clamped == underwaterMiningPenaltyKeep) {
			return false;
		}
		LOGGER.warn("Clamping underwaterMiningPenaltyKeep from {} to {}",
			underwaterMiningPenaltyKeep, clamped);
		underwaterMiningPenaltyKeep = clamped;
		return true;
	}

	// 修正空中挖掘惩罚保留百分比。
	private boolean sanitizeAirPenaltyKeep() {
		int clamped = Math.max(TweakifyModConfig.MIN_MINING_PENALTY_KEEP_PERCENT,
			Math.min(TweakifyModConfig.MAX_MINING_PENALTY_KEEP_PERCENT, airMiningPenaltyKeep));
		if (clamped == airMiningPenaltyKeep) {
			return false;
		}
		LOGGER.warn("Clamping airMiningPenaltyKeep from {} to {}", airMiningPenaltyKeep, clamped);
		airMiningPenaltyKeep = clamped;
		return true;
	}

	// 修正睡觉最大距离。
	private boolean sanitizeBedDistance() {
		int clamped = Math.max(TweakifyModConfig.MIN_BED_TOO_FAR_DISTANCE,
			Math.min(TweakifyModConfig.MAX_BED_TOO_FAR_DISTANCE, bedTooFarMaxDistance));
		if (clamped == bedTooFarMaxDistance) {
			return false;
		}
		LOGGER.warn("Clamping bedTooFarMaxDistance from {} to {}", bedTooFarMaxDistance, clamped);
		bedTooFarMaxDistance = clamped;
		return true;
	}

	// 修正附魔等级数字样式，回退到 plain。
	private boolean sanitizeEnchantmentLevelStyle() {
		if (TweakifyModConfig.isSupportedValue(
			TweakifyModConfig.SUPPORTED_ENCHANTMENT_LEVEL_STYLES, enchantmentLevelArabicStyle)) {
			return false;
		}
		LOGGER.warn("Ignoring unsupported enchantment level style: {}", enchantmentLevelArabicStyle);
		enchantmentLevelArabicStyle = TweakifyModConfig.ENCHANTMENT_LEVEL_STYLE_PLAIN;
		return true;
	}

	// 修正锻造模板保留概率。
	private boolean sanitizeSmithingTemplateChance() {
		int clamped = Math.max(TweakifyModConfig.MIN_SMITHING_TEMPLATE_KEEP_CHANCE,
			Math.min(TweakifyModConfig.MAX_SMITHING_TEMPLATE_KEEP_CHANCE, smithingTemplateKeepChance));
		if (clamped == smithingTemplateKeepChance) {
			return false;
		}
		LOGGER.warn("Clamping smithingTemplateKeepChance from {} to {}",
			smithingTemplateKeepChance, clamped);
		smithingTemplateKeepChance = clamped;
		return true;
	}

	// 修正使用物品减速保留百分比。
	private boolean sanitizeUseItemSlowdownKeep() {
		int clamped = Math.max(TweakifyModConfig.MIN_USE_ITEM_SLOWDOWN_KEEP_PERCENT,
			Math.min(TweakifyModConfig.MAX_USE_ITEM_SLOWDOWN_KEEP_PERCENT, useItemSlowdownKeepPercent));
		if (clamped == useItemSlowdownKeepPercent) {
			return false;
		}
		LOGGER.warn("Clamping useItemSlowdownKeepPercent from {} to {}",
			useItemSlowdownKeepPercent, clamped);
		useItemSlowdownKeepPercent = clamped;
		return true;
	}

	// 修正铁傀儡上车距离。
	private boolean sanitizeIronGolemPickupDistance() {
		int clamped = Math.max(TweakifyModConfig.MIN_IRON_GOLEM_PICKUP_DISTANCE,
			Math.min(TweakifyModConfig.MAX_IRON_GOLEM_PICKUP_DISTANCE, ironGolemRideMinecartMaxDistance));
		if (clamped == ironGolemRideMinecartMaxDistance) {
			return false;
		}
		LOGGER.warn("Clamping ironGolemRideMinecartMaxDistance from {} to {}",
			ironGolemRideMinecartMaxDistance, clamped);
		ironGolemRideMinecartMaxDistance = clamped;
		return true;
	}
}
