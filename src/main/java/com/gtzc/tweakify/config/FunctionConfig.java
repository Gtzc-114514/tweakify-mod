package com.gtzc.tweakify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 功能分区：新增原版没有的行为，也是快捷键支持最完整的一组。
 *
 * <p>字段按原声明顺序排列，注释与默认值保持与拆分前一致；
 * 数值与枚举字段的清洗随字段一起搬到这里。
 */
public final class FunctionConfig extends ConfigSection {
	private static final Logger LOGGER = LoggerFactory.getLogger("tweakify-mod-config");

	// 校验器绑定到当前实例：方法引用需要接收者，静态字段无法绑定。
	// transient：字段索引会跳过它，既不能出现在 JSON 里，也不是可配置项。
	private final transient List<FieldSanitizer> sanitizers;

	FunctionConfig() {
		this.sanitizers = List.of(
			FieldSanitizer.of("durabilityOverlayLowThreshold", this::sanitizeDurabilityThreshold),
			FieldSanitizer.of("betterBlockPlacementMode", this::sanitizeBlockPlacementMode),
			FieldSanitizer.of("copyItemIdFormat", this::sanitizeCopyIdFormat),
			FieldSanitizer.of("chatSelectionColor", this::sanitizeChatSelectionColor),
			FieldSanitizer.of("chatSelectionOpacity", this::sanitizeChatSelectionOpacity),
			FieldSanitizer.of("durabilityDisplayMode", this::sanitizeDurabilityDisplayMode),
			FieldSanitizer.of("durabilityOverlayColorMode", this::sanitizeDurabilityColorMode),
			FieldSanitizer.of("attackRangeDisplayMode", this::sanitizeAttackRangeDisplayMode),
			new FieldSanitizer(List.of("attackRangeMaxDistance", "attackRangeMaxTargets"),
				this::sanitizeAttackRangeLimits),
			FieldSanitizer.of("attackRangeRenderMode", this::sanitizeAttackRangeRenderMode),
			FieldSanitizer.of("attackRangePalette", this::sanitizeAttackRangePalette),
			FieldSanitizer.of("highlightSingleLayerBedrockMaxMarkers", this::sanitizeBedrockMarkerLimit),
			FieldSanitizer.of("highlightSingleLayerBedrockScanRadius", this::sanitizeBedrockScanRadius),
			FieldSanitizer.of("highlightSingleLayerBedrockColor", this::sanitizeBedrockMarkerColor),
			FieldSanitizer.of("barrierVisibilityMaxMarkers", this::sanitizeBarrierMarkerLimit),
			new FieldSanitizer(List.of("barrierVisibilityScanRadius", "barrierVisibilityParticleInterval"),
				this::sanitizeBarrierScanAndInterval),
			FieldSanitizer.of("autoStepUpHeight", this::sanitizeAutoStepUpHeight),
			FieldSanitizer.of("throwSpawnEggVelocity", this::sanitizeSpawnEggVelocity),
			FieldSanitizer.of("pickFluidBucketFluids", this::sanitizeFluidPickScope),
			FieldSanitizer.of("cutThroughItems", this::sanitizeCutThroughItems),
			FieldSanitizer.of("cutThroughPlants", this::sanitizeCutThroughPlants),
			FieldSanitizer.of("elytraFireworkCooldownTicks", this::sanitizeElytraFireworkTicks),
			FieldSanitizer.of("blockOutlineWidth", this::sanitizeBlockOutlineWidth),
			FieldSanitizer.of("blockOutlinePalette", this::sanitizeBlockOutlinePalette),
			FieldSanitizer.of("blockOutlineColorMode", this::sanitizeBlockOutlineColorMode),
			FieldSanitizer.of("blockOutlineAnimationSpeed", this::sanitizeBlockOutlineAnimationSpeed),
			FieldSanitizer.of("piglinGoldArmorSwapMode", this::sanitizePiglinArmorSwapMode),
			FieldSanitizer.of("itemGroupsSort", this::sanitizeItemGroupsSort),
			FieldSanitizer.of("locatorBarHeadScale", this::sanitizeLocatorHeadScale),
			FieldSanitizer.of("locatorBarHeadBorder", this::sanitizeLocatorHeadBorder),
			FieldSanitizer.of("locatorBarHeadBorderThickness", this::sanitizeLocatorHeadBorderThickness),
			FieldSanitizer.of("locatorBarHeadBorderColor", this::sanitizeLocatorBorderColor),
			FieldSanitizer.of("locatorBarHeadNames", this::sanitizeLocatorHeadNames),
			FieldSanitizer.of("locatorBarHeadFilter", this::sanitizeLocatorHeadFilter),
			FieldSanitizer.of("locatorBarCompassColor", this::sanitizeLocatorCompassColor),
			FieldSanitizer.of("entityHighlightStyle", this::sanitizeEntityHighlightStyle),
			FieldSanitizer.of("entityHighlightBlinkPeriod", this::sanitizeEntityHighlightBlinkPeriod),
			FieldSanitizer.of("entityHighlightColor", this::sanitizeEntityHighlightColor),
			FieldSanitizer.of("cameraMotionIntensity", this::sanitizeCameraMotionIntensity),
			FieldSanitizer.of("statusEffectDurationLowThreshold", this::sanitizeStatusDurationThreshold),
			FieldSanitizer.of("statusEffectDurationColorMode", this::sanitizeStatusDurationColorMode));
		// 启动期校验：新增数值/枚举字段忘记登记校验器时直接失败。
		assertSanitizerCoverage(fieldNamesOf(this.sanitizers));
	}

	// 复制物体 ID：开启后用“复制目标 ID”快捷键对着方块、实体或容器内悬停的物品复制其 ID。
	public volatile boolean copyItemId = false;
	// 复制物体 ID 的切换快捷键，以及真正执行复制的快捷键（子设置）。
	public volatile String hotkeyCopyItemId = "";
	public volatile String hotkeyCopyTargetId = "";

	// 复制格式：plain（纯 ID）/ typed（带 block/entity/item 前缀）/ counted（带数量）。
	public volatile String copyItemIdFormat = TweakifyModConfig.COPY_ID_FORMAT_PLAIN;

	// 复制的 ID 是否保留命名空间前缀：关闭后 minecraft:stone 复制为 stone。
	public volatile boolean copyItemIdNamespace = true;

	// 复制后自动关闭容器界面。
	public volatile boolean copyItemIdCloseContainer = false;

	// 复制后在聊天栏显示提示。
	public volatile boolean copyItemIdChatMessage = true;

	// 是否拦截“主手物品用完、右键还没松开时副手自动接手”的原版回退行为。
	// true = 开启拦截（模组功能生效），false = 保持原版行为。
	public volatile boolean blockOffhandFallback = true;

	// 仅当 blockOffhandFallback 为 true 时生效：命中方块时是否也取消本次使用。
	// 默认 false：放行门、拉杆等可交互方块的交互，但仍拦截副手方块物品的放置，
	// 保证主手用完瞬间副手不会把方块放出去（副手回推效果不丢）。
	// true = 所有方块交互一并拦截。
	public volatile boolean interceptBlockInteraction = false;

