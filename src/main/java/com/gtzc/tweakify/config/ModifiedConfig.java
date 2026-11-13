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
			FieldSanitizer.of("nightVisionFadeOutCurve", this::sanitizeNightVisionCurve));
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

	// 这些物品仍保留原版的使用减速（物品 ID 列表）。
	volatile List<String> useItemSlowdownExceptions = new ArrayList<>();

	// 禁用受伤抖动：受到伤害时不再产生视角抖动。
	public volatile boolean disableHurtShake = false;

	// 禁用受伤变红：受伤时不再为生物叠加红色 overlay（死亡变红保留）。
	public volatile boolean disableHurtTint = false;

	// 禁用水下挖掘惩罚：水下挖掘不再按 SUBMERGED_MINING_SPEED 属性减速。
	public volatile boolean disableUnderwaterMiningPenalty = false;

	// 禁用空中挖掘惩罚：离地挖掘不再额外降低速度。
	public volatile boolean disableAirMiningPenalty = false;

	// 诅咒附魔可移除：砂轮可以去除诅咒附魔（原版会保留诅咒）。
	public volatile boolean disableCurseEnchantmentRestriction = false;

	// 禁用附魔过于昂贵：铁砧花费达到 40 级时仍可取出结果（仅单人/局域网生效）。
	public volatile boolean disableTooExpensive = false;

	// 移除附魔冲突：铁砧合并装备时忽略附魔互斥限制，互斥附魔可以合到同一件装备上。
	public volatile boolean removeEnchantmentConflicts = false;

	// 附魔描述：在物品提示中为每个附魔追加一行描述。
	public volatile boolean enchantmentDescriptions = false;

	// 第三人称自身名字：第三/第二人称显示玩家自己的名字。
	public volatile boolean thirdPersonPlayerName = false;

	// 雪傀儡不受群系影响：温暖/干燥群系不再让雪傀儡因群系属性融化。
	// 雨、水和熔岩伤害仍按原版；判定在服务端执行，仅单人/局域网生效。
	public volatile boolean snowGolemBiomeImmunity = false;

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

	// 夜视淡出：夜视效果结束前平滑淡出，不再闪烁。
	public volatile boolean nightVisionFadeOut = false;

	// 夜视淡出曲线：smooth（平滑）/ linear（线性）。
	public volatile String nightVisionFadeOutCurve = TweakifyModConfig.NIGHT_VISION_CURVE_SMOOTH;

	// 夜视淡出时长（tick）：剩余时间进入该窗口后开始淡出。
	public volatile int nightVisionFadeOutDuration = TweakifyModConfig.DEFAULT_NIGHT_VISION_FADE_TICKS;

	// 附魔等级使用阿拉伯数字（1、2、3...）代替罗马数字。
	public volatile boolean enchantmentLevelArabic = false;

	// 疣块分解：合成格里单独放下界疣块时，结果槽产出 9 个下界疣（仅单人/局域网生效）。
	public volatile boolean wartBlockDecomposition = false;

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

	// 铁傀儡进矿车：空矿车碰到铁傀儡时让铁傀儡坐进矿车（仅单人/局域网生效）。
	public volatile boolean ironGolemRideMinecart = false;

	// 连接方块轮廓：准星对准门、床、双箱等连接方块时显示整体轮廓（类似基岩版）。
	public volatile boolean connectedBlockOutline = false;

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

	// 禁用耕地踩踏：从高处落到耕地上不会把耕地踩回泥土（仅单人/局域网生效）。
	public volatile boolean disableFarmlandTrampling = false;

	// 只保护不被玩家踩坏；动物与村民仍会踩坏耕地。
	public volatile boolean farmlandTramplingPlayerOnly = false;

	// 切换物品不打断挖掘：挖掘中切换手持物品时保留当前破坏进度（像基岩版）。
	public volatile boolean keepMiningProgressOnItemSwitch = false;

	// 不使用最后一根火把：包里只剩一根火把时不允许放置，避免把最后的火把用掉。
	public volatile boolean keepLastTorch = false;

	// 经过可攀爬方块不减速：蹭到梯子/藤蔓边缘时不再被原版夹紧水平速度。
	public volatile boolean climbablePassNoSlowdown = false;

	// 可见交易显示：在村民交易界面显示未来等级（锁定）的交易。
	public volatile boolean visibleTraders = false;

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
}
