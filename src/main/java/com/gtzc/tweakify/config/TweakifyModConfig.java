package com.gtzc.tweakify.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class TweakifyModConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger("tweakify-mod-config");

	// 反射字段表（字段名 == JSON 键名）：按名字读写配置，使命令注册、MaLiLib 双向同步与
	// 写盘回滚共用同一份字段清单，新增开关时无需再同步修改多处拷贝清单。
	private static final Map<String, Field> CONFIG_FIELDS = indexConfigFields();

	private static Map<String, Field> indexConfigFields() {
		Map<String, Field> fields = new LinkedHashMap<>();
		for (Field field : TweakifyModConfig.class.getDeclaredFields()) {
			int modifiers = field.getModifiers();
			if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) {
				continue;
			}
			field.setAccessible(true);
			fields.put(field.getName(), field);
		}
		return Collections.unmodifiableMap(fields);
	}

	// 配置文件版本：字段语义发生变化时递增，并在 load() 里按旧版本做迁移。
	public static final int CURRENT_CONFIG_VERSION = 2;

	// 模组界面支持的语言代码，与 assets/tweakify-mod/lang 下的文件名一致。
	public static final List<String> SUPPORTED_LANGUAGES = List.of("zh_cn", "en_us", "ru_ru", "fr_fr");

	// 快捷键字段的统一前缀：字段名为 hotkey + 配置项名首字母大写。
	// 选项注册（TweakifyModConfigOptions）与配置清洗共用，避免两处硬编码字符串漂移。
	public static final String HOTKEY_FIELD_PREFIX = "hotkey";

	// 三个“空列表 = 不做限制”的物品列表字段名：配置文件缺少这些键时必须补回默认值。
	public static final String RESTRICTED_OFFHAND_ITEMS_FIELD = "restrictedOffhandItems";
	public static final String MAIN_HAND_USE_COOLDOWN_ITEMS_FIELD = "mainHandUseCooldownItems";
	public static final String OFFHAND_USE_COOLDOWN_ITEMS_FIELD = "offhandUseCooldownItems";

	// 配置版本字段名：迁移步骤专用，档案加载时不复制（档案不应改写当前版本号）。
	private static final String CONFIG_VERSION_FIELD = "configVersion";

	// 这些字段的修正在 load() 的前置迁移步骤完成，不要求登记字段级校验器。
	private static final Set<String> MIGRATION_FIELDS = Set.of(CONFIG_VERSION_FIELD);

	// 字段级校验器：每个需要清洗的数值/枚举字段都必须登记，
	// 启动期由 assertSanitizersCoverAllFields() 断言覆盖度，
	// 新增字段忘记登记时直接让类初始化失败，而不是静默接受越界或非法的值。
	// 未绑定方法引用（TweakifyModConfig::sanitizeXxx）把接收者作为唯一参数，
	// 因此校验器本身仍可保留为实例方法。
	private static final List<FieldSanitizer> FIELD_SANITIZERS = buildFieldSanitizers();

	@FunctionalInterface
	private interface FieldSanitizerAction {
		boolean sanitize(TweakifyModConfig config);
	}

	// 一个校验器可以负责多个字段：攻击范围的距离与数量需要一起夹紧。
	private record FieldSanitizer(List<String> fields, FieldSanitizerAction action) {
		static FieldSanitizer of(String field, FieldSanitizerAction action) {
			return new FieldSanitizer(List.of(field), action);
		}
	}

	private static List<FieldSanitizer> buildFieldSanitizers() {
		List<FieldSanitizer> sanitizers = new ArrayList<>();
		sanitizers.add(FieldSanitizer.of("language", TweakifyModConfig::sanitizeLanguage));
		sanitizers.add(FieldSanitizer.of("itemDespawnTime", TweakifyModConfig::sanitizeItemDespawnTime));
		sanitizers.add(FieldSanitizer.of("itemDespawnTimeXpOrbs", TweakifyModConfig::sanitizeXpOrbDespawnTime));
		sanitizers.add(FieldSanitizer.of("durabilityOverlayLowThreshold",
			TweakifyModConfig::sanitizeDurabilityThreshold));
		sanitizers.add(FieldSanitizer.of("nightVisionFadeOutDuration",
			TweakifyModConfig::sanitizeNightVisionFadeDuration));
		sanitizers.add(FieldSanitizer.of("betterBlockPlacementMode", TweakifyModConfig::sanitizeBlockPlacementMode));
		sanitizers.add(FieldSanitizer.of("copyItemIdFormat", TweakifyModConfig::sanitizeCopyIdFormat));
		sanitizers.add(FieldSanitizer.of("chatSelectionColor", TweakifyModConfig::sanitizeChatSelectionColor));
		sanitizers.add(FieldSanitizer.of("durabilityDisplayMode", TweakifyModConfig::sanitizeDurabilityDisplayMode));
		sanitizers.add(FieldSanitizer.of("attackRangeDisplayMode",
			TweakifyModConfig::sanitizeAttackRangeDisplayMode));
		sanitizers.add(new FieldSanitizer(
			List.of("attackRangeMaxDistance", "attackRangeMaxTargets"),
			TweakifyModConfig::sanitizeAttackRangeLimits));
		sanitizers.add(FieldSanitizer.of("attackRangeRenderMode",
			TweakifyModConfig::sanitizeAttackRangeRenderMode));
		sanitizers.add(FieldSanitizer.of("highlightSingleLayerBedrockMaxMarkers",
			TweakifyModConfig::sanitizeBedrockMarkerLimit));
		sanitizers.add(FieldSanitizer.of("cutThroughItems", TweakifyModConfig::sanitizeCutThroughItems));
		sanitizers.add(FieldSanitizer.of("cutThroughPlants", TweakifyModConfig::sanitizeCutThroughPlants));
		sanitizers.add(FieldSanitizer.of("nightVisionFadeOutCurve", TweakifyModConfig::sanitizeNightVisionCurve));
		sanitizers.add(FieldSanitizer.of("elytraFireworkCooldownTicks",
			TweakifyModConfig::sanitizeElytraFireworkTicks));
		sanitizers.add(FieldSanitizer.of("blockOutlineWidth", TweakifyModConfig::sanitizeBlockOutlineWidth));
		sanitizers.add(FieldSanitizer.of("blockOutlinePalette", TweakifyModConfig::sanitizeBlockOutlinePalette));
		sanitizers.add(FieldSanitizer.of("blockOutlineColorMode", TweakifyModConfig::sanitizeBlockOutlineColorMode));
		sanitizers.add(FieldSanitizer.of("blockOutlineAnimationSpeed",
			TweakifyModConfig::sanitizeBlockOutlineAnimationSpeed));
		assertSanitizersCoverAllFields(sanitizers);
		return List.copyOf(sanitizers);
	}

	// 启动期断言（类加载期执行）：每个数值/枚举字段都必须登记校验器。
	// 四类问题都会在这里直接失败：字段名拼错、重复登记、新增字段未登记、字段改名后表未跟着改。
	private static void assertSanitizersCoverAllFields(List<FieldSanitizer> sanitizers) {
		Set<String> covered = new LinkedHashSet<>();
		for (FieldSanitizer sanitizer : sanitizers) {
			for (String fieldName : sanitizer.fields()) {
				if (!CONFIG_FIELDS.containsKey(fieldName)) {
					throw new IllegalStateException("Sanitizer registered for unknown config field: " + fieldName);
				}
				if (!covered.add(fieldName)) {
					throw new IllegalStateException("Config field has more than one sanitizer: " + fieldName);
				}
			}
		}
		for (Map.Entry<String, Field> entry : CONFIG_FIELDS.entrySet()) {
			String name = entry.getKey();
			if (MIGRATION_FIELDS.contains(name) || isImplicitlySanitized(name, entry.getValue().getType())) {
				continue;
			}
			if (!covered.contains(name)) {
				throw new IllegalStateException("Config field without sanitizer: " + name);
			}
		}
	}

	// 布尔开关的值域天然安全；列表与热键分别由 sanitizeAllItems / sanitizeHotkeys 统一清洗。
	private static boolean isImplicitlySanitized(String name, Class<?> type) {
		return type == boolean.class
			|| List.class.isAssignableFrom(type)
			|| (type == String.class && name.startsWith(HOTKEY_FIELD_PREFIX));
	}

	// 掉落物消失时间（tick）的允许范围：3 秒 ~ 30 分钟，默认值为原版的 6000（5 分钟）。
	public static final int MIN_ITEM_DESPAWN_TICKS = 60;
	public static final int MAX_ITEM_DESPAWN_TICKS = 36000;
	public static final int DEFAULT_ITEM_DESPAWN_TICKS = 6000;

	// 耐久文字的显示内容：耐久百分比 / 剩余耐久数值。
	public static final String DURABILITY_DISPLAY_PERCENT = "percent";
	public static final String DURABILITY_DISPLAY_VALUE = "value";
	public static final List<String> SUPPORTED_DURABILITY_DISPLAYS = List.of(
		DURABILITY_DISPLAY_PERCENT, DURABILITY_DISPLAY_VALUE);

	// 自定义方块轮廓：字段名、步进、线宽范围与默认值。
	public static final String BLOCK_OUTLINE_WIDTH_FIELD = "blockOutlineWidth";
	public static final double BLOCK_OUTLINE_WIDTH_STEP = 0.5;
	public static final double MIN_BLOCK_OUTLINE_WIDTH = 0.5;
	public static final double MAX_BLOCK_OUTLINE_WIDTH = 8.0;
	// 原版线宽随分辨率在 2.5 ~ 5 之间变化，默认取 2.0 避免比原版明显更细。
	public static final double DEFAULT_BLOCK_OUTLINE_WIDTH = 2.0;

	// 彩虹/呼吸动画速度：百分比，100 为默认速度。
	public static final int MIN_BLOCK_OUTLINE_ANIMATION_SPEED = 10;
	public static final int MAX_BLOCK_OUTLINE_ANIMATION_SPEED = 400;
	public static final int DEFAULT_BLOCK_OUTLINE_ANIMATION_SPEED = 100;

	// 线宽步进与范围夹紧的唯一实现：滑块、配置清洗、setDouble 与渲染层共用。
	// 只做步进取整，不负责范围限制；需要范围约束时使用 clampBlockOutlineWidth。
	public static double roundBlockOutlineWidth(double value) {
		if (!Double.isFinite(value)) {
			return DEFAULT_BLOCK_OUTLINE_WIDTH;
		}
		return Math.round(value / BLOCK_OUTLINE_WIDTH_STEP) * BLOCK_OUTLINE_WIDTH_STEP;
	}

	public static double clampBlockOutlineWidth(double value) {
		return Math.max(MIN_BLOCK_OUTLINE_WIDTH,
			Math.min(MAX_BLOCK_OUTLINE_WIDTH, roundBlockOutlineWidth(value)));
	}

	// 轮廓颜色调色板。
	public static final String BLOCK_OUTLINE_PALETTE_WHITE = "white";
	public static final String BLOCK_OUTLINE_PALETTE_RED = "red";
	public static final String BLOCK_OUTLINE_PALETTE_GREEN = "green";
	public static final String BLOCK_OUTLINE_PALETTE_BLUE = "blue";
	public static final String BLOCK_OUTLINE_PALETTE_YELLOW = "yellow";
	public static final String BLOCK_OUTLINE_PALETTE_CYAN = "cyan";
	public static final String BLOCK_OUTLINE_PALETTE_MAGENTA = "magenta";
	public static final String BLOCK_OUTLINE_PALETTE_ORANGE = "orange";
	public static final List<String> SUPPORTED_BLOCK_OUTLINE_PALETTES = List.of(
		BLOCK_OUTLINE_PALETTE_WHITE, BLOCK_OUTLINE_PALETTE_RED, BLOCK_OUTLINE_PALETTE_GREEN,
		BLOCK_OUTLINE_PALETTE_BLUE, BLOCK_OUTLINE_PALETTE_YELLOW, BLOCK_OUTLINE_PALETTE_CYAN,
		BLOCK_OUTLINE_PALETTE_MAGENTA, BLOCK_OUTLINE_PALETTE_ORANGE);

	// 轮廓彩色模式：调色板单色 / 彩虹循环 / 呼吸。
	public static final String BLOCK_OUTLINE_MODE_PALETTE = "palette";
	public static final String BLOCK_OUTLINE_MODE_RAINBOW = "rainbow";
	public static final String BLOCK_OUTLINE_MODE_PULSE = "pulse";
	public static final List<String> SUPPORTED_BLOCK_OUTLINE_COLOR_MODES = List.of(
		BLOCK_OUTLINE_MODE_PALETTE, BLOCK_OUTLINE_MODE_RAINBOW, BLOCK_OUTLINE_MODE_PULSE);

	// 攻击范围的显示方式：平面圆环 / 半透明球体。
	public static final String ATTACK_RANGE_DISPLAY_FLAT = "flat";
	public static final String ATTACK_RANGE_DISPLAY_SPHERE = "sphere";
	public static final List<String> SUPPORTED_ATTACK_RANGE_DISPLAY_MODES = List.of(
		ATTACK_RANGE_DISPLAY_FLAT, ATTACK_RANGE_DISPLAY_SPHERE);

	// 攻击范围渲染方式：蓝色线框 / 半透明实心。
	public static final String ATTACK_RANGE_RENDER_WIREFRAME = "wireframe";
	public static final String ATTACK_RANGE_RENDER_FILLED = "filled";
	public static final List<String> SUPPORTED_ATTACK_RANGE_RENDER_MODES = List.of(
		ATTACK_RANGE_RENDER_WIREFRAME, ATTACK_RANGE_RENDER_FILLED);

	// 攻击范围的显示数量/距离限制。
	public static final int MIN_ATTACK_RANGE_DISTANCE = 8;
	public static final int MAX_ATTACK_RANGE_DISTANCE = 128;
	public static final int DEFAULT_ATTACK_RANGE_DISTANCE = 48;
	public static final int MIN_ATTACK_RANGE_TARGETS = 8;
	public static final int MAX_ATTACK_RANGE_TARGETS = 256;
	public static final int DEFAULT_ATTACK_RANGE_TARGETS = 64;

	// 单层基岩高亮的标记数量限制。
	public static final int MIN_BEDROCK_MARKERS = 1;
	public static final int MAX_BEDROCK_MARKERS = 256;
	public static final int DEFAULT_BEDROCK_MARKERS = 32;

	// 更好的放置覆盖：可覆盖的植物范围（仅花草 / 含作物 / 全部植物）。
	public static final String BLOCK_PLACEMENT_FLOWERS = "flowers";
	public static final String BLOCK_PLACEMENT_CROPS = "crops";
	public static final String BLOCK_PLACEMENT_ALL = "all";
	public static final List<String> SUPPORTED_BLOCK_PLACEMENT_MODES = List.of(
		BLOCK_PLACEMENT_FLOWERS, BLOCK_PLACEMENT_CROPS, BLOCK_PLACEMENT_ALL);

	// 复制 ID 的输出格式：纯 ID / 带类型前缀 / 带数量。
	public static final String COPY_ID_FORMAT_PLAIN = "plain";
	public static final String COPY_ID_FORMAT_TYPED = "typed";
	public static final String COPY_ID_FORMAT_COUNTED = "counted";
	public static final List<String> SUPPORTED_COPY_ID_FORMATS = List.of(
		COPY_ID_FORMAT_PLAIN, COPY_ID_FORMAT_TYPED, COPY_ID_FORMAT_COUNTED);

	// 聊天框选颜色。
	public static final String CHAT_SELECTION_BLUE = "blue";
	public static final String CHAT_SELECTION_GREEN = "green";
	public static final String CHAT_SELECTION_ORANGE = "orange";
	public static final String CHAT_SELECTION_PURPLE = "purple";
	public static final String CHAT_SELECTION_GRAY = "gray";
	public static final List<String> SUPPORTED_CHAT_SELECTION_COLORS = List.of(
		CHAT_SELECTION_BLUE, CHAT_SELECTION_GREEN, CHAT_SELECTION_ORANGE,
		CHAT_SELECTION_PURPLE, CHAT_SELECTION_GRAY);

	// 耐久文字低阈值（百分比）与夜视淡出时长的允许范围。
	public static final int MIN_DURABILITY_THRESHOLD = 0;
	public static final int MAX_DURABILITY_THRESHOLD = 100;
	public static final int MIN_NIGHT_VISION_FADE_TICKS = 20;
	public static final int MAX_NIGHT_VISION_FADE_TICKS = 400;
	public static final int DEFAULT_NIGHT_VISION_FADE_TICKS = 200;

	// 剑穿草打生物：生效物品范围与植物范围。
	public static final String CUT_THROUGH_ITEMS_SWORD = "sword";
	public static final String CUT_THROUGH_ITEMS_SWORD_AND_AXE = "sword_and_axe";
	public static final String CUT_THROUGH_ITEMS_ANY = "any";
	public static final List<String> SUPPORTED_CUT_THROUGH_ITEMS = List.of(
		CUT_THROUGH_ITEMS_SWORD, CUT_THROUGH_ITEMS_SWORD_AND_AXE, CUT_THROUGH_ITEMS_ANY);
	public static final String CUT_THROUGH_PLANTS_FLOWERS = "flowers";
	public static final String CUT_THROUGH_PLANTS_CROPS = "crops";
	public static final String CUT_THROUGH_PLANTS_ALL = "all";
	public static final List<String> SUPPORTED_CUT_THROUGH_PLANTS = List.of(
		CUT_THROUGH_PLANTS_FLOWERS, CUT_THROUGH_PLANTS_CROPS, CUT_THROUGH_PLANTS_ALL);

	// 夜视淡出曲线：平滑（smoothstep）/ 线性。
	public static final String NIGHT_VISION_CURVE_SMOOTH = "smooth";
	public static final String NIGHT_VISION_CURVE_LINEAR = "linear";
	public static final List<String> SUPPORTED_NIGHT_VISION_CURVES = List.of(
		NIGHT_VISION_CURVE_SMOOTH, NIGHT_VISION_CURVE_LINEAR);

	// 鞘翅烟花最短间隔（tick）：0 表示等本次加速结束（原行为）。
	public static final int MIN_ELYTRA_FIREWORK_TICKS = 0;
	public static final int MAX_ELYTRA_FIREWORK_TICKS = 200;

	// 配置文件路径。测试时可通过包私有构造器注入；反序列化（Gson）时不参与。
	private transient Path configPath;

	// /tweakify 命令在服务端线程改值，而 MaLiLib 选项属于客户端线程：
	// 这里只挂一个标记，由客户端 tick 统一做一次全量同步，避免跨线程直接改界面控件。
	private transient final AtomicBoolean optionSyncRequested = new AtomicBoolean();

	public TweakifyModConfig() {
		this.configPath = null;
	}

	TweakifyModConfig(Path configPath) {
		this.configPath = configPath;
	}

	private Path configPathOrDefault() {
		return configPath != null ? configPath : ConfigStorage.defaultPath();
	}

	// 配置文件版本号：读取旧文件（缺少该字段时为 0）后自动升级到当前版本。
	public volatile int configVersion = CURRENT_CONFIG_VERSION;

	// 模组界面语言代码（zh_cn / en_us / ru_ru / fr_fr）；跟随系统语言时忽略。
	public volatile String language = "zh_cn";

	// 跟随系统语言：开启时使用游戏当前语言，忽略 language 设置。
	public volatile boolean followSystemLanguage = true;

	// 掉落物消失时间（tick）：默认 6000 = 原版 5 分钟。超出范围的配置在读取时会被夹紧。
	public volatile int itemDespawnTime = DEFAULT_ITEM_DESPAWN_TICKS;

	// 仅对玩家丢弃的掉落物使用上面的时间；其它来源保持原版 6000。
	public volatile boolean itemDespawnPlayerDroppedOnly = false;

	// 经验球消失时间（tick），默认 6000 = 原版 5 分钟。
	public volatile int itemDespawnTimeXpOrbs = DEFAULT_ITEM_DESPAWN_TICKS;

	// 复制物体 ID：开启后用“复制目标 ID”快捷键对着方块、实体或容器内悬停的物品复制其 ID。
	public volatile boolean copyItemId = false;
	// 复制物体 ID 的切换快捷键，以及真正执行复制的快捷键（子设置）。
	public volatile String hotkeyCopyItemId = "";
	public volatile String hotkeyCopyTargetId = "";

	// 复制格式：plain（纯 ID）/ typed（带 block/entity/item 前缀）/ counted（带数量）。
	public volatile String copyItemIdFormat = COPY_ID_FORMAT_PLAIN;

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

	// 禁用使用物品减速：进食、拉弓、举盾等使用物品期间不再降低移动速度。
	public volatile boolean disableUseItemSlowdown = false;

	// 这些物品仍保留原版的使用减速（物品 ID 列表）。
	private volatile List<String> useItemSlowdownExceptions = new ArrayList<>();

	// 禁用受伤抖动：受到伤害时不再产生视角抖动。
	public volatile boolean disableHurtShake = false;

	// 移除飞行漂移：创造/旁观飞行时松开方向键立即停止水平移动，
	// 消除原版空气摩擦带来的惯性滑行。
	public volatile boolean disableFlightDrift = false;

	// 垂直飞行影响：开启移除飞行漂移时，松开跳跃/潜行键后也立即停止升降。
	public volatile boolean flightDriftVertical = false;

	// 禁用受伤变红：受伤时不再为生物叠加红色 overlay（死亡变红保留）。
	public volatile boolean disableHurtTint = false;

	// 禁用维度传送：总开关，阻止实体通过传送门切换维度。
	public volatile boolean disableDimensionTeleport = false;

	// 只拦截玩家的维度传送，其他实体仍可正常穿过传送门。
	public volatile boolean dimensionTeleportPlayersOnly = false;

	// 禁用地狱门传送：总开关开启时，阻止实体通过下界传送门。
	public volatile boolean disableNetherPortalTeleport = true;

	// 禁用末地门传送：总开关开启时，阻止实体通过要塞末地传送门。
	public volatile boolean disableEndPortalTeleport = true;

	// 禁用末地折跃门传送：总开关开启时，阻止实体通过末地折跃门。
	public volatile boolean disableEndGatewayTeleport = true;

	// 禁用水下挖掘惩罚：水下挖掘不再按 SUBMERGED_MINING_SPEED 属性减速。
	public volatile boolean disableUnderwaterMiningPenalty = false;

	// 禁用空中挖掘惩罚：离地挖掘不再额外降低速度。
	public volatile boolean disableAirMiningPenalty = false;

	// 高亮单层基岩：描出周围上下都不是基岩的基岩（下界顶棚的可击穿薄弱点）。
	public volatile boolean highlightSingleLayerBedrock = false;

	// 单层基岩单次最多显示的标记数量（超出时保留最近的）。
	public volatile int highlightSingleLayerBedrockMaxMarkers = DEFAULT_BEDROCK_MARKERS;

	// 诅咒附魔可移除：砂轮可以去除诅咒附魔（原版会保留诅咒）。
	public volatile boolean disableCurseEnchantmentRestriction = false;

	// 禁用附魔过于昂贵：铁砧花费达到 40 级时仍可取出结果（仅单人/局域网生效）。
	public volatile boolean disableTooExpensive = false;

	// 锹单层铲雪：手持锹破坏多层雪层时一次只减少一层，而非整块移除。
	public volatile boolean disableShovelSnowFullBreak = false;

	// 铲雪时掉落雪球。
	public volatile boolean shovelSnowDropSnowball = true;

	// 铲雪时消耗锹的耐久。
	public volatile boolean shovelSnowConsumeDurability = true;

	// 草径卡船修复：土径对船的碰撞按满方块处理，消除土径与完整方块之间的 1/16 台阶。
	public volatile boolean fixBoatStuckOnDirtPath = false;

	// 鞘翅烟花使用限制：滑翔中使用烟花后，等烟花加速结束才能再次使用。
	public volatile boolean elytraFireworkCooldown = false;

	// 两次烟花之间的最短间隔（tick）；0 = 等本次加速结束（原行为）。
	public volatile int elytraFireworkCooldownTicks = MIN_ELYTRA_FIREWORK_TICKS;

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

	// 聊天框文字框选：在聊天界面拖动鼠标选择消息文字，Ctrl+C 复制。
	public volatile boolean chatTextSelection = false;

	// 框选颜色：blue / green / orange / purple / gray。
	public volatile String chatSelectionColor = CHAT_SELECTION_BLUE;

	// 松开鼠标时自动复制选中文字。
	public volatile boolean chatSelectionAutoCopy = false;

	// 生物攻击范围显示：以生物为中心画半透明蓝色范围，列表为空时作用于全部生物与玩家。
	public volatile boolean attackRangeDisplay = false;
	public volatile List<String> attackRangeEntities = new ArrayList<>();

	// 是否显示本地玩家自己的攻击范围（默认关闭，避免球体包住自身）。
	public volatile boolean attackRangeIncludeSelf = false;

	// 收集范围（格）与单次最多显示的目标数量。
	public volatile int attackRangeMaxDistance = DEFAULT_ATTACK_RANGE_DISTANCE;
	public volatile int attackRangeMaxTargets = DEFAULT_ATTACK_RANGE_TARGETS;

	// 攻击范围显示方式：flat（脚下平面圆环）/ sphere（经纬线框球体）。
	public volatile String attackRangeDisplayMode = ATTACK_RANGE_DISPLAY_FLAT;

	// 攻击范围渲染方式：wireframe（蓝色线框，性能更好）/ filled（半透明实心，可能掉帧）。
	public volatile String attackRangeRenderMode = ATTACK_RANGE_RENDER_WIREFRAME;

	// 夜视淡出：夜视效果结束前平滑淡出，不再闪烁。
	public volatile boolean nightVisionFadeOut = false;

	// 夜视淡出曲线：smooth（平滑）/ linear（线性）。
	public volatile String nightVisionFadeOutCurve = NIGHT_VISION_CURVE_SMOOTH;

	// 夜视淡出时长（tick）：剩余时间进入该窗口后开始淡出。
	public volatile int nightVisionFadeOutDuration = DEFAULT_NIGHT_VISION_FADE_TICKS;

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

	// 自定义方块轮廓：总开关、线宽、调色板与彩色模式。
	public volatile boolean customBlockOutline = false;
	public volatile double blockOutlineWidth = DEFAULT_BLOCK_OUTLINE_WIDTH;
	public volatile int blockOutlineAnimationSpeed = DEFAULT_BLOCK_OUTLINE_ANIMATION_SPEED;
	public volatile String blockOutlinePalette = BLOCK_OUTLINE_PALETTE_WHITE;
	public volatile String blockOutlineColorMode = BLOCK_OUTLINE_MODE_PALETTE;

	// 更好的放置覆盖：手持方块右键时可直接替换花、高草等植物（仅单人/局域网生效）。
	public volatile boolean betterBlockPlacement = false;

	// 可覆盖的植物范围：flowers（仅花草）/ crops（含作物）/ all（全部植物）。
	public volatile String betterBlockPlacementMode = BLOCK_PLACEMENT_FLOWERS;

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

	// 剑穿草打生物：主手持剑时跳过花草，直接选中后方的生物。
	public volatile boolean swordCutThroughPlants = false;

	// 生效物品范围：sword（仅剑）/ sword_and_axe（剑与斧）/ any（任意物品）。
	public volatile String cutThroughItems = CUT_THROUGH_ITEMS_SWORD;

	// 植物范围：flowers（花草）/ crops（含作物）/ all（全部无碰撞植物）。
	public volatile String cutThroughPlants = CUT_THROUGH_PLANTS_ALL;

	// 潜行时恢复原版：左键先把植物打掉，而不是穿过去。
	public volatile boolean cutThroughSneakBypass = true;

	// 中键获得液体桶：准星指向水/岩浆时，中键得到对应的液体桶（仅创造模式）。
	public volatile boolean pickFluidBucket = false;

	// 生存模式中键取物：准星指向方块/液体/实体时，背包里有对应物品就切换到主手。
	public volatile boolean survivalPickBlock = false;

	// 强制关闭加载屏幕：加载世界时立即关掉“正在加载地形”界面。
	public volatile boolean forceCloseLoadingScreen = false;

	// 关闭资源包兼容性提示：资源包界面不显示不兼容标记，也不弹确认框（仅资源包）。
	public volatile boolean disableResourcePackCompatibilityCheck = false;

	// 屏障可见：开启后在周围屏障方块中心显示红色标记，未手持屏障物品也能看到。
	public volatile boolean barrierVisibility = false;

	// 装备耐久显示：在物品栏耐久条上方叠加耐久文字，满耐久时不显示。
	public volatile boolean durabilityOverlay = false;

	// 耐久文字内容：percent（耐久百分比）/ value（剩余耐久数值）。
	public volatile String durabilityDisplayMode = DURABILITY_DISPLAY_PERCENT;

	// 仅在 HUD 快捷栏显示耐久文字（打开任意界面时不显示）。
	public volatile boolean durabilityOverlayHotbarOnly = false;

	// 只在剩余耐久不高于该百分比时显示（0 = 不限制）。
	public volatile int durabilityOverlayLowThreshold = MIN_DURABILITY_THRESHOLD;

	// 滚动列表记忆：关闭界面时记住列表滚动位置，下次打开直接回到该位置。
	public volatile boolean scrollListMemory = true;

	// 跨重启记忆：把位置写入配置文件，下次启动仍生效。
	public volatile boolean scrollListMemoryPersist = false;

	// 子开关：记忆 MaLiLib 配置界面的滚动位置。
	public volatile boolean malilibScrollMemory = true;

	// 子开关：记忆 Mod Menu 模组列表的滚动位置。
	public volatile boolean modMenuScrollMemory = true;

	// 标签记忆：记住上次选择的分类标签，下次打开配置界面直接停在该标签。
	public volatile boolean rememberSelectedTab = false;

	// 各功能根开关的切换快捷键，默认无快捷键（空字符串）。
	// 功能标签只给根功能提供快捷键，子设置不占快捷键栏。
	public volatile String hotkeyMiningCooldown = "";
	public volatile String hotkeyUseCooldown = "";
	public volatile String hotkeyDisableFlightDrift = "";
	public volatile String hotkeyFlightDriftVertical = "";
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
	public volatile String hotkeyDisableOverlayEffects = "";

	// 主手使用冷却的物品 ID 列表，默认是所有可交互物品。
	private volatile List<String> mainHandUseCooldownItems = new ArrayList<>();

	// 副手使用冷却的物品 ID 列表，默认是所有可交互物品。
	private volatile List<String> offhandUseCooldownItems = new ArrayList<>();

	// 受限的副手物品 ID 列表（namespace:path）。默认是所有光源物品。
	// 仅当副手物品命中该列表时，主手用完瞬间才会拦截副手接手；可手动增删。
	private volatile List<String> restrictedOffhandItems = new ArrayList<>();

	// 打开配置界面的快捷键，默认 G + C。
	public volatile String hotkeyOpenConfigGui = "G,C";

	// 实体高亮总开关：开启后按当前启用的预设列表显示发光描边。
	public volatile boolean entityHighlight = false;
	public volatile String hotkeyEntityHighlight = "";

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

	// 运行期加速用的缓存集合，不参与序列化。
	// volatile：命令写入发生在集成服务端线程，判定读发生在客户端线程，
	// volatile 同时保证“集合已构建完成”对读线程可见。
	private transient volatile Set<String> restrictedCache;
	private transient volatile Set<String> mainHandItemsCache;
	private transient volatile Set<String> offhandItemsCache;
	private transient volatile Set<String> preset1EntitiesCache;
	private transient volatile Set<String> preset2EntitiesCache;
	private transient volatile Set<String> preset3EntitiesCache;
	private transient volatile Set<String> netherPortalBlacklistCache;
	private transient volatile Set<String> endPortalBlacklistCache;
	private transient volatile Set<String> endGatewayBlacklistCache;
	private transient volatile Set<String> attackRangeEntitiesCache;
	private transient volatile Set<String> useItemSlowdownExceptionCache;

	// 缓存是否已过期：一次配置同步会连续写 8 个列表，延迟到下次查询时只重建一次。
	private transient volatile boolean cacheDirty = true;

	// 实体高亮判定缓存：渲染每帧对每个可见实体都会查询，避免重复注册表查找与字符串分配。
	// 值以实体类型对象身份为键，实体列表/预设/总开关变化时整体失效。
	private transient volatile Map<EntityType<?>, Boolean> entityHighlightCache;

	// 上次构建高亮缓存时的开关组合：字段是 public 的，直接赋值也要能检测到变化。
	private transient boolean cachedEntityHighlight;
	private transient boolean cachedHighlightPreset1;
	private transient boolean cachedHighlightPreset2;
	private transient boolean cachedHighlightPreset3;

	public static TweakifyModConfig load() {
		Path configPath = ConfigStorage.defaultPath();
		ConfigStorage.migrateLegacy(configPath);
		return load(configPath);
	}

	// 包私有入口，测试时注入临时目录，避免依赖 FabricLoader 环境。
	static TweakifyModConfig load(Path configPath) {
		boolean saveDefaults = true;
		if (Files.exists(configPath)) {
			try {
				String json = Files.readString(configPath);
				JsonElement parsed = JsonParser.parseString(json);
				if (!parsed.isJsonObject()) {
					throw new JsonParseException("Config root must be a JSON object");
				}
				JsonObject root = parsed.getAsJsonObject();
				TweakifyModConfig config = ConfigStorage.GSON.fromJson(root, TweakifyModConfig.class);
				if (config == null) {
					throw new JsonParseException("Config root cannot be null");
				}
				config.configPath = configPath;
				int loadedVersion = root.has("configVersion") ? config.configVersion : 0;
				// 前置迁移：只处理版本号与缺失键，不做字段值校验。
				boolean corrected = config.sanitizeConfigVersion(root.has("configVersion"))
					| config.migrateScrollListMemoryDefault(loadedVersion)
					| config.applyMissingListDefaults(root);
				// 字段级校验：表驱动，新增数值/枚举字段未登记校验器时类加载期就失败。
				corrected |= config.sanitizeAllFields();
				// 容器级校验：所有列表与热键字段按类型统一清洗。
				corrected |= config.sanitizeAllItems() | config.sanitizeHotkeys();
				config.rebuildCache();
				if (corrected && !config.save()) {
					LOGGER.warn("Failed to save corrected config");
				}
				return config;
			} catch (JsonParseException e) {
				LOGGER.error("Failed to parse config, falling back to defaults", e);
				saveDefaults = ConfigStorage.backupBroken(configPath);
			} catch (IOException e) {
				LOGGER.error("Failed to read config, using defaults without overwriting the existing file", e);
				saveDefaults = false;
			} catch (RuntimeException e) {
				// 字段类型与 JSON 不匹配（例如把列表写成数字）会在这里抛出：
				// 同样备份损坏文件并回退默认值，不能因为一处坏配置就进不了游戏。
				LOGGER.error("Failed to load config, falling back to defaults", e);
				saveDefaults = ConfigStorage.backupBroken(configPath);
			}
		}

		TweakifyModConfig config = new TweakifyModConfig(configPath);
		config.applyDefaultItemLists();
		config.rebuildCache();
		if (saveDefaults) {
			config.save();
		}
		return config;
	}

	// 写盘时序列化整个对象：与写操作共用同一把锁，避免边改边写产生半新半旧的 JSON。
	public synchronized boolean save() {
		return ConfigStorage.save(this, configPathOrDefault());
	}

	// 在客户端线程完成序列化：synchronized 保证读到一致的字段状态，
	// 返回的文本不含任何引用，可以交给后台线程写盘。
	public synchronized String toJson() {
		return ConfigStorage.toJson(this);
	}

	// 在配置锁内序列化成 JSON 树：与写操作共用同一把锁，供档案保存整套配置。
	public synchronized JsonObject toJsonTree() {
		return ConfigStorage.toJsonTree(this);
	}

	// 只落盘，不读写字段：供后台写盘线程调用（不能在这里同步整个对象）。
	public boolean writeJson(String json) {
		return ConfigStorage.writeAtomically(json, configPathOrDefault());
	}

	// 修正配置版本：旧文件（缺少该字段）按 0 处理并升级到当前版本。
	private boolean sanitizeConfigVersion(boolean versionPresent) {
		int detected = versionPresent ? configVersion : 0;
		if (detected == CURRENT_CONFIG_VERSION) {
			return false;
		}
		LOGGER.info("Upgrading config version {} -> {}", detected, CURRENT_CONFIG_VERSION);
		configVersion = CURRENT_CONFIG_VERSION;
		return true;
	}

	// 版本 2：滚动列表记忆默认改为开启。版本 1 里该开关默认关闭，
	// 统一升级为开，避免旧配置文件一直停在关闭状态。
	private boolean migrateScrollListMemoryDefault(int loadedVersion) {
		if (loadedVersion >= 2 || scrollListMemory) {
			return false;
		}
		LOGGER.info("Enabling scrollListMemory for configs older than version 2");
		scrollListMemory = true;
		return true;
	}

	// 旧配置文件缺少物品列表键时补回默认值。
	// 这三个列表的空列表语义是“不做任何限制”，而不是“使用默认值”：
	// Gson 反序列化缺键会保留字段初始化器的空列表，对应功能就会静默失效。
	// 用 root.has() 而不是判断列表是否为空，是为了保留用户主动清空列表的意图。
	private boolean applyMissingListDefaults(JsonObject root) {
		boolean changed = false;
		if (!root.has(RESTRICTED_OFFHAND_ITEMS_FIELD)) {
			LOGGER.info("Filling missing default list: {}", RESTRICTED_OFFHAND_ITEMS_FIELD);
			setStringList(RESTRICTED_OFFHAND_ITEMS_FIELD, defaultLightSourceItemIds());
			changed = true;
		}
		if (!root.has(MAIN_HAND_USE_COOLDOWN_ITEMS_FIELD)) {
			LOGGER.info("Filling missing default list: {}", MAIN_HAND_USE_COOLDOWN_ITEMS_FIELD);
			setStringList(MAIN_HAND_USE_COOLDOWN_ITEMS_FIELD, defaultInteractableItemIds());
			changed = true;
		}
		if (!root.has(OFFHAND_USE_COOLDOWN_ITEMS_FIELD)) {
			LOGGER.info("Filling missing default list: {}", OFFHAND_USE_COOLDOWN_ITEMS_FIELD);
			setStringList(OFFHAND_USE_COOLDOWN_ITEMS_FIELD, defaultInteractableItemIds());
			changed = true;
		}
		return changed;
	}

	// 新建配置文件：三个物品列表直接使用默认值（写入时统一走清洗）。
	private void applyDefaultItemLists() {
		setStringList(RESTRICTED_OFFHAND_ITEMS_FIELD, defaultLightSourceItemIds());
		setStringList(MAIN_HAND_USE_COOLDOWN_ITEMS_FIELD, defaultInteractableItemIds());
		setStringList(OFFHAND_USE_COOLDOWN_ITEMS_FIELD, defaultInteractableItemIds());
	}

	// 按表驱动执行所有字段级校验：返回是否修正了值。
	private boolean sanitizeAllFields() {
		boolean changed = false;
		for (FieldSanitizer sanitizer : FIELD_SANITIZERS) {
			changed |= sanitizer.action().sanitize(this);
		}
		return changed;
	}

	// 包私有：已登记字段级校验器的字段名，供测试校验覆盖度。
	List<String> sanitizedFieldNames() {
		List<String> names = new ArrayList<>();
		for (FieldSanitizer sanitizer : FIELD_SANITIZERS) {
			names.addAll(sanitizer.fields());
		}
		return names;
	}

	// 包私有：按类型规则推出“必须登记校验器”的字段名，供测试校验覆盖度。
	static List<String> fieldsRequiringSanitizer() {
		List<String> names = new ArrayList<>();
		for (Map.Entry<String, Field> entry : CONFIG_FIELDS.entrySet()) {
			String name = entry.getKey();
			if (!MIGRATION_FIELDS.contains(name) && !isImplicitlySanitized(name, entry.getValue().getType())) {
				names.add(name);
			}
		}
		return names;
	}

	// 清洗所有 List<String> 配置字段（物品/实体 ID 列表）：新增列表字段时自动覆盖，不再手工维护。
	private boolean sanitizeAllItems() {
		boolean changed = false;
		for (Map.Entry<String, Field> entry : CONFIG_FIELDS.entrySet()) {
			Field field = entry.getValue();
			if (!List.class.isAssignableFrom(field.getType())) {
				continue;
			}
			// 配置里写成 null 时 Gson 会直接置空字段：此时“空列表 == 空列表”不会触发回写，
			// 留到 rebuildCache 就是 new HashSet<>(null) 的启动 NPE，这里强制回写一份空列表。
			Object raw = read(entry.getKey());
			List<String> original = asList(raw);
			List<String> sanitized = sanitizeItemList(original);
			if (raw == null || !sanitized.equals(original)) {
				write(entry.getKey(), sanitized);
				changed = true;
			}
		}
		return changed;
	}

	// 清洗快捷键字段：配置文件里把 hotkeyXxx 写成 null 时，Gson 会原样置空，
	// 而 MaLiLib 的 KeybindMulti.setValueFromString 会在 String.split 处抛 NPE，
	// 导致进游戏或打开配置界面直接崩溃。快捷键的“未绑定”表示是空字符串，这里统一回写。
	private boolean sanitizeHotkeys() {
		boolean changed = false;
		for (Map.Entry<String, Field> entry : CONFIG_FIELDS.entrySet()) {
			if (!entry.getKey().startsWith(HOTKEY_FIELD_PREFIX)
				|| entry.getValue().getType() != String.class) {
				continue;
			}
			if (read(entry.getKey()) == null) {
				LOGGER.warn("Replacing null hotkey config with an empty binding: {}", entry.getKey());
				write(entry.getKey(), "");
				changed = true;
			}
		}
		return changed;
	}

	// 修正掉落物消失时间：手工改坏配置时夹回允许范围。
	private boolean sanitizeItemDespawnTime() {
		int clamped = Math.max(MIN_ITEM_DESPAWN_TICKS, Math.min(MAX_ITEM_DESPAWN_TICKS, itemDespawnTime));
		if (clamped == itemDespawnTime) {
			return false;
		}
		LOGGER.warn("Clamping itemDespawnTime from {} to {}", itemDespawnTime, clamped);
		itemDespawnTime = clamped;
		return true;
	}

	// 修正不合法的耐久显示内容，回退到百分比。
	private boolean sanitizeDurabilityDisplayMode() {
		if (isSupportedValue(SUPPORTED_DURABILITY_DISPLAYS, durabilityDisplayMode)) {
			return false;
		}
		LOGGER.warn("Ignoring unsupported durability display mode in config: {}", durabilityDisplayMode);
		durabilityDisplayMode = DURABILITY_DISPLAY_PERCENT;
		return true;
	}

	// 修正不合法的攻击范围显示方式，回退到平面圆环。
	private boolean sanitizeAttackRangeDisplayMode() {
		if (isSupportedValue(SUPPORTED_ATTACK_RANGE_DISPLAY_MODES, attackRangeDisplayMode)) {
			return false;
		}
		LOGGER.warn("Ignoring unsupported attack range display mode in config: {}", attackRangeDisplayMode);
		attackRangeDisplayMode = ATTACK_RANGE_DISPLAY_FLAT;
		return true;
	}

	// 修正不合法的攻击范围渲染方式，回退到线框。
	private boolean sanitizeAttackRangeRenderMode() {
		if (isSupportedValue(SUPPORTED_ATTACK_RANGE_RENDER_MODES, attackRangeRenderMode)) {
			return false;
		}
		LOGGER.warn("Ignoring unsupported attack range render mode in config: {}", attackRangeRenderMode);
		attackRangeRenderMode = ATTACK_RANGE_RENDER_WIREFRAME;
		return true;
	}

	// 修正攻击范围的收集距离与目标数量，手工改坏配置时夹回允许范围。
	private boolean sanitizeAttackRangeLimits() {
		int distance = Math.max(MIN_ATTACK_RANGE_DISTANCE,
			Math.min(MAX_ATTACK_RANGE_DISTANCE, attackRangeMaxDistance));
		int targets = Math.max(MIN_ATTACK_RANGE_TARGETS,
			Math.min(MAX_ATTACK_RANGE_TARGETS, attackRangeMaxTargets));
		if (distance == attackRangeMaxDistance && targets == attackRangeMaxTargets) {
			return false;
		}
		LOGGER.warn("Clamping attack range limits: distance {} -> {}, targets {} -> {}",
			attackRangeMaxDistance, distance, attackRangeMaxTargets, targets);
		attackRangeMaxDistance = distance;
		attackRangeMaxTargets = targets;
		return true;
	}

	// 修正经验球消失时间，手工改坏配置时夹回允许范围。
	private boolean sanitizeXpOrbDespawnTime() {
		int clamped = Math.max(MIN_ITEM_DESPAWN_TICKS, Math.min(MAX_ITEM_DESPAWN_TICKS, itemDespawnTimeXpOrbs));
		if (clamped == itemDespawnTimeXpOrbs) {
			return false;
		}
		LOGGER.warn("Clamping itemDespawnTimeXpOrbs from {} to {}", itemDespawnTimeXpOrbs, clamped);
		itemDespawnTimeXpOrbs = clamped;
		return true;
	}

	// 修正耐久文字低阈值。
	private boolean sanitizeDurabilityThreshold() {
		int clamped = Math.max(MIN_DURABILITY_THRESHOLD,
			Math.min(MAX_DURABILITY_THRESHOLD, durabilityOverlayLowThreshold));
		if (clamped == durabilityOverlayLowThreshold) {
			return false;
		}
		LOGGER.warn("Clamping durabilityOverlayLowThreshold from {} to {}",
			durabilityOverlayLowThreshold, clamped);
		durabilityOverlayLowThreshold = clamped;
		return true;
	}

	// 修正夜视淡出时长。
	private boolean sanitizeNightVisionFadeDuration() {
		int clamped = Math.max(MIN_NIGHT_VISION_FADE_TICKS,
			Math.min(MAX_NIGHT_VISION_FADE_TICKS, nightVisionFadeOutDuration));
		if (clamped == nightVisionFadeOutDuration) {
			return false;
		}
		LOGGER.warn("Clamping nightVisionFadeOutDuration from {} to {}",
			nightVisionFadeOutDuration, clamped);
		nightVisionFadeOutDuration = clamped;
		return true;
	}

	// 修正不合法的放置覆盖模式。
	private boolean sanitizeBlockPlacementMode() {
		if (isSupportedValue(SUPPORTED_BLOCK_PLACEMENT_MODES, betterBlockPlacementMode)) {
			return false;
		}
		LOGGER.warn("Ignoring unsupported block placement mode: {}", betterBlockPlacementMode);
		betterBlockPlacementMode = BLOCK_PLACEMENT_FLOWERS;
		return true;
	}

	// 修正不合法的复制格式。
	private boolean sanitizeCopyIdFormat() {
		if (isSupportedValue(SUPPORTED_COPY_ID_FORMATS, copyItemIdFormat)) {
			return false;
		}
		LOGGER.warn("Ignoring unsupported copy ID format: {}", copyItemIdFormat);
		copyItemIdFormat = COPY_ID_FORMAT_PLAIN;
		return true;
	}

	// 修正不合法的框选颜色。
	private boolean sanitizeChatSelectionColor() {
		if (isSupportedValue(SUPPORTED_CHAT_SELECTION_COLORS, chatSelectionColor)) {
			return false;
		}
		LOGGER.warn("Ignoring unsupported chat selection color: {}", chatSelectionColor);
		chatSelectionColor = CHAT_SELECTION_BLUE;
		return true;
	}

	// 修正剑穿草打生物的生效物品范围。
	private boolean sanitizeCutThroughItems() {
		if (isSupportedValue(SUPPORTED_CUT_THROUGH_ITEMS, cutThroughItems)) {
			return false;
		}
		LOGGER.warn("Ignoring unsupported cut through item scope: {}", cutThroughItems);
		cutThroughItems = CUT_THROUGH_ITEMS_SWORD;
		return true;
	}

	// 修正剑穿草打生物的植物范围。
	private boolean sanitizeCutThroughPlants() {
		if (isSupportedValue(SUPPORTED_CUT_THROUGH_PLANTS, cutThroughPlants)) {
			return false;
		}
		LOGGER.warn("Ignoring unsupported cut through plant scope: {}", cutThroughPlants);
		cutThroughPlants = CUT_THROUGH_PLANTS_ALL;
		return true;
	}

	// 修正夜视淡出曲线。
	private boolean sanitizeNightVisionCurve() {
		if (isSupportedValue(SUPPORTED_NIGHT_VISION_CURVES, nightVisionFadeOutCurve)) {
			return false;
		}
		LOGGER.warn("Ignoring unsupported night vision curve: {}", nightVisionFadeOutCurve);
		nightVisionFadeOutCurve = NIGHT_VISION_CURVE_SMOOTH;
		return true;
	}

	// 修正鞘翅烟花最短间隔。
	private boolean sanitizeElytraFireworkTicks() {
		int clamped = Math.max(MIN_ELYTRA_FIREWORK_TICKS,
			Math.min(MAX_ELYTRA_FIREWORK_TICKS, elytraFireworkCooldownTicks));
		if (clamped == elytraFireworkCooldownTicks) {
			return false;
		}
		LOGGER.warn("Clamping elytraFireworkCooldownTicks from {} to {}", elytraFireworkCooldownTicks, clamped);
		elytraFireworkCooldownTicks = clamped;
		return true;
	}

	// 修正单层基岩标记数量，手工改坏配置时夹回允许范围。
	private boolean sanitizeBedrockMarkerLimit() {
		int clamped = Math.max(MIN_BEDROCK_MARKERS,
			Math.min(MAX_BEDROCK_MARKERS, highlightSingleLayerBedrockMaxMarkers));
		if (clamped == highlightSingleLayerBedrockMaxMarkers) {
			return false;
		}
		LOGGER.warn("Clamping highlightSingleLayerBedrockMaxMarkers from {} to {}",
			highlightSingleLayerBedrockMaxMarkers, clamped);
		highlightSingleLayerBedrockMaxMarkers = clamped;
		return true;
	}

	// 修正自定义轮廓线宽：统一走 0.5 步进与范围夹紧工具。
	private boolean sanitizeBlockOutlineWidth() {
		double clamped = clampBlockOutlineWidth(blockOutlineWidth);
		if (Double.compare(clamped, blockOutlineWidth) == 0) {
			return false;
		}
		LOGGER.warn("Correcting blockOutlineWidth from {} to {}", blockOutlineWidth, clamped);
		blockOutlineWidth = clamped;
		return true;
	}

	// 修正彩虹/呼吸动画速度。
	private boolean sanitizeBlockOutlineAnimationSpeed() {
		int clamped = Math.max(MIN_BLOCK_OUTLINE_ANIMATION_SPEED,
			Math.min(MAX_BLOCK_OUTLINE_ANIMATION_SPEED, blockOutlineAnimationSpeed));
		if (clamped == blockOutlineAnimationSpeed) {
			return false;
		}
		LOGGER.warn("Clamping blockOutlineAnimationSpeed from {} to {}", blockOutlineAnimationSpeed, clamped);
		blockOutlineAnimationSpeed = clamped;
		return true;
	}

	// 修正不合法的轮廓调色板。
	private boolean sanitizeBlockOutlinePalette() {
		if (isSupportedValue(SUPPORTED_BLOCK_OUTLINE_PALETTES, blockOutlinePalette)) {
			return false;
		}
		LOGGER.warn("Ignoring unsupported block outline palette: {}", blockOutlinePalette);
		blockOutlinePalette = BLOCK_OUTLINE_PALETTE_WHITE;
		return true;
	}

	// 修正不合法的轮廓彩色模式。
	private boolean sanitizeBlockOutlineColorMode() {
		if (isSupportedValue(SUPPORTED_BLOCK_OUTLINE_COLOR_MODES, blockOutlineColorMode)) {
			return false;
		}
		LOGGER.warn("Ignoring unsupported block outline color mode: {}", blockOutlineColorMode);
		blockOutlineColorMode = BLOCK_OUTLINE_MODE_PALETTE;
		return true;
	}

	// 配置字符串为 null 时 List.of(...).contains(null) 会抛 NPE，统一用这个入口判断。
	private static boolean isSupportedValue(List<String> supported, String value) {
		return value != null && supported.contains(value);
	}

	// 修正不合法的语言代码，回退到中文。
	private boolean sanitizeLanguage() {
		if (isSupportedValue(SUPPORTED_LANGUAGES, language)) {
			return false;
		}
		LOGGER.warn("Ignoring unsupported language in config: {}", language);
		language = "zh_cn";
		return true;
	}

	// 清洗单个 ID 列表（物品或实体）：补全命名空间、去重、丢弃非法条目，返回新列表。
	private static List<String> sanitizeItemList(List<String> itemIds) {
		if (itemIds == null) {
			return new ArrayList<>();
		}
		Set<String> uniqueItems = new LinkedHashSet<>();
		for (String itemId : itemIds) {
			String normalized = normalize(itemId);
			if (normalized == null) {
				LOGGER.warn("Ignoring invalid item ID in config: {}", itemId);
				continue;
			}
			if (!uniqueItems.add(normalized)) {
				LOGGER.warn("Ignoring duplicate item ID in config: {}", normalized);
			}
		}
		return new ArrayList<>(uniqueItems);
	}

	private void rebuildCache() {
		restrictedCache = new HashSet<>(restrictedOffhandItems);
		mainHandItemsCache = new HashSet<>(mainHandUseCooldownItems);
		offhandItemsCache = new HashSet<>(offhandUseCooldownItems);
		preset1EntitiesCache = new HashSet<>(highlightPreset1Entities);
		preset2EntitiesCache = new HashSet<>(highlightPreset2Entities);
		preset3EntitiesCache = new HashSet<>(highlightPreset3Entities);
		netherPortalBlacklistCache = new HashSet<>(netherPortalTeleportBlacklist);
		endPortalBlacklistCache = new HashSet<>(endPortalTeleportBlacklist);
		endGatewayBlacklistCache = new HashSet<>(endGatewayTeleportBlacklist);
		attackRangeEntitiesCache = new HashSet<>(attackRangeEntities);
		useItemSlowdownExceptionCache = new HashSet<>(useItemSlowdownExceptions);
		cacheDirty = false;
		entityHighlightCache = null;
	}

	// 惰性重建：批量修改期间只标记过期，真正查询时重建一次。
	private void ensureCache() {
		if (cacheDirty || restrictedCache == null) {
			rebuildCache();
		}
	}

	// 按字段名读取布尔开关（命令注册与 MaLiLib 同步共用）。
	public boolean getBoolean(String name) {
		return (Boolean) read(name);
	}

	// 命令改值后请求一次选项同步：真正的同步由客户端 tick 执行。
	public void requestOptionSync() {
		optionSyncRequested.set(true);
	}

	// 客户端 tick 消费同步请求：true 表示需要把配置值同步回 MaLiLib 选项与界面。
	public boolean consumeOptionSyncRequest() {
		return optionSyncRequested.compareAndSet(true, false);
	}

	// 写入均加锁：/tweakify 命令在集成服务端线程执行，客户端线程同时在读配置。
	public synchronized void setBoolean(String name, boolean value) {
		write(name, value);
	}

	// 按字段名读取字符串项（语言代码、快捷键）。
	public String getString(String name) {
		return (String) read(name);
	}

	public synchronized void setString(String name, String value) {
		write(name, value);
	}

	// 按字段名读取整数项（掉落物消失时间）。
	public int getInteger(String name) {
		return (Integer) read(name);
	}

	public synchronized void setInteger(String name, int value) {
		write(name, value);
	}

	// 按字段名读取浮点项（轮廓线宽）。
	public double getDouble(String name) {
		return (Double) read(name);
	}

	// 写入时对线宽按配置步进取整并夹紧；其它 double 字段保持原值。
	public synchronized void setDouble(String name, double value) {
		if (BLOCK_OUTLINE_WIDTH_FIELD.equals(name)) {
			value = clampBlockOutlineWidth(value);
		}
		write(name, value);
	}

	// 按字段名读取 ID 列表；写入时统一走清洗，与配置文件读取得到相同的规范化结果。
	public List<String> getStringList(String name) {
		return List.copyOf(asList(read(name)));
	}

	// 写入时统一换成新的不可变列表（写时复制）：渲染线程正在遍历旧列表也不会看到中间状态。
	public synchronized void setStringList(String name, List<String> values) {
		write(name, List.copyOf(sanitizeItemList(values)));
		// 列表内容变了，命中判定用的缓存集合延迟到下次查询时统一重建。
		cacheDirty = true;
	}

	// 字段是否存在：命令表与 MaLiLib 选项名都以字段名为键，注册前先校验。
	public static boolean hasField(String name) {
		return CONFIG_FIELDS.containsKey(name);
	}

	// 全部配置字段名（声明顺序）：选项注册与目录校验用它保证“字段 ⟷ 选项”一一对应。
	public static List<String> configFieldNames() {
		return List.copyOf(CONFIG_FIELDS.keySet());
	}

	// 字段是否为布尔开关：配置目录用它校验“命令项必须是开关”，避免注册出无法切换的命令。
	public static boolean isBooleanField(String name) {
		Field field = CONFIG_FIELDS.get(name);
		return field != null && field.getType() == boolean.class;
	}

	private Object read(String name) {
		return readField(requireField(name));
	}

	private Object readField(Field field) {
		try {
			return field.get(this);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException("Cannot read config field " + field.getName(), e);
		}
	}

	private void write(String name, Object value) {
		try {
			requireField(name).set(this, value);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException("Cannot write config field " + name, e);
		}
		// 开关或列表变化都可能推翻高亮判定，按实体类型的缓存直接失效。
		entityHighlightCache = null;
	}

	private static Field requireField(String name) {
		Field field = CONFIG_FIELDS.get(name);
		if (field == null) {
			throw new IllegalArgumentException("Unknown config field: " + name);
		}
		return field;
	}

	@SuppressWarnings("unchecked")
	private static List<String> asList(Object value) {
		return value == null ? List.of() : (List<String>) value;
	}

	// 以下 8 组列表统一走通用读写入口：清洗、写时复制与缓存失效只实现一次，避免各写一遍后行为漂移。
	public List<String> getRestrictedOffhandItems() {
		return getStringList(RESTRICTED_OFFHAND_ITEMS_FIELD);
	}

	public synchronized void replaceRestrictedOffhandItems(List<String> itemIds) {
		setStringList(RESTRICTED_OFFHAND_ITEMS_FIELD, itemIds);
	}

	public List<String> getMainHandUseCooldownItems() {
		return getStringList(MAIN_HAND_USE_COOLDOWN_ITEMS_FIELD);
	}

	public synchronized void replaceMainHandUseCooldownItems(List<String> itemIds) {
		setStringList(MAIN_HAND_USE_COOLDOWN_ITEMS_FIELD, itemIds);
	}

	public List<String> getOffhandUseCooldownItems() {
		return getStringList(OFFHAND_USE_COOLDOWN_ITEMS_FIELD);
	}

	public synchronized void replaceOffhandUseCooldownItems(List<String> itemIds) {
		setStringList(OFFHAND_USE_COOLDOWN_ITEMS_FIELD, itemIds);
	}

	public List<String> getHighlightPreset1Entities() {
		return getStringList("highlightPreset1Entities");
	}

	public synchronized void replaceHighlightPreset1Entities(List<String> entityIds) {
		setStringList("highlightPreset1Entities", entityIds);
	}

	public List<String> getHighlightPreset2Entities() {
		return getStringList("highlightPreset2Entities");
	}

	public synchronized void replaceHighlightPreset2Entities(List<String> entityIds) {
		setStringList("highlightPreset2Entities", entityIds);
	}

	public List<String> getHighlightPreset3Entities() {
		return getStringList("highlightPreset3Entities");
	}

	public synchronized void replaceHighlightPreset3Entities(List<String> entityIds) {
		setStringList("highlightPreset3Entities", entityIds);
	}

	public List<String> getNetherPortalTeleportBlacklist() {
		return getStringList("netherPortalTeleportBlacklist");
	}

	public synchronized void replaceNetherPortalTeleportBlacklist(List<String> entityIds) {
		setStringList("netherPortalTeleportBlacklist", entityIds);
	}

	public List<String> getEndPortalTeleportBlacklist() {
		return getStringList("endPortalTeleportBlacklist");
	}

	public synchronized void replaceEndPortalTeleportBlacklist(List<String> entityIds) {
		setStringList("endPortalTeleportBlacklist", entityIds);
	}

	public List<String> getEndGatewayTeleportBlacklist() {
		return getStringList("endGatewayTeleportBlacklist");
	}

	public synchronized void replaceEndGatewayTeleportBlacklist(List<String> entityIds) {
		setStringList("endGatewayTeleportBlacklist", entityIds);
	}

	public List<String> getAttackRangeEntities() {
		return getStringList("attackRangeEntities");
	}

	// 实体是否在地狱门传送黑名单中（命中则强制拦截）。
	public boolean isNetherPortalTeleportBlacklisted(EntityType<?> type) {
		ensureCache();
		Identifier id = entityTypeId(type);
		return id != null && netherPortalBlacklistCache.contains(id.toString());
	}

	// 实体是否在末地门传送黑名单中（命中则强制拦截）。
	public boolean isEndPortalTeleportBlacklisted(EntityType<?> type) {
		ensureCache();
		Identifier id = entityTypeId(type);
		return id != null && endPortalBlacklistCache.contains(id.toString());
	}

	// 实体是否在末地折跃门传送黑名单中。
	public boolean isEndGatewayTeleportBlacklisted(EntityType<?> type) {
		ensureCache();
		Identifier id = entityTypeId(type);
		return id != null && endGatewayBlacklistCache.contains(id.toString());
	}

	// 黑名单是否为空（命中缓存）：
	// 传送判定在实体处于门内时每 tick 调用，不能每次都经过反射读列表再复制。
	public boolean isNetherPortalTeleportBlacklistEmpty() {
		ensureCache();
		return netherPortalBlacklistCache.isEmpty();
	}

	public boolean isEndPortalTeleportBlacklistEmpty() {
		ensureCache();
		return endPortalBlacklistCache.isEmpty();
	}

	public boolean isEndGatewayTeleportBlacklistEmpty() {
		ensureCache();
		return endGatewayBlacklistCache.isEmpty();
	}

	// 攻击范围实体过滤：列表为空表示不过滤（作用于全部生物与玩家）。
	// 命中集合现已缓存，扫描时不再每两天重建一次 HashSet。
	public boolean isAttackRangeEntityAllowed(EntityType<?> type) {
		ensureCache();
		if (attackRangeEntitiesCache.isEmpty()) {
			return true;
		}
		Identifier id = entityTypeId(type);
		return id != null && attackRangeEntitiesCache.contains(id.toString());
	}

	// 使用物品减速的例外：命中列表的物品保持原版减速。
	public boolean isUseItemSlowdownException(Item item) {
		if (item == null || item == Items.AIR) {
			return false;
		}
		ensureCache();
		if (useItemSlowdownExceptionCache.isEmpty()) {
			return false;
		}
		Identifier id = idOf(item);
		return id != null && useItemSlowdownExceptionCache.contains(id.toString());
	}

	// 运行期快照：按字段名保存全部非 transient 配置项，写盘失败时整体回滚。
	// 字段由反射枚举，新增开关无需再手工维护一份拷贝清单。
	public synchronized Snapshot snapshot() {
		Map<String, Object> values = new LinkedHashMap<>();
		for (Map.Entry<String, Field> entry : CONFIG_FIELDS.entrySet()) {
			// 字段表里已经缓存了 Field，直接读可以省掉 read(name) 的一次 Map 查找。
			Object value = readField(entry.getValue());
			values.put(entry.getKey(), value instanceof List<?> list ? List.copyOf(list) : value);
		}
		return new Snapshot(values);
	}

	// 只回滚到快照，不写盘：异步写盘失败后由客户端线程调用。
	public synchronized void restoreSnapshot(Snapshot snapshot) {
		for (Map.Entry<String, Object> entry : snapshot.values.entrySet()) {
			Object value = entry.getValue();
			write(entry.getKey(), value instanceof List<?> list ? new ArrayList<>(list) : value);
		}
		cacheDirty = true;
	}

	// 档案里可应用的字段：排除版本号与已废弃的字段名。
	// 命令层显示的“已应用 N 项”也用它计数，避免把被跳过的字段算进去。
	public static boolean isProfileField(String name) {
		return !CONFIG_VERSION_FIELD.equals(name) && CONFIG_FIELDS.containsKey(name);
	}

	// 按字段名批量应用另一份配置的值（档案加载用）：
	// 只覆盖 fieldNames 里列出的字段，因此旧档案缺少的新字段会保持当前值，
	// 档案里已废弃的字段名会被静默跳过，版本号也不覆盖。
	// 档案可能被手工改坏：列表里的 null/非法 ID、越界数值、非法枚举值
	// 都在这里复用 load() 的同一套收敛逻辑，避免把脏值带进运行期。
	public synchronized void applyFrom(TweakifyModConfig source, Set<String> fieldNames) {
		for (String name : fieldNames) {
			if (!isProfileField(name)) {
				continue;
			}
			Object value = source.read(name);
			if (value instanceof List<?> list) {
				@SuppressWarnings("unchecked")
				List<String> raw = (List<String>) list;
				write(name, List.copyOf(sanitizeItemList(raw)));
			} else if (value != null) {
				// null 时保持当前值：把列表/字符串字段置空会让下游每 tick 调用 NPE。
				write(name, value);
			}
		}
		// 与 load() 同一套收敛：数值夹紧 + 枚举/语言回退 + 热键 null -> ""
		sanitizeAllFields();
		sanitizeHotkeys();
		cacheDirty = true;
	}

	// 命令专用：把「读旧值 → 改值 → 写盘 → 失败回滚」全部压在同一个锁内。
	// 客户端线程的 setter 也持有这把锁，因此不会出现“快照与回滚之间被插入修改”的窗口；
	// 回滚范围也只有本次命令改动的那一个字段，不会覆盖其它字段的并发修改。
	// mutation 返回回滚动作；返回 null 表示字段没有真正变化，此时不写盘。
	public synchronized ApplyResult applyAndSave(Supplier<Runnable> mutation) {
		Runnable rollback = mutation.get();
		if (rollback == null) {
			return ApplyResult.UNCHANGED;
		}
		if (ConfigStorage.save(this, configPathOrDefault())) {
			return ApplyResult.SAVED;
		}
		rollback.run();
		cacheDirty = true;
		return ApplyResult.SAVE_FAILED;
	}

	public enum ApplyResult {
		// 字段值没有变化，未写盘。
		UNCHANGED,
		// 已改值并成功写盘。
		SAVED,
		// 写盘失败，已回滚本次改动。
		SAVE_FAILED
	}

	public static boolean isRegisteredItem(String id) {
		String normalized = normalize(id);
		if (normalized == null) {
			return false;
		}
		Identifier identifier = Identifier.tryParse(normalized);
		return identifier != null && BuiltInRegistries.ITEM.containsKey(identifier);
	}

	// 副手物品是否在受限列表中。
	public boolean isOffhandRestricted(Item item) {
		if (item == null || item == Items.AIR) {
			return false;
		}
		ensureCache();
		Identifier id = idOf(item);
		return id != null && restrictedCache.contains(id.toString());
	}

	// 物品是否在主手使用冷却列表中。
	public boolean isMainHandUseRestricted(Item item) {
		if (item == null || item == Items.AIR) {
			return false;
		}
		ensureCache();
		Identifier id = idOf(item);
		return id != null && mainHandItemsCache.contains(id.toString());
	}

	// 物品是否在副手使用冷却列表中。
	public boolean isOffhandUseRestricted(Item item) {
		if (item == null || item == Items.AIR) {
			return false;
		}
		ensureCache();
		Identifier id = idOf(item);
		return id != null && offhandItemsCache.contains(id.toString());
	}

	// 实体是否应被自定义高亮：总开关开启、当前预设开启且实体 ID 命中列表。
	public boolean shouldHighlightEntity(EntityType<?> type) {
		invalidateHighlightCacheIfStateChanged();
		if (!entityHighlight || type == null) {
			return false;
		}
		ensureCache();
		Map<EntityType<?>, Boolean> cache = entityHighlightCache;
		if (cache == null) {
			cache = new IdentityHashMap<>();
			entityHighlightCache = cache;
		}
		Boolean cached = cache.get(type);
		if (cached == null) {
			Set<String> active = activeHighlightEntities();
			Identifier id = active == null || active.isEmpty() ? null : entityTypeId(type);
			cached = id != null && active.contains(id.toString());
			cache.put(type, cached);
		}
		return cached;
	}

	// 总开关或预设开关变化（包括直接赋值）时丢弃按实体类型的缓存。
	private void invalidateHighlightCacheIfStateChanged() {
		if (cachedEntityHighlight == entityHighlight
			&& cachedHighlightPreset1 == highlightPreset1
			&& cachedHighlightPreset2 == highlightPreset2
			&& cachedHighlightPreset3 == highlightPreset3) {
			return;
		}
		cachedEntityHighlight = entityHighlight;
		cachedHighlightPreset1 = highlightPreset1;
		cachedHighlightPreset2 = highlightPreset2;
		cachedHighlightPreset3 = highlightPreset3;
		entityHighlightCache = null;
	}

	// 互斥后最多只有一个预设开启；手工改坏配置时按 1→2→3 的优先级取第一个。
	private Set<String> activeHighlightEntities() {
		ensureCache();
		if (highlightPreset1) {
			return preset1EntitiesCache;
		}
		if (highlightPreset2) {
			return preset2EntitiesCache;
		}
		if (highlightPreset3) {
			return preset3EntitiesCache;
		}
		return null;
	}

	private static Identifier entityTypeId(EntityType<?> type) {
		try {
			return BuiltInRegistries.ENTITY_TYPE.getKey(type);
		} catch (Exception | LinkageError e) {
			// 注册表未初始化（如单测环境）时 BuiltInRegistries 会抛 Error，不能让它穿透。
			return null;
		}
	}

	// 添加一个物品 ID，返回是否新增了（已存在返回 false）。
	// 写时复制：列表整体替换为新实例，渲染线程不会遍历到被就地修改的旧列表。
	public synchronized boolean addItem(String id) {
		String normalized = normalize(id);
		if (normalized == null || restrictedOffhandItems.contains(normalized)) {
			return false;
		}
		List<String> updated = new ArrayList<>(restrictedOffhandItems);
		updated.add(normalized);
		restrictedOffhandItems = List.copyOf(updated);
		cacheDirty = true;
		return true;
	}

	// 移除一个物品 ID，返回是否移除了。
	public synchronized boolean removeItem(String id) {
		String normalized = normalize(id);
		if (normalized == null || !restrictedOffhandItems.contains(normalized)) {
			return false;
		}
		List<String> updated = new ArrayList<>(restrictedOffhandItems);
		updated.remove(normalized);
		restrictedOffhandItems = List.copyOf(updated);
		cacheDirty = true;
		return true;
	}

	// 重置为默认光源物品列表，返回数量。
	public synchronized int resetToLightSources() {
		restrictedOffhandItems = List.copyOf(defaultLightSourceItemIds());
		cacheDirty = true;
		return restrictedOffhandItems.size();
	}

	private static String normalize(String id) {
		if (id == null || id.isBlank()) {
			return null;
		}
		Identifier parsed = Identifier.tryParse(id.contains(":") ? id : "minecraft:" + id);
		return parsed == null ? null : parsed.toString();
	}

	private static Identifier idOf(Item item) {
		return DefaultItemLists.idOf(item);
	}

	// 不可变快照：仅用于写盘失败后的整体回滚，不对外暴露具体字段。
	public static final class Snapshot {
		private final Map<String, Object> values;

		private Snapshot(Map<String, Object> values) {
			this.values = values;
		}
	}

	// 默认列表由 DefaultItemLists 扫描并缓存；保留此入口供配置项初始化与测试使用。
	public static List<String> defaultLightSourceItemIds() {
		return DefaultItemLists.lightSourceItemIds();
	}

	public static List<String> defaultInteractableItemIds() {
		return DefaultItemLists.interactableItemIds();
	}
}