	// 仅当 blockOffhandFallback 为 true 时生效：命中实体时是否也取消本次使用。
	// 默认 false（更保守）：保留实体交互手感。
	// true = 实体交互也拦截。
	public volatile boolean interceptEntityInteraction = false;

	// 左键交互冷却：一次左键只挖一个方块（按住左键连续挖掘被禁用，需松开再按）。
	// true = 开启，false = 关闭。
	public volatile boolean miningCooldown = false;

	// 攻击实体也受“一次左键一次”限制（与挖方块共用按住周期）。
	public volatile boolean miningCooldownAttacks = false;

	// 右键交互冷却总开关（主闸）：关闭时主/副手使用冷却全部失效。
	public volatile boolean useCooldown = true;

	// 主手使用冷却：总开关开启时，主手物品一次右键只使用一次。
	public volatile boolean mainHandUseCooldown = false;

	// 副手使用冷却：总开关开启时，副手物品一次右键只使用一次。
	public volatile boolean offhandUseCooldown = false;

	// 移除飞行漂移：创造/旁观飞行时松开方向键立即停止水平移动，
	// 消除原版空气摩擦带来的惯性滑行。
	public volatile boolean disableFlightDrift = false;

	// 垂直飞行影响：开启移除飞行漂移时，松开跳跃/潜行键后也立即停止升降。
	public volatile boolean flightDriftVertical = false;

	// 禁用维度传送：总开关，阻止实体通过传送门切换维度。
	public volatile boolean disableDimensionTeleport = false;

	// 只拦截玩家的维度传送，其他实体仍可正常穿过传送门。
	public volatile boolean dimensionTeleportPlayersOnly = false;

	// 地狱门传送延迟：调整玩家通过下界传送门的等待时间。
	public volatile boolean netherPortalTeleportDelay = false;

	// 子开关：生存/冒险模式玩家进入地狱门立即传送（与创造模式一致）。
	public volatile boolean netherPortalSurvivalInstantTeleport = false;

	// 子开关：创造模式玩家进入地狱门也等待生存模式的延迟，不再秒传。
	public volatile boolean netherPortalCreativeDelayedTeleport = false;

	// 禁用地狱门传送：总开关开启时，阻止实体通过下界传送门。
	public volatile boolean disableNetherPortalTeleport = true;

	// 禁用末地门传送：总开关开启时，阻止实体通过要塞末地传送门。
	public volatile boolean disableEndPortalTeleport = true;

	// 禁用末地折跃门传送：总开关开启时，阻止实体通过末地折跃门。
	public volatile boolean disableEndGatewayTeleport = true;

	// 高亮单层基岩：描出周围上下都不是基岩的基岩（下界顶棚的可击穿薄弱点）。
	public volatile boolean highlightSingleLayerBedrock = false;

	// 单层基岩单次最多显示的标记数量（超出时保留最近的）。
	public volatile int highlightSingleLayerBedrockMaxMarkers = TweakifyModConfig.DEFAULT_BEDROCK_MARKERS;

	// 扫描半径（8 ~ 32 格）与标记颜色（gold / white / red / green / blue / cyan）。
	public volatile int highlightSingleLayerBedrockScanRadius = TweakifyModConfig.DEFAULT_BEDROCK_SCAN_RADIUS;
	public volatile String highlightSingleLayerBedrockColor = TweakifyModConfig.BEDROCK_COLOR_GOLD;

	// 锹单层铲雪：手持锹破坏多层雪层时一次只减少一层，而非整块移除。
	public volatile boolean disableShovelSnowFullBreak = false;

	// 铲雪时掉落雪球。
	public volatile boolean shovelSnowDropSnowball = true;

	// 铲雪时消耗锹的耐久。
	public volatile boolean shovelSnowConsumeDurability = true;

	// 相机运动：参考 CameraOverhaul 的转向/横移/俯仰/静止晃动与屏幕震动。
	public volatile boolean cameraMotion = false;
	public volatile String hotkeyCameraMotion = "";
	public volatile int cameraMotionIntensity = TweakifyModConfig.DEFAULT_CAMERA_MOTION_INTENSITY;
	public volatile boolean cameraMotionTurningRoll = true;
	public volatile boolean cameraMotionStrafingRoll = true;
	public volatile boolean cameraMotionForwardPitch = true;
	public volatile boolean cameraMotionVerticalPitch = true;
	public volatile boolean cameraMotionIdleSway = true;
	public volatile boolean cameraMotionScreenShake = true;
	public volatile boolean cameraMotionMouseSmoothing = false;
	public volatile boolean cameraMotionFirstPerson = false;
	public volatile boolean cameraMotionThirdPerson = true;

	// 物品分组：同一分类里把楼梯、台阶、羊毛等成组物品折叠成组图标。
	public volatile boolean itemGroups = false;
	public volatile String hotkeyItemGroups = "";
	public volatile String itemGroupsSort = TweakifyModConfig.ITEM_GROUP_SORT_DEFAULT;

	// 展开时图标改为轮播组内物品（与参考实现同名选项一致）。
	public volatile boolean itemGroupsShowItems = false;

	// 是否启用内置默认组；关闭后只用自定义组。
	public volatile boolean itemGroupsAddAll = true;

	// 自定义组：每行 tabId|组名|包含(逗号分隔)|排除|等价；至少需要前三个字段。
	volatile List<String> customItemGroups = new ArrayList<>();

	// 定位栏优化：把定位栏上的彩色圆点换成本服玩家的头像（参考 locator-heads）。
	public volatile boolean locatorBarHeads = false;
	public volatile String hotkeyLocatorBarHeads = "";
	// 经验条与定位栏同时显示（参考 alwaysShowXP）：强制优先经验条，并把定位栏标记叠在它上面。
	public volatile boolean locatorBarAlwaysShowXp = false;
	public volatile int locatorBarHeadScale = TweakifyModConfig.DEFAULT_LOCATOR_HEAD_SIZE;
	public volatile boolean locatorBarHeadHatLayer = true;

	// 描边：样式（关/固定色/队伍色）、粗细（0.3/0.5/1.0/2.0 四档）与固定颜色。
	public volatile String locatorBarHeadBorder = TweakifyModConfig.LOCATOR_HEAD_BORDER_OFF;
	public volatile String locatorBarHeadBorderThickness = TweakifyModConfig.DEFAULT_LOCATOR_HEAD_BORDER_THICKNESS;
	public volatile String locatorBarHeadBorderColor = TweakifyModConfig.LOCATOR_COLOR_WHITE;

	// 玩家名字时机与玩家过滤（与参考实现一致：包含/排除各一份名单，名单为空时 INCLUDE 全部隐藏、EXCLUDE 全部显示）。
	public volatile String locatorBarHeadNames = TweakifyModConfig.LOCATOR_HEAD_NAMES_NEVER;
	public volatile String locatorBarHeadFilter = TweakifyModConfig.LOCATOR_HEAD_FILTER_ALL;
	volatile List<String> locatorBarHeadIncludedPlayers = new ArrayList<>();
	volatile List<String> locatorBarHeadExcludedPlayers = new ArrayList<>();

	// 只显示当前已加载的玩家（附近）。
	public volatile boolean locatorBarHeadNearbyOnly = false;

	// 指南针覆盖层：开关、颜色、文字阴影与坐标标记（+X/-X/+Z/-Z）。
	public volatile boolean locatorBarCompass = false;
	public volatile String locatorBarCompassColor = TweakifyModConfig.LOCATOR_COLOR_WHITE;
	public volatile boolean locatorBarCompassShadow = true;
	public volatile boolean locatorBarCompassCoordinates = false;

	// 关闭指定生物生成：列表中的生物不会通过自然/结构/巡逻/增援/刷怪笼等世界生成出现。
	// 玩家主动生成默认放行；关闭 disabledSpawnAllowPlayerSpawn 后刷怪蛋、命令、繁殖等也会被拦截。
	public volatile boolean disableSpecifiedMobSpawn = false;
	volatile List<String> disabledSpawnEntities = new ArrayList<>();
	public volatile boolean disabledSpawnAllowPlayerSpawn = true;

	// 和平模式怪物：允许列表中的怪物在和平难度下生成/保留，并可设置为不攻击玩家。
	public volatile boolean peacefulModeMonsters = false;
	volatile List<String> peacefulModeMonsterEntities = new ArrayList<>();
	public volatile boolean peacefulModeMonsterSpawn = true;
	public volatile boolean peacefulModeMonsterNoAttack = true;

	// 刷怪蛋投掷：像鸡蛋/雪球一样投掷刷怪蛋，命中后生成生物。
	public volatile boolean throwSpawnEgg = false;
	public volatile String hotkeyThrowSpawnEgg = "";

	// 投掷速度百分比（50 ~ 200，100 = 原版鸡蛋速度）。
	public volatile int throwSpawnEggVelocity = TweakifyModConfig.DEFAULT_SPAWN_EGG_VELOCITY;

	// 潜行时保持原版放置，不改成投掷（与“剑穿草打生物”的潜行放行一致）。
	public volatile boolean throwSpawnEggSneakBypass = false;

	// 鞘翅烟花使用限制：滑翔中使用烟花后，等烟花加速结束才能再次使用。
	public volatile boolean elytraFireworkCooldown = false;

	// 两次烟花之间的最短间隔（tick）；0 = 等本次加速结束（原行为）。
	public volatile int elytraFireworkCooldownTicks = TweakifyModConfig.MIN_ELYTRA_FIREWORK_TICKS;

	// 聊天框文字框选：在聊天界面拖动鼠标选择消息文字，Ctrl+C 复制。
	public volatile boolean chatTextSelection = false;

	// 框选颜色：blue / green / orange / purple / gray。
	public volatile String chatSelectionColor = TweakifyModConfig.CHAT_SELECTION_BLUE;

	// 框选背景透明度百分比（10 ~ 100）。
	public volatile int chatSelectionOpacity = TweakifyModConfig.DEFAULT_CHAT_SELECTION_OPACITY;

	// 松开鼠标时自动复制选中文字。
	public volatile boolean chatSelectionAutoCopy = false;

	// 防猪灵装备切换：附近有猪灵时换上一件金制护甲，猪灵离开后换回。
	public volatile boolean piglinGoldArmorSwap = false;

	// 切换模式：auto（有哪个金制就换哪个）/ helmet / chestplate / leggings / boots。
	public volatile String piglinGoldArmorSwapMode = TweakifyModConfig.PIGLIN_ARMOR_SWAP_AUTO;

	// 聊天框内容记忆：关闭聊天框后保留未发送的输入内容，再次打开时恢复。
	public volatile boolean chatInputMemory = false;

	// 以 / 开头的命令草稿不记忆（避免把未发送的命令带回来误执行）。
	public volatile boolean chatInputMemoryIgnoreCommands = false;

	// 生物攻击范围显示：以生物为中心画半透明蓝色范围，列表为空时作用于全部生物与玩家。
	public volatile boolean attackRangeDisplay = false;
	public volatile List<String> attackRangeEntities = new ArrayList<>();

	// 是否显示本地玩家自己的攻击范围（默认关闭，避免球体包住自身）。
	public volatile boolean attackRangeIncludeSelf = false;

	// 只显示敌对生物（保持原版的 Monster 判定）。
	public volatile boolean attackRangeHostileOnly = false;

	// 是否连隐身实体也一并显示（默认关闭，与原版行为一致）。
	public volatile boolean attackRangeIncludeInvisible = false;

	// 收集范围（格）与单次最多显示的目标数量。
	public volatile int attackRangeMaxDistance = TweakifyModConfig.DEFAULT_ATTACK_RANGE_DISTANCE;
	public volatile int attackRangeMaxTargets = TweakifyModConfig.DEFAULT_ATTACK_RANGE_TARGETS;

	// 攻击范围显示方式：flat（脚下平面圆环）/ sphere（经纬线框球体）。
	public volatile String attackRangeDisplayMode = TweakifyModConfig.ATTACK_RANGE_DISPLAY_FLAT;

	// 攻击范围渲染方式：wireframe（蓝色线框，性能更好）/ filled（半透明实心，可能掉帧）。
	public volatile String attackRangeRenderMode = TweakifyModConfig.ATTACK_RANGE_RENDER_WIREFRAME;

	// 攻击范围颜色调色板：只决定 RGB，透明度沿用各模式的固定档位。
	public volatile String attackRangePalette = TweakifyModConfig.ATTACK_RANGE_PALETTE_BLUE;

	// 自定义方块轮廓：总开关、线宽、调色板与彩色模式。
	public volatile boolean customBlockOutline = false;
	public volatile double blockOutlineWidth = TweakifyModConfig.DEFAULT_BLOCK_OUTLINE_WIDTH;
	public volatile int blockOutlineAnimationSpeed = TweakifyModConfig.DEFAULT_BLOCK_OUTLINE_ANIMATION_SPEED;
	public volatile String blockOutlinePalette = TweakifyModConfig.BLOCK_OUTLINE_PALETTE_WHITE;
	public volatile String blockOutlineColorMode = TweakifyModConfig.BLOCK_OUTLINE_MODE_PALETTE;

	// 更好的放置覆盖：手持方块右键时可直接替换花、高草等植物（仅单人/局域网生效）。
	public volatile boolean betterBlockPlacement = false;

	// 潜行时保持原版：不覆盖植物，按原版流程先打掉植物。
	public volatile boolean betterBlockPlacementSneakBypass = true;

	// 可覆盖的植物范围：flowers（仅花草）/ crops（含作物）/ all（全部植物）。
	public volatile String betterBlockPlacementMode = TweakifyModConfig.BLOCK_PLACEMENT_FLOWERS;

	// 更好的骨粉催熟：让骨粉像基岩版一样作用于甘蔗与一格高的花。
	public volatile boolean betterBoneMeal = false;

	// 子开关：骨粉把整株甘蔗一次催到最高 3 格，只消耗 1 个骨粉。
	public volatile boolean boneMealSugarCane = true;

	// 子开关：骨粉在一格高的花周围长出一片相同的花（凋零玫瑰除外）。
	public volatile boolean boneMealFlowers = true;

	// 剑穿草打生物：主手持剑时跳过花草，直接选中后方的生物。
	public volatile boolean swordCutThroughPlants = false;

	// 生效物品范围：sword（仅剑）/ sword_and_axe（剑与斧）/ any（任意物品）。
	public volatile String cutThroughItems = TweakifyModConfig.CUT_THROUGH_ITEMS_SWORD;

	// 植物范围：flowers（花草）/ crops（含作物）/ all（全部无碰撞植物）。
	public volatile String cutThroughPlants = TweakifyModConfig.CUT_THROUGH_PLANTS_ALL;

	// 潜行时恢复原版：左键先把植物打掉，而不是穿过去。
	public volatile boolean cutThroughSneakBypass = true;

	// 中键获得液体桶：准星指向水/岩浆时，中键得到对应的液体桶（仅创造模式）。
	public volatile boolean pickFluidBucket = false;

	// 液体范围：水与岩浆 / 仅水 / 仅岩浆。
	public volatile String pickFluidBucketFluids = TweakifyModConfig.FLUID_PICK_WATER_AND_LAVA;

	// 生存模式中键取物：准星指向方块/液体/实体时，背包里有对应物品就切换到主手。
	public volatile boolean survivalPickBlock = false;

	// 生存取物也处理液体（水/岩浆 → 已有的桶）；关闭后退回原版，只取方块与实体。
	public volatile boolean survivalPickBlockIncludeFluids = true;

	// 便捷焚烧：背包/容器里拿着物品，右键点击岩浆桶或打火石即焚烧该物品。
	public volatile boolean quickBurnItem = false;

	// 需要潜行才焚烧：避免在整理背包时误触销毁物品。
	public volatile boolean quickBurnItemRequireSneak = false;

	// 如履平地：自动跨上 1 格及以上的方块（像马一样）；步高由子设置控制。
	public volatile boolean autoStepUp = false;

	// 跨步高度：one_block（1.0）/ one_and_half（1.5）/ two_blocks（2.0）。
	public volatile String autoStepUpHeight = TweakifyModConfig.AUTO_STEP_UP_HEIGHT_ONE_BLOCK;

	// 更好的攀爬：攀爬方块相关的移动优化（子开关见下）。
	public volatile boolean betterClimbing = false;

	// 子开关：向下看时加速下降。
	public volatile boolean climbingFastDescend = true;

	// 子开关：攀爬上升更快。
	public volatile boolean climbingFasterAscend = true;

	// 子开关：攀爬方块中可从地面起跳。
	public volatile boolean climbingJumpFromGround = true;

	// 子开关：攀爬时无移动输入不触发碰撞减速。
	public volatile boolean climbingCancelCollision = true;

	// 主动爬行：按住绑定的爬行键时强制进入爬行姿态。
	public volatile boolean forceCrawl = false;

	// 屏障可见：开启后在周围屏障方块中心显示红色标记，未手持屏障物品也能看到。
	public volatile boolean barrierVisibility = false;

	// 单次扫描最多保留的屏障标记数量（超出时保留离玩家最近的）。
	public volatile int barrierVisibilityMaxMarkers = TweakifyModConfig.DEFAULT_BARRIER_MARKERS;

	// 扫描半径（8 ~ 32 格）与同一方块两次标记粒子的间隔（20 ~ 200 tick）。
	public volatile int barrierVisibilityScanRadius = TweakifyModConfig.DEFAULT_BARRIER_SCAN_RADIUS;
	public volatile int barrierVisibilityParticleInterval = TweakifyModConfig.DEFAULT_BARRIER_PARTICLE_INTERVAL;

	// 装备耐久显示：在物品栏耐久条上方叠加耐久文字，满耐久时不显示。
	public volatile boolean durabilityOverlay = false;

	// 耐久文字内容：percent（耐久百分比）/ value（剩余耐久数值）。
	public volatile String durabilityDisplayMode = TweakifyModConfig.DURABILITY_DISPLAY_PERCENT;

	// 耐久文字配色：auto（按剩余耐久分档）/ white / yellow / red。
	public volatile String durabilityOverlayColorMode = TweakifyModConfig.DURABILITY_COLOR_MODE_AUTO;

	// 仅在 HUD 快捷栏显示耐久文字（打开任意界面时不显示）。
	public volatile boolean durabilityOverlayHotbarOnly = false;

	// 只在剩余耐久不高于该百分比时显示（0 = 不限制）。
	public volatile int durabilityOverlayLowThreshold = TweakifyModConfig.MIN_DURABILITY_THRESHOLD;

	// 药水效果时间显示：在 HUD 的效果图标下方显示剩余时间。
	public volatile boolean statusEffectDurationDisplay = false;

	// 时间格式固定为秒（不使用分:秒 / h / d 缩写）。
	public volatile boolean statusEffectDurationAlwaysSeconds = false;

	// 只在剩余时间不高于该秒数时显示（0 = 不过滤）。
	public volatile int statusEffectDurationLowThreshold = TweakifyModConfig.DEFAULT_STATUS_DURATION_THRESHOLD;

	// 文字配色：auto（按剩余时间分档）/ white / yellow / red。
	public volatile String statusEffectDurationColorMode = TweakifyModConfig.STATUS_COLOR_AUTO;

	// 各功能根开关的切换快捷键，默认无快捷键（空字符串）。
	// 功能标签只给根功能提供快捷键，子设置不占快捷键栏。
	public volatile String hotkeyMiningCooldown = "";
	public volatile String hotkeyUseCooldown = "";
	public volatile String hotkeyDisableFlightDrift = "";
	public volatile String hotkeyHighlightSingleLayerBedrock = "";
	public volatile String hotkeyDisableShovelSnowFullBreak = "";
	public volatile String hotkeyBarrierVisibility = "";
	public volatile String hotkeyDurabilityOverlay = "";
	public volatile String hotkeyElytraFireworkCooldown = "";
	public volatile String hotkeyAttackRangeDisplay = "";
	public volatile String hotkeyDisableDimensionTeleport = "";
	public volatile String hotkeyChatTextSelection = "";
	public volatile String hotkeySwordCutThroughPlants = "";
	public volatile String hotkeyPickFluidBucket = "";
	public volatile String hotkeySurvivalPickBlock = "";
	public volatile String hotkeyQuickBurnItem = "";
	public volatile String hotkeyCustomBlockOutline = "";
	public volatile String hotkeyBetterBlockPlacement = "";
	public volatile String hotkeyAutoStepUp = "";
	public volatile String hotkeyBetterClimbing = "";
	public volatile String hotkeyForceCrawl = "";
	// 主动爬行的按住键：未绑定时不应触发爬行，因此使用 allowEmpty = false 的按键设置。
	public volatile String hotkeyForceCrawlKey = "";
	public volatile String hotkeyBetterBoneMeal = "";
	public volatile String hotkeyPiglinGoldArmorSwap = "";
	public volatile String hotkeyChatInputMemory = "";
	public volatile String hotkeyStatusEffectDurationDisplay = "";
	public volatile String hotkeyNetherPortalTeleportDelay = "";
	public volatile String hotkeyDisableSpecifiedMobSpawn = "";
	public volatile String hotkeyPeacefulModeMonsters = "";

	// 主手使用冷却的物品 ID 列表，默认是所有可交互物品。
	volatile List<String> mainHandUseCooldownItems = new ArrayList<>();

	// 副手使用冷却的物品 ID 列表，默认是所有可交互物品。
	volatile List<String> offhandUseCooldownItems = new ArrayList<>();

	// 受限的副手物品 ID 列表（namespace:path）。默认是所有光源物品。
	// 仅当副手物品命中该列表时，主手用完瞬间才会拦截副手接手；可手动增删。
	volatile List<String> restrictedOffhandItems = new ArrayList<>();

	// 实体高亮总开关：开启后按当前启用的预设列表显示发光描边。
	public volatile boolean entityHighlight = false;
	public volatile String hotkeyEntityHighlight = "";

	// 描边样式：outline（常亮，默认）/ blink（按周期闪烁）。
	public volatile String entityHighlightStyle = TweakifyModConfig.ENTITY_HIGHLIGHT_STYLE_OUTLINE;

	// 闪烁周期（tick，10 ~ 200）：一半时间显示、一半时间隐藏。
	public volatile int entityHighlightBlinkPeriod = TweakifyModConfig.DEFAULT_ENTITY_HIGHLIGHT_BLINK_PERIOD;

	// 描边颜色：vanilla（原版队伍色，默认）或固定调色板。
	public volatile String entityHighlightColor = TweakifyModConfig.ENTITY_HIGHLIGHT_COLOR_VANILLA;

	// 三个互斥的高亮预设：开启一个会自动关闭其他两个。
	public volatile boolean highlightPreset1 = false;
	public volatile boolean highlightPreset2 = false;
	public volatile boolean highlightPreset3 = false;

	// 各预设需要高亮的实体 ID 列表（如 minecraft:item 表示掉落物）。
	public volatile List<String> highlightPreset1Entities = new ArrayList<>();
	public volatile List<String> highlightPreset2Entities = new ArrayList<>();
	public volatile List<String> highlightPreset3Entities = new ArrayList<>();

	// 传送门黑名单：对应门开关开启时，命中名单的实体不允许通过该门传送；名单为空则拦截全部实体。
	public volatile List<String> netherPortalTeleportBlacklist = new ArrayList<>();
	public volatile List<String> endPortalTeleportBlacklist = new ArrayList<>();
	public volatile List<String> endGatewayTeleportBlacklist = new ArrayList<>();

	@Override
	boolean sanitize() {
		return runSanitizers(this.sanitizers);
	}

	@Override
	List<String> sanitizedFieldNames() {
		return fieldNamesOf(this.sanitizers);
	}

	// 修正耐久文字低阈值。
	private boolean sanitizeDurabilityThreshold() {
		int clamped = Math.max(TweakifyModConfig.MIN_DURABILITY_THRESHOLD,
			Math.min(TweakifyModConfig.MAX_DURABILITY_THRESHOLD, durabilityOverlayLowThreshold));
		if (clamped == durabilityOverlayLowThreshold) {
			return false;
		}
		LOGGER.warn("Clamping durabilityOverlayLowThreshold from {} to {}",
			durabilityOverlayLowThreshold, clamped);
		durabilityOverlayLowThreshold = clamped;
		return true;
	}

	// 修正不合法的放置覆盖模式。
	private boolean sanitizeBlockPlacementMode() {
		if (TweakifyModConfig.isSupportedValue(
			TweakifyModConfig.SUPPORTED_BLOCK_PLACEMENT_MODES, betterBlockPlacementMode)) {
			return false;
		}
		LOGGER.warn("Ignoring unsupported block placement mode: {}", betterBlockPlacementMode);
		betterBlockPlacementMode = TweakifyModConfig.BLOCK_PLACEMENT_FLOWERS;
		return true;
	}

	// 修正不合法的复制格式。
	private boolean sanitizeCopyIdFormat() {
		if (TweakifyModConfig.isSupportedValue(TweakifyModConfig.SUPPORTED_COPY_ID_FORMATS, copyItemIdFormat)) {
			return false;
		}
		LOGGER.warn("Ignoring unsupported copy ID format: {}", copyItemIdFormat);
		copyItemIdFormat = TweakifyModConfig.COPY_ID_FORMAT_PLAIN;
		return true;
	}

	// 修正不合法的框选颜色。
	private boolean sanitizeChatSelectionColor() {
		if (TweakifyModConfig.isSupportedValue(
			TweakifyModConfig.SUPPORTED_CHAT_SELECTION_COLORS, chatSelectionColor)) {
			return false;
		}
		LOGGER.warn("Ignoring unsupported chat selection color: {}", chatSelectionColor);
		chatSelectionColor = TweakifyModConfig.CHAT_SELECTION_BLUE;
		return true;
	}

	// 修正不合法的耐久显示内容，回退到百分比。
	private boolean sanitizeDurabilityDisplayMode() {
		if (TweakifyModConfig.isSupportedValue(
			TweakifyModConfig.SUPPORTED_DURABILITY_DISPLAYS, durabilityDisplayMode)) {
			return false;
		}
		LOGGER.warn("Ignoring unsupported durability display mode in config: {}", durabilityDisplayMode);
		durabilityDisplayMode = TweakifyModConfig.DURABILITY_DISPLAY_PERCENT;
		return true;
	}

	// 修正不合法的耐久配色，回退到自动分档。
	private boolean sanitizeDurabilityColorMode() {
		if (TweakifyModConfig.isSupportedValue(
			TweakifyModConfig.SUPPORTED_DURABILITY_COLOR_MODES, durabilityOverlayColorMode)) {
			return false;
		}
		LOGGER.warn("Ignoring unsupported durability color mode in config: {}", durabilityOverlayColorMode);
		durabilityOverlayColorMode = TweakifyModConfig.DURABILITY_COLOR_MODE_AUTO;
		return true;
	}

	// 修正不合法的攻击范围显示方式，回退到平面圆环。
	private boolean sanitizeAttackRangeDisplayMode() {
		if (TweakifyModConfig.isSupportedValue(
			TweakifyModConfig.SUPPORTED_ATTACK_RANGE_DISPLAY_MODES, attackRangeDisplayMode)) {
			return false;
		}
		LOGGER.warn("Ignoring unsupported attack range display mode in config: {}", attackRangeDisplayMode);
		attackRangeDisplayMode = TweakifyModConfig.ATTACK_RANGE_DISPLAY_FLAT;
		return true;
	}

	// 修正攻击范围的收集距离与目标数量，手工改坏配置时夹回允许范围。
	private boolean sanitizeAttackRangeLimits() {
		int distance = Math.max(TweakifyModConfig.MIN_ATTACK_RANGE_DISTANCE,
			Math.min(TweakifyModConfig.MAX_ATTACK_RANGE_DISTANCE, attackRangeMaxDistance));
		int targets = Math.max(TweakifyModConfig.MIN_ATTACK_RANGE_TARGETS,
			Math.min(TweakifyModConfig.MAX_ATTACK_RANGE_TARGETS, attackRangeMaxTargets));
		if (distance == attackRangeMaxDistance && targets == attackRangeMaxTargets) {
			return false;
		}
		LOGGER.warn("Clamping attack range limits: distance {} -> {}, targets {} -> {}",
			attackRangeMaxDistance, distance, attackRangeMaxTargets, targets);
		attackRangeMaxDistance = distance;
		attackRangeMaxTargets = targets;
		return true;
	}

	// 修正不合法的攻击范围渲染方式，回退到线框。
	private boolean sanitizeAttackRangeRenderMode() {
		if (TweakifyModConfig.isSupportedValue(
			TweakifyModConfig.SUPPORTED_ATTACK_RANGE_RENDER_MODES, attackRangeRenderMode)) {
			return false;
		}
		LOGGER.warn("Ignoring unsupported attack range render mode in config: {}", attackRangeRenderMode);
		attackRangeRenderMode = TweakifyModConfig.ATTACK_RANGE_RENDER_WIREFRAME;
		return true;
	}

	// 修正单层基岩标记数量，手工改坏配置时夹回允许范围。
	private boolean sanitizeBedrockMarkerLimit() {
		int clamped = Math.max(TweakifyModConfig.MIN_BEDROCK_MARKERS,
			Math.min(TweakifyModConfig.MAX_BEDROCK_MARKERS, highlightSingleLayerBedrockMaxMarkers));
		if (clamped == highlightSingleLayerBedrockMaxMarkers) {
			return false;
		}
		LOGGER.warn("Clamping highlightSingleLayerBedrockMaxMarkers from {} to {}",
			highlightSingleLayerBedrockMaxMarkers, clamped);
		highlightSingleLayerBedrockMaxMarkers = clamped;
		return true;
	}

	// 修正基岩扫描半径，手工改坏配置时夹回允许范围。
	private boolean sanitizeBedrockScanRadius() {
		int clamped = Math.max(TweakifyModConfig.MIN_BEDROCK_SCAN_RADIUS,
			Math.min(TweakifyModConfig.MAX_BEDROCK_SCAN_RADIUS, highlightSingleLayerBedrockScanRadius));
		if (clamped == highlightSingleLayerBedrockScanRadius) {
			return false;
		}
		LOGGER.warn("Clamping highlightSingleLayerBedrockScanRadius from {} to {}",
			highlightSingleLayerBedrockScanRadius, clamped);
		highlightSingleLayerBedrockScanRadius = clamped;
		return true;
	}

	// 修正基岩标记颜色，回退到金色。
	private boolean sanitizeBedrockMarkerColor() {
		if (TweakifyModConfig.isSupportedValue(
			TweakifyModConfig.SUPPORTED_BEDROCK_COLORS, highlightSingleLayerBedrockColor)) {
			return false;
		}
		LOGGER.warn("Ignoring unsupported bedrock marker color: {}", highlightSingleLayerBedrockColor);
		highlightSingleLayerBedrockColor = TweakifyModConfig.BEDROCK_COLOR_GOLD;
		return true;
	}

	// 修正攻击范围调色板，回退到蓝色。
	private boolean sanitizeAttackRangePalette() {
		if (TweakifyModConfig.isSupportedValue(
			TweakifyModConfig.SUPPORTED_ATTACK_RANGE_PALETTES, attackRangePalette)) {
			return false;
		}
		LOGGER.warn("Ignoring unsupported attack range palette: {}", attackRangePalette);
		attackRangePalette = TweakifyModConfig.ATTACK_RANGE_PALETTE_BLUE;
		return true;
	}

	// 修正聊天框选透明度。
	private boolean sanitizeChatSelectionOpacity() {
		int clamped = Math.max(TweakifyModConfig.MIN_CHAT_SELECTION_OPACITY,
			Math.min(TweakifyModConfig.MAX_CHAT_SELECTION_OPACITY, chatSelectionOpacity));
		if (clamped == chatSelectionOpacity) {
			return false;
		}
		LOGGER.warn("Clamping chatSelectionOpacity from {} to {}", chatSelectionOpacity, clamped);
		chatSelectionOpacity = clamped;
		return true;
	}

	// 修正药水时间的低时长过滤阈值。
	private boolean sanitizeStatusDurationThreshold() {
		int clamped = Math.max(TweakifyModConfig.MIN_STATUS_DURATION_THRESHOLD,
			Math.min(TweakifyModConfig.MAX_STATUS_DURATION_THRESHOLD, statusEffectDurationLowThreshold));
		if (clamped == statusEffectDurationLowThreshold) {
			return false;
		}
		LOGGER.warn("Clamping statusEffectDurationLowThreshold from {} to {}",
			statusEffectDurationLowThreshold, clamped);
		statusEffectDurationLowThreshold = clamped;
		return true;
	}

	// 修正药水时间文字配色，回退到自动分档。
	private boolean sanitizeStatusDurationColorMode() {
		if (TweakifyModConfig.isSupportedValue(
			TweakifyModConfig.SUPPORTED_STATUS_COLOR_MODES, statusEffectDurationColorMode)) {
			return false;
		}
		LOGGER.warn("Ignoring unsupported status duration color mode: {}", statusEffectDurationColorMode);
		statusEffectDurationColorMode = TweakifyModConfig.STATUS_COLOR_AUTO;
		return true;
	}

	// 修正屏障标记数量，手工改坏配置时夹回允许范围。
	private boolean sanitizeBarrierMarkerLimit() {
		int clamped = Math.max(TweakifyModConfig.MIN_BARRIER_MARKERS,
			Math.min(TweakifyModConfig.MAX_BARRIER_MARKERS, barrierVisibilityMaxMarkers));
		if (clamped == barrierVisibilityMaxMarkers) {
			return false;
		}
		LOGGER.warn("Clamping barrierVisibilityMaxMarkers from {} to {}",
			barrierVisibilityMaxMarkers, clamped);
		barrierVisibilityMaxMarkers = clamped;
		return true;
	}

	// 修正屏障扫描半径与标记间隔，手工改坏配置时夹回允许范围。
	private boolean sanitizeBarrierScanAndInterval() {
		int radius = Math.max(TweakifyModConfig.MIN_BARRIER_SCAN_RADIUS,
			Math.min(TweakifyModConfig.MAX_BARRIER_SCAN_RADIUS, barrierVisibilityScanRadius));
		int interval = Math.max(TweakifyModConfig.MIN_BARRIER_PARTICLE_INTERVAL,
			Math.min(TweakifyModConfig.MAX_BARRIER_PARTICLE_INTERVAL, barrierVisibilityParticleInterval));
		if (radius == barrierVisibilityScanRadius
			&& interval == barrierVisibilityParticleInterval) {
			return false;
		}
		LOGGER.warn("Clamping barrier scan settings: radius {} -> {}, interval {} -> {}",
			barrierVisibilityScanRadius, radius, barrierVisibilityParticleInterval, interval);
		barrierVisibilityScanRadius = radius;
		barrierVisibilityParticleInterval = interval;
		return true;
	}

	// 修正不合法的跨步高度，回退到 1 格。
	private boolean sanitizeAutoStepUpHeight() {
		if (TweakifyModConfig.isSupportedValue(
			TweakifyModConfig.SUPPORTED_AUTO_STEP_UP_HEIGHTS, autoStepUpHeight)) {
			return false;
		}
		LOGGER.warn("Ignoring unsupported auto step-up height: {}", autoStepUpHeight);
		autoStepUpHeight = TweakifyModConfig.AUTO_STEP_UP_HEIGHT_ONE_BLOCK;
		return true;
	}

	// 修正刷怪蛋投掷速度，手工改坏配置时夹回允许范围。
	private boolean sanitizeSpawnEggVelocity() {
		int clamped = Math.max(TweakifyModConfig.MIN_SPAWN_EGG_VELOCITY,
			Math.min(TweakifyModConfig.MAX_SPAWN_EGG_VELOCITY, throwSpawnEggVelocity));
		if (clamped == throwSpawnEggVelocity) {
			return false;
		}
		LOGGER.warn("Clamping throwSpawnEggVelocity from {} to {}", throwSpawnEggVelocity, clamped);
		throwSpawnEggVelocity = clamped;
		return true;
	}

	// 修正中键取液体的范围，回退到水与岩浆。
	private boolean sanitizeFluidPickScope() {
		if (TweakifyModConfig.isSupportedValue(
			TweakifyModConfig.SUPPORTED_FLUID_PICK_SCOPES, pickFluidBucketFluids)) {
			return false;
		}
		LOGGER.warn("Ignoring unsupported fluid pick scope: {}", pickFluidBucketFluids);
		pickFluidBucketFluids = TweakifyModConfig.FLUID_PICK_WATER_AND_LAVA;
		return true;
	}

	// 修正剑穿草打生物的生效物品范围。
	private boolean sanitizeCutThroughItems() {
		if (TweakifyModConfig.isSupportedValue(TweakifyModConfig.SUPPORTED_CUT_THROUGH_ITEMS, cutThroughItems)) {
			return false;
		}
		LOGGER.warn("Ignoring unsupported cut through item scope: {}", cutThroughItems);
		cutThroughItems = TweakifyModConfig.CUT_THROUGH_ITEMS_SWORD;
		return true;
	}

	// 修正剑穿草打生物的植物范围。
	private boolean sanitizeCutThroughPlants() {
		if (TweakifyModConfig.isSupportedValue(TweakifyModConfig.SUPPORTED_CUT_THROUGH_PLANTS, cutThroughPlants)) {
			return false;
		}
		LOGGER.warn("Ignoring unsupported cut through plant scope: {}", cutThroughPlants);
		cutThroughPlants = TweakifyModConfig.CUT_THROUGH_PLANTS_ALL;
		return true;
	}

	// 修正鞘翅烟花最短间隔。
	private boolean sanitizeElytraFireworkTicks() {
		int clamped = Math.max(TweakifyModConfig.MIN_ELYTRA_FIREWORK_TICKS,
			Math.min(TweakifyModConfig.MAX_ELYTRA_FIREWORK_TICKS, elytraFireworkCooldownTicks));
		if (clamped == elytraFireworkCooldownTicks) {
			return false;
		}
		LOGGER.warn("Clamping elytraFireworkCooldownTicks from {} to {}", elytraFireworkCooldownTicks, clamped);
		elytraFireworkCooldownTicks = clamped;
		return true;
	}

	// 修正自定义轮廓线宽：统一走 0.5 步进与范围夹紧工具。
	private boolean sanitizeBlockOutlineWidth() {
		double clamped = TweakifyModConfig.clampBlockOutlineWidth(blockOutlineWidth);
		if (Double.compare(clamped, blockOutlineWidth) == 0) {
			return false;
		}
		LOGGER.warn("Correcting blockOutlineWidth from {} to {}", blockOutlineWidth, clamped);
		blockOutlineWidth = clamped;
		return true;
	}

	// 修正不合法的轮廓调色板。
	private boolean sanitizeBlockOutlinePalette() {
		if (TweakifyModConfig.isSupportedValue(
			TweakifyModConfig.SUPPORTED_BLOCK_OUTLINE_PALETTES, blockOutlinePalette)) {
			return false;
		}
		LOGGER.warn("Ignoring unsupported block outline palette: {}", blockOutlinePalette);
		blockOutlinePalette = TweakifyModConfig.BLOCK_OUTLINE_PALETTE_WHITE;
		return true;
	}

	// 修正不合法的轮廓彩色模式。
	private boolean sanitizeBlockOutlineColorMode() {
		if (TweakifyModConfig.isSupportedValue(
			TweakifyModConfig.SUPPORTED_BLOCK_OUTLINE_COLOR_MODES, blockOutlineColorMode)) {
			return false;
		}
		LOGGER.warn("Ignoring unsupported block outline color mode: {}", blockOutlineColorMode);
		blockOutlineColorMode = TweakifyModConfig.BLOCK_OUTLINE_MODE_PALETTE;
		return true;
	}

	// 修正彩虹/呼吸动画速度。
	private boolean sanitizeBlockOutlineAnimationSpeed() {
		int clamped = Math.max(TweakifyModConfig.MIN_BLOCK_OUTLINE_ANIMATION_SPEED,
			Math.min(TweakifyModConfig.MAX_BLOCK_OUTLINE_ANIMATION_SPEED, blockOutlineAnimationSpeed));
		if (clamped == blockOutlineAnimationSpeed) {
			return false;
		}
		LOGGER.warn("Clamping blockOutlineAnimationSpeed from {} to {}", blockOutlineAnimationSpeed, clamped);
		blockOutlineAnimationSpeed = clamped;
		return true;
	}

	// 修正不合法的猪灵装备切换模式。
	private boolean sanitizePiglinArmorSwapMode() {
		if (TweakifyModConfig.isSupportedValue(
			TweakifyModConfig.SUPPORTED_PIGLIN_ARMOR_SWAP_MODES, piglinGoldArmorSwapMode)) {
			return false;
		}
		LOGGER.warn("Ignoring unsupported piglin armor swap mode: {}", piglinGoldArmorSwapMode);
		piglinGoldArmorSwapMode = TweakifyModConfig.PIGLIN_ARMOR_SWAP_AUTO;
		return true;
	}

	// 修正物品分组的排序方式。
	private boolean sanitizeItemGroupsSort() {
		if (TweakifyModConfig.isSupportedValue(TweakifyModConfig.SUPPORTED_ITEM_GROUP_SORTS, itemGroupsSort)) {
			return false;
		}
		LOGGER.warn("Ignoring unsupported item group sort: {}", itemGroupsSort);
		itemGroupsSort = TweakifyModConfig.ITEM_GROUP_SORT_DEFAULT;
		return true;
	}

	// 修正定位栏头像尺寸，手工改坏配置时夹回允许范围。
	private boolean sanitizeLocatorHeadScale() {
		int clamped = Math.max(TweakifyModConfig.MIN_LOCATOR_HEAD_SIZE,
			Math.min(TweakifyModConfig.MAX_LOCATOR_HEAD_SIZE, locatorBarHeadScale));
		if (clamped == locatorBarHeadScale) {
			return false;
		}
		LOGGER.warn("Clamping locatorBarHeadScale from {} to {}", locatorBarHeadScale, clamped);
		locatorBarHeadScale = clamped;
		return true;
	}

	// 修正描边样式。
	private boolean sanitizeLocatorHeadBorder() {
		if (TweakifyModConfig.isSupportedValue(
			TweakifyModConfig.SUPPORTED_LOCATOR_HEAD_BORDERS, locatorBarHeadBorder)) {
			return false;
		}
		LOGGER.warn("Ignoring unsupported locator head border: {}", locatorBarHeadBorder);
		locatorBarHeadBorder = TweakifyModConfig.LOCATOR_HEAD_BORDER_OFF;
		return true;
	}

	// 修正描边粗细：四档代码；旧版数字（1/2/3）按 1→normal、2 及以上→thick 迁移。
	private boolean sanitizeLocatorHeadBorderThickness() {
		String value = locatorBarHeadBorderThickness;
		String migrated = switch (value == null ? "" : value.trim()) {
			case "1" -> TweakifyModConfig.LOCATOR_HEAD_BORDER_NORMAL;
			case "2", "3" -> TweakifyModConfig.LOCATOR_HEAD_BORDER_THICK;
			default -> value;
		};
		if (TweakifyModConfig.isSupportedValue(
			TweakifyModConfig.SUPPORTED_LOCATOR_HEAD_BORDER_THICKNESSES, migrated)) {
			if (!migrated.equals(value)) {
				LOGGER.info("Migrating locator head border thickness {} to {}", value, migrated);
				locatorBarHeadBorderThickness = migrated;
				return true;
			}
			return false;
		}
		LOGGER.warn("Ignoring unsupported locator head border thickness: {}", value);
		locatorBarHeadBorderThickness = TweakifyModConfig.DEFAULT_LOCATOR_HEAD_BORDER_THICKNESS;
		return true;
	}

	// 修正描边固定颜色。
	private boolean sanitizeLocatorBorderColor() {
		if (TweakifyModConfig.isSupportedValue(
			TweakifyModConfig.SUPPORTED_LOCATOR_COLORS, locatorBarHeadBorderColor)) {
			return false;
		}
		LOGGER.warn("Ignoring unsupported locator border color: {}", locatorBarHeadBorderColor);
		locatorBarHeadBorderColor = TweakifyModConfig.LOCATOR_COLOR_WHITE;
		return true;
	}

	// 修正名字显示时机。
	private boolean sanitizeLocatorHeadNames() {
		if (TweakifyModConfig.isSupportedValue(
			TweakifyModConfig.SUPPORTED_LOCATOR_HEAD_NAMES, locatorBarHeadNames)) {
			return false;
		}
		LOGGER.warn("Ignoring unsupported locator head names mode: {}", locatorBarHeadNames);
		locatorBarHeadNames = TweakifyModConfig.LOCATOR_HEAD_NAMES_NEVER;
		return true;
	}

	// 修正玩家过滤模式。
	private boolean sanitizeLocatorHeadFilter() {
		if (TweakifyModConfig.isSupportedValue(
			TweakifyModConfig.SUPPORTED_LOCATOR_HEAD_FILTERS, locatorBarHeadFilter)) {
			return false;
		}
		LOGGER.warn("Ignoring unsupported locator head filter: {}", locatorBarHeadFilter);
		locatorBarHeadFilter = TweakifyModConfig.LOCATOR_HEAD_FILTER_ALL;
		return true;
	}

	// 修正指南针颜色。
	private boolean sanitizeLocatorCompassColor() {
		if (TweakifyModConfig.isSupportedValue(
			TweakifyModConfig.SUPPORTED_LOCATOR_COLORS, locatorBarCompassColor)) {
			return false;
		}
		LOGGER.warn("Ignoring unsupported locator compass color: {}", locatorBarCompassColor);
		locatorBarCompassColor = TweakifyModConfig.LOCATOR_COLOR_WHITE;
		return true;
	}

	// 修正相机运动强度。
	private boolean sanitizeCameraMotionIntensity() {
		int clamped = Math.max(TweakifyModConfig.MIN_CAMERA_MOTION_INTENSITY,
			Math.min(TweakifyModConfig.MAX_CAMERA_MOTION_INTENSITY, cameraMotionIntensity));
		if (clamped == cameraMotionIntensity) {
			return false;
		}
		LOGGER.warn("Clamping cameraMotionIntensity from {} to {}", cameraMotionIntensity, clamped);
		cameraMotionIntensity = clamped;
		return true;
	}

	// 修正高亮描边样式，回退到常亮。
	private boolean sanitizeEntityHighlightStyle() {
		if (TweakifyModConfig.isSupportedValue(
			TweakifyModConfig.SUPPORTED_ENTITY_HIGHLIGHT_STYLES, entityHighlightStyle)) {
			return false;
		}
		LOGGER.warn("Ignoring unsupported entity highlight style: {}", entityHighlightStyle);
		entityHighlightStyle = TweakifyModConfig.ENTITY_HIGHLIGHT_STYLE_OUTLINE;
		return true;
	}

	// 修正闪烁周期，手工改坏配置时夹回允许范围。
	private boolean sanitizeEntityHighlightBlinkPeriod() {
		int clamped = Math.max(TweakifyModConfig.MIN_ENTITY_HIGHLIGHT_BLINK_PERIOD,
			Math.min(TweakifyModConfig.MAX_ENTITY_HIGHLIGHT_BLINK_PERIOD, entityHighlightBlinkPeriod));
		if (clamped == entityHighlightBlinkPeriod) {
			return false;
		}
		LOGGER.warn("Clamping entityHighlightBlinkPeriod from {} to {}",
			entityHighlightBlinkPeriod, clamped);
		entityHighlightBlinkPeriod = clamped;
		return true;
	}

	// 修正描边颜色，回退到原版队伍色。
	private boolean sanitizeEntityHighlightColor() {
		if (TweakifyModConfig.isSupportedValue(
			TweakifyModConfig.SUPPORTED_ENTITY_HIGHLIGHT_COLORS, entityHighlightColor)) {
			return false;
		}
		LOGGER.warn("Ignoring unsupported entity highlight color: {}", entityHighlightColor);
		entityHighlightColor = TweakifyModConfig.ENTITY_HIGHLIGHT_COLOR_VANILLA;
		return true;
	}
}
