package com.gtzc.tweakify.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.gtzc.tweakify.util.AsyncFileWriter;
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
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class TweakifyModConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger("tweakify-mod-config");

	// 反射字段表（字段名 == JSON 键名）：按名字读写配置，使命令注册、MaLiLib 双向同步与
	// 写盘回滚共用同一份字段清单，新增开关时无需再同步修改多处拷贝清单。
	// 字段可以声明在本类上，也可以声明在 facade 持有的分区对象里：索引只保存 Field 引用，
	// 读写时按分区字段解引用 owner，因此分区拆分不改变字段表的键与相对顺序。
	private static final Map<String, ConfigFieldRef> CONFIG_FIELDS = indexConfigFields();

	private static Map<String, ConfigFieldRef> indexConfigFields() {
		Map<String, ConfigFieldRef> fields = new LinkedHashMap<>();
		for (Field field : TweakifyModConfig.class.getDeclaredFields()) {
			if (isIndexSkipped(field)) {
				continue;
			}
			if (ConfigSection.class.isAssignableFrom(field.getType())) {
				indexSection(field.getType(), field, fields);
			} else {
				addConfigField(fields, new ConfigFieldRef(field.getName(), null, field));
			}
		}
		return Collections.unmodifiableMap(fields);
	}

	// 仅索引一层分区：分区不嵌套，避免字段归属难以追踪。
	private static void indexSection(Class<?> type, Field sectionField, Map<String, ConfigFieldRef> fields) {
		sectionField.setAccessible(true);
		for (Field field : type.getDeclaredFields()) {
			if (isIndexSkipped(field)) {
				continue;
			}
			addConfigField(fields, new ConfigFieldRef(field.getName(), sectionField, field));
		}
	}

	private static boolean isIndexSkipped(Field field) {
		int modifiers = field.getModifiers();
		return Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || field.isSynthetic();
	}

	// 重名直接让类初始化失败：否则后注册的字段会静默覆盖前一个，配置文件与界面一起丢项。
	private static void addConfigField(Map<String, ConfigFieldRef> fields, ConfigFieldRef ref) {
		ref.valueField().setAccessible(true);
		if (fields.putIfAbsent(ref.name(), ref) != null) {
			throw new IllegalStateException("Duplicate config field: " + ref.name());
		}
	}

	// 配置文件版本：字段语义发生变化时递增，并在 load() 里按旧版本做迁移。
	// 版本 4：定位栏单名单+模式改为 included/excluded 双名单，描边粗细改为四档代码。
	// 版本 5：盔甲架手臂并入「更好的盔甲架」父开关，旧配置自动启用父开关。
	public static final int CURRENT_CONFIG_VERSION = 5;

	// 同步保存等待写盘完成的最长时间：超时后任务仍会按队列顺序继续执行，不会取消。
	private static final long SAVE_TIMEOUT_MILLIS = 2000L;

	// 模组界面支持的语言代码，与 assets/tweakify-mod/lang 下的文件名一致。
	public static final List<String> SUPPORTED_LANGUAGES = List.of("zh_cn", "en_us", "ru_ru", "fr_fr");

	// 快捷键字段的统一前缀：字段名为 hotkey + 配置项名首字母大写。
	// 选项注册（TweakifyModConfigOptions）与配置清洗共用，避免两处硬编码字符串漂移。
	public static final String HOTKEY_FIELD_PREFIX = "hotkey";

	// 三个“空列表 = 不做限制”的物品列表字段名：配置文件缺少这些键时必须补回默认值。
	public static final String RESTRICTED_OFFHAND_ITEMS_FIELD = "restrictedOffhandItems";
	public static final String MAIN_HAND_USE_COOLDOWN_ITEMS_FIELD = "mainHandUseCooldownItems";
	public static final String OFFHAND_USE_COOLDOWN_ITEMS_FIELD = "offhandUseCooldownItems";

	// 自定义物品组定义列表：内容不是单个 ID，而是“标签页|组名|包含|排除|等价”的整行，
	// 因此不参与物品 ID 规范化（否则整行会被当成非法 ID 丢掉）。
	public static final String CUSTOM_ITEM_GROUPS_FIELD = "customItemGroups";

	// 定位栏玩家过滤名单字段名：与参考实现一致拆成“包含/排除”两个名单（内容是玩家名，非物品 ID）。
	public static final String LOCATOR_HEAD_INCLUDED_PLAYERS_FIELD = "locatorBarHeadIncludedPlayers";
	public static final String LOCATOR_HEAD_EXCLUDED_PLAYERS_FIELD = "locatorBarHeadExcludedPlayers";

	// 盔甲架姿态预设列表字段名：内容是姿态代码（sitting 等），不参与物品 ID 规范化。
	public static final String ARMOR_STAND_POSE_PRESETS_FIELD = "armorStandPosePresets";

	// 「更好的盔甲架」父开关字段名：v5 迁移要把旧配置的盔甲架手臂映射到它。
	public static final String BETTER_ARMOR_STANDS_FIELD = "betterArmorStands";

	// 配置版本字段名：迁移步骤专用，档案加载时不复制（档案不应改写当前版本号）。
	private static final String CONFIG_VERSION_FIELD = "configVersion";

	// 这些字段的修正在 load() 的前置迁移步骤完成，不要求登记字段级校验器。
	private static final Set<String> MIGRATION_FIELDS = Set.of(CONFIG_VERSION_FIELD);

	// 字段级校验器已随字段搬到各分区（见 ConfigSection 与各 XxxConfig），
	// 每个分区在构造时自行断言覆盖度，facade 不再维护校验器表。

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

	// 物品数量数字缩放百分比：50 ~ 200，默认 100 保持原版字号。
	public static final int MIN_ITEM_COUNT_SCALE = 50;
	public static final int MAX_ITEM_COUNT_SCALE = 200;
	public static final int DEFAULT_ITEM_COUNT_SCALE = 100;

	// 进度界面标签间距与固定标签间距（像素）：0 = 原版紧凑排列。
	public static final int MIN_ADVANCEMENT_TAB_SPACING = 0;
	public static final int MAX_ADVANCEMENT_TAB_SPACING = 8;
	public static final int DEFAULT_ADVANCEMENT_TAB_SPACING = 0;
	public static final int DEFAULT_ADVANCEMENT_PINNED_TAB_SPACING = 0;

	// 耐久文字的显示内容：耐久百分比 / 剩余耐久数值。
	public static final String DURABILITY_DISPLAY_PERCENT = "percent";
	public static final String DURABILITY_DISPLAY_VALUE = "value";
	public static final List<String> SUPPORTED_DURABILITY_DISPLAYS = List.of(
		DURABILITY_DISPLAY_PERCENT, DURABILITY_DISPLAY_VALUE);

	// 耐久文字的配色：auto 按剩余耐久分档（绿/白/黄/红），其余固定单色。
	public static final String DURABILITY_COLOR_MODE_AUTO = "auto";
	public static final String DURABILITY_COLOR_MODE_WHITE = "white";
	public static final String DURABILITY_COLOR_MODE_YELLOW = "yellow";
	public static final String DURABILITY_COLOR_MODE_RED = "red";
	public static final List<String> SUPPORTED_DURABILITY_COLOR_MODES = List.of(
		DURABILITY_COLOR_MODE_AUTO, DURABILITY_COLOR_MODE_WHITE,
		DURABILITY_COLOR_MODE_YELLOW, DURABILITY_COLOR_MODE_RED);

	// 手持物品信息（参考 held-item-info）：行数、行宽、排版与淡出时长的范围与默认值。
	// 默认值保持与参考实现一致，方便按原模组的观感直接使用。
	public static final int MIN_HELD_ITEM_INFO_LINES = 0;
	public static final int MAX_HELD_ITEM_INFO_LINES = 40;
	public static final int DEFAULT_HELD_ITEM_INFO_MAX_LINES = 6;
	public static final int MIN_HELD_ITEM_INFO_LINE_LENGTH = 1;
	public static final int MAX_HELD_ITEM_INFO_LINE_LENGTH = 240;
	public static final int DEFAULT_HELD_ITEM_INFO_MAX_LINE_LENGTH = 48;
	public static final int DEFAULT_HELD_ITEM_INFO_MAX_COMMAND_LINES = 2;
	public static final int DEFAULT_HELD_ITEM_INFO_MAX_LORE_LINES = 3;
	public static final int MIN_HELD_ITEM_INFO_LINE_HEIGHT = 1;
	public static final int MAX_HELD_ITEM_INFO_LINE_HEIGHT = 40;
	public static final int DEFAULT_HELD_ITEM_INFO_LINE_HEIGHT = 8;
	public static final int MIN_HELD_ITEM_INFO_NAME_SPACING = 0;
	public static final int MAX_HELD_ITEM_INFO_NAME_SPACING = 40;
	public static final int DEFAULT_HELD_ITEM_INFO_NAME_SPACING = 2;
	public static final int MIN_HELD_ITEM_INFO_VERTICAL_OFFSET = -40;
	public static final int MAX_HELD_ITEM_INFO_VERTICAL_OFFSET = 40;
	public static final int DEFAULT_HELD_ITEM_INFO_VERTICAL_OFFSET = 0;
	public static final double MIN_HELD_ITEM_INFO_OFFSET_PER_LINE = 0.0;
	public static final double MAX_HELD_ITEM_INFO_OFFSET_PER_LINE = 1.0;
	public static final double DEFAULT_HELD_ITEM_INFO_OFFSET_PER_LINE = 0.3334;
	public static final double MIN_HELD_ITEM_INFO_FADE_DURATION = 0.0;
	public static final double MAX_HELD_ITEM_INFO_FADE_DURATION = 20.0;
	public static final double DEFAULT_HELD_ITEM_INFO_BASE_FADE = 2.0;
	public static final double MAX_HELD_ITEM_INFO_FADE_PER_LINE = 10.0;
	public static final double DEFAULT_HELD_ITEM_INFO_FADE_PER_LINE = 0.2;

	// 手持物品信息的背景：vanilla（跟随文字背景无障碍选项）/ always / never。
	public static final String HELD_ITEM_INFO_BACKGROUND_VANILLA = "vanilla";
	public static final String HELD_ITEM_INFO_BACKGROUND_ALWAYS = "always";
	public static final String HELD_ITEM_INFO_BACKGROUND_NEVER = "never";
	public static final List<String> SUPPORTED_HELD_ITEM_INFO_BACKGROUNDS = List.of(
		HELD_ITEM_INFO_BACKGROUND_VANILLA, HELD_ITEM_INFO_BACKGROUND_ALWAYS,
		HELD_ITEM_INFO_BACKGROUND_NEVER);

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

	// 存档游玩时长显示格式：hours 小数小时 / clock 时:分 / duration 小时与分钟。
	// 只影响存档列表右上角的文本，不改变读取的 play_time 数据。
	public static final String SAVE_PLAY_TIME_FORMAT_HOURS = "hours";
	public static final String SAVE_PLAY_TIME_FORMAT_CLOCK = "clock";
	public static final String SAVE_PLAY_TIME_FORMAT_DURATION = "duration";
	public static final List<String> SUPPORTED_SAVE_PLAY_TIME_FORMATS = List.of(
		SAVE_PLAY_TIME_FORMAT_HOURS, SAVE_PLAY_TIME_FORMAT_CLOCK, SAVE_PLAY_TIME_FORMAT_DURATION);

	// 攻击范围渲染方式：蓝色线框 / 半透明实心。
	public static final String ATTACK_RANGE_RENDER_WIREFRAME = "wireframe";
	public static final String ATTACK_RANGE_RENDER_FILLED = "filled";
	public static final List<String> SUPPORTED_ATTACK_RANGE_RENDER_MODES = List.of(
		ATTACK_RANGE_RENDER_WIREFRAME, ATTACK_RANGE_RENDER_FILLED);

	// 攻击范围颜色调色板：只决定 RGB，透明度沿用各模式的固定档位。
	public static final String ATTACK_RANGE_PALETTE_BLUE = "blue";
	public static final String ATTACK_RANGE_PALETTE_RED = "red";
	public static final String ATTACK_RANGE_PALETTE_GREEN = "green";
	public static final String ATTACK_RANGE_PALETTE_YELLOW = "yellow";
	public static final String ATTACK_RANGE_PALETTE_CYAN = "cyan";
	public static final String ATTACK_RANGE_PALETTE_MAGENTA = "magenta";
	public static final String ATTACK_RANGE_PALETTE_WHITE = "white";
	public static final List<String> SUPPORTED_ATTACK_RANGE_PALETTES = List.of(
		ATTACK_RANGE_PALETTE_BLUE, ATTACK_RANGE_PALETTE_RED, ATTACK_RANGE_PALETTE_GREEN,
		ATTACK_RANGE_PALETTE_YELLOW, ATTACK_RANGE_PALETTE_CYAN, ATTACK_RANGE_PALETTE_MAGENTA,
		ATTACK_RANGE_PALETTE_WHITE);

	// 更好的 F2 截图输出格式。
	public static final String SCREENSHOT_FORMAT_PNG = "png";
	public static final String SCREENSHOT_FORMAT_JPG = "jpg";
	public static final String SCREENSHOT_FORMAT_TGA = "tga";
	public static final String SCREENSHOT_FORMAT_BMP = "bmp";
	public static final List<String> SUPPORTED_SCREENSHOT_FORMATS = List.of(
		SCREENSHOT_FORMAT_PNG, SCREENSHOT_FORMAT_JPG, SCREENSHOT_FORMAT_TGA,
		SCREENSHOT_FORMAT_BMP);

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

	// 单层基岩高亮的扫描半径与标记颜色；半径上限与 BlockMarkerScanner 的数组容量一致。
	public static final int MIN_BEDROCK_SCAN_RADIUS = 8;
	public static final int MAX_BEDROCK_SCAN_RADIUS = 32;
	public static final int DEFAULT_BEDROCK_SCAN_RADIUS = 24;
	public static final String BEDROCK_COLOR_GOLD = "gold";
	public static final String BEDROCK_COLOR_WHITE = "white";
	public static final String BEDROCK_COLOR_RED = "red";
	public static final String BEDROCK_COLOR_GREEN = "green";
	public static final String BEDROCK_COLOR_BLUE = "blue";
	public static final String BEDROCK_COLOR_CYAN = "cyan";
	public static final List<String> SUPPORTED_BEDROCK_COLORS = List.of(
		BEDROCK_COLOR_GOLD, BEDROCK_COLOR_WHITE, BEDROCK_COLOR_RED,
		BEDROCK_COLOR_GREEN, BEDROCK_COLOR_BLUE, BEDROCK_COLOR_CYAN);

	// 屏障可见的标记数量限制：默认 256 与旧版固定上限一致。
	public static final int MIN_BARRIER_MARKERS = 16;
	public static final int MAX_BARRIER_MARKERS = 256;
	public static final int DEFAULT_BARRIER_MARKERS = 256;

	// 屏障可见的扫描半径（格）与同一方块两次标记粒子的间隔（tick）。
	// 上限 32 与 BlockMarkerScanner 的数组容量一致，避免改配置后越界。
	public static final int MIN_BARRIER_SCAN_RADIUS = 8;
	public static final int MAX_BARRIER_SCAN_RADIUS = 32;
	public static final int DEFAULT_BARRIER_SCAN_RADIUS = 24;
	public static final int MIN_BARRIER_PARTICLE_INTERVAL = 20;
	public static final int MAX_BARRIER_PARTICLE_INTERVAL = 200;
	public static final int DEFAULT_BARRIER_PARTICLE_INTERVAL = 60;

	// 如履平地：可跨上的最大步高。原版玩家步高为 0.6，1.0 即“像马一样”跨上 1 格。
	public static final String AUTO_STEP_UP_HEIGHT_ONE_BLOCK = "one_block";
	public static final String AUTO_STEP_UP_HEIGHT_ONE_AND_HALF = "one_and_half";
	public static final String AUTO_STEP_UP_HEIGHT_TWO_BLOCKS = "two_blocks";
	public static final List<String> SUPPORTED_AUTO_STEP_UP_HEIGHTS = List.of(
		AUTO_STEP_UP_HEIGHT_ONE_BLOCK, AUTO_STEP_UP_HEIGHT_ONE_AND_HALF,
		AUTO_STEP_UP_HEIGHT_TWO_BLOCKS);

	// 地狱门自定义等待时间（tick）：0 = 使用子开关与原版规则。
	public static final int MIN_NETHER_PORTAL_DELAY_TICKS = 0;
	public static final int MAX_NETHER_PORTAL_DELAY_TICKS = 600;
	public static final int DEFAULT_NETHER_PORTAL_DELAY_TICKS = 0;

	// 防猪灵装备切换的探测半径（格）。
	public static final int MIN_PIGLIN_SWAP_RANGE = 4;
	public static final int MAX_PIGLIN_SWAP_RANGE = 48;
	public static final int DEFAULT_PIGLIN_SWAP_RANGE = 16;

	// 可见交易：最多显示的未来等级数（村民最高 5 级，当前等级之后最多 4 组）。
	public static final int MIN_VISIBLE_TRADER_FUTURE_LEVELS = 1;
	public static final int MAX_VISIBLE_TRADER_FUTURE_LEVELS = 4;
	public static final int DEFAULT_VISIBLE_TRADER_FUTURE_LEVELS = 4;

	// 可见交易：单次最多下发的锁定交易条数（0 = 不限制）。
	public static final int MIN_VISIBLE_TRADER_LOCKED_OFFERS = 0;
	public static final int MAX_VISIBLE_TRADER_LOCKED_OFFERS = 64;
	public static final int DEFAULT_VISIBLE_TRADER_LOCKED_OFFERS = 0;

	// 骨粉催花：单次最多扩散数量与随机取点半径。
	public static final int MIN_BONE_MEAL_FLOWER_COUNT = 1;
	public static final int MAX_BONE_MEAL_FLOWER_COUNT = 9;
	public static final int DEFAULT_BONE_MEAL_FLOWER_COUNT = 5;
	public static final int MIN_BONE_MEAL_FLOWER_RADIUS = 1;
	public static final int MAX_BONE_MEAL_FLOWER_RADIUS = 4;
	public static final int DEFAULT_BONE_MEAL_FLOWER_RADIUS = 3;

	// 枚举代码 → 实际步高（未知取值回退 1.0，与旧版固定值一致）。
	public static double autoStepUpHeight(String code) {
		if (AUTO_STEP_UP_HEIGHT_ONE_AND_HALF.equals(code)) {
			return 1.5;
		}
		if (AUTO_STEP_UP_HEIGHT_TWO_BLOCKS.equals(code)) {
			return 2.0;
		}
		return 1.0;
	}

	// 更好的放置覆盖：可覆盖的植物范围（仅花草 / 含作物 / 全部植物）。
	public static final String BLOCK_PLACEMENT_FLOWERS = "flowers";
	public static final String BLOCK_PLACEMENT_CROPS = "crops";
	public static final String BLOCK_PLACEMENT_ALL = "all";
	public static final List<String> SUPPORTED_BLOCK_PLACEMENT_MODES = List.of(
		BLOCK_PLACEMENT_FLOWERS, BLOCK_PLACEMENT_CROPS, BLOCK_PLACEMENT_ALL);

	// 防猪灵装备切换的替换部位：auto 表示从背包里选一件金制装备换上（有哪个用哪个）。
	public static final String PIGLIN_ARMOR_SWAP_AUTO = "auto";
	public static final String PIGLIN_ARMOR_SWAP_HELMET = "helmet";
	public static final String PIGLIN_ARMOR_SWAP_CHESTPLATE = "chestplate";
	public static final String PIGLIN_ARMOR_SWAP_LEGGINGS = "leggings";
	public static final String PIGLIN_ARMOR_SWAP_BOOTS = "boots";
	public static final List<String> SUPPORTED_PIGLIN_ARMOR_SWAP_MODES = List.of(
		PIGLIN_ARMOR_SWAP_AUTO, PIGLIN_ARMOR_SWAP_HELMET, PIGLIN_ARMOR_SWAP_CHESTPLATE,
		PIGLIN_ARMOR_SWAP_LEGGINGS, PIGLIN_ARMOR_SWAP_BOOTS);

	// 物品分组：排序方式（保持物品栏原顺序 / 按物品 ID 字母序）。
	public static final String ITEM_GROUP_SORT_DEFAULT = "default";
	public static final String ITEM_GROUP_SORT_ALPHABETICAL = "alphabetical";
	public static final List<String> SUPPORTED_ITEM_GROUP_SORTS = List.of(
		ITEM_GROUP_SORT_DEFAULT, ITEM_GROUP_SORT_ALPHABETICAL);

	// 相机运动强度：百分比，100 为参考模组的默认强度。
	public static final int MIN_CAMERA_MOTION_INTENSITY = 0;
	public static final int MAX_CAMERA_MOTION_INTENSITY = 200;
	public static final int DEFAULT_CAMERA_MOTION_INTENSITY = 100;

	// 定位栏头像倍率档位（与参考实现同一张表）：1..9 对应
	// 0.5x / 0.6x / 0.7x / 0.8x / 1.0x / 1.1x / 1.2x / 1.3x / 1.5x，默认 5 = 1.0x。
	public static final int MIN_LOCATOR_HEAD_SIZE = 1;
	public static final int MAX_LOCATOR_HEAD_SIZE = 9;
	public static final int DEFAULT_LOCATOR_HEAD_SIZE = 5;

	// 定位栏头像描边样式：关闭 / 固定颜色 / 跟随队伍颜色。
	public static final String LOCATOR_HEAD_BORDER_OFF = "off";
	public static final String LOCATOR_HEAD_BORDER_STATIC = "static";
	public static final String LOCATOR_HEAD_BORDER_TEAM = "team";
	public static final List<String> SUPPORTED_LOCATOR_HEAD_BORDERS = List.of(
		LOCATOR_HEAD_BORDER_OFF, LOCATOR_HEAD_BORDER_STATIC, LOCATOR_HEAD_BORDER_TEAM);

	// 描边粗细（与参考实现同一张表）：0.3 / 0.5 / 1.0 / 2.0 像素。
	public static final String LOCATOR_HEAD_BORDER_THIN = "thin";
	public static final String LOCATOR_HEAD_BORDER_MEDIUM = "medium";
	public static final String LOCATOR_HEAD_BORDER_NORMAL = "normal";
	public static final String LOCATOR_HEAD_BORDER_THICK = "thick";
	public static final List<String> SUPPORTED_LOCATOR_HEAD_BORDER_THICKNESSES = List.of(
		LOCATOR_HEAD_BORDER_THIN, LOCATOR_HEAD_BORDER_MEDIUM,
		LOCATOR_HEAD_BORDER_NORMAL, LOCATOR_HEAD_BORDER_THICK);
	public static final String DEFAULT_LOCATOR_HEAD_BORDER_THICKNESS = LOCATOR_HEAD_BORDER_NORMAL;
	
	// 描边粗细代码 → 像素值（未知取值回退 1.0）。
	public static double locatorHeadBorderThicknessPixels(String code) {
		if (LOCATOR_HEAD_BORDER_THIN.equals(code)) {
			return 0.3;
		}
		if (LOCATOR_HEAD_BORDER_MEDIUM.equals(code)) {
			return 0.5;
		}
		if (LOCATOR_HEAD_BORDER_THICK.equals(code)) {
			return 2.0;
		}
		return 1.0;
	}

	// 定位栏玩家名字显示时机：不显示 / 总是 / 仅注视时 / 仅玩家列表打开时。
	public static final String LOCATOR_HEAD_NAMES_NEVER = "never";
	public static final String LOCATOR_HEAD_NAMES_ALWAYS = "always";
	public static final String LOCATOR_HEAD_NAMES_LOOKING_AT = "looking_at";
	public static final String LOCATOR_HEAD_NAMES_PLAYER_LIST = "player_list";
	public static final List<String> SUPPORTED_LOCATOR_HEAD_NAMES = List.of(
		LOCATOR_HEAD_NAMES_NEVER, LOCATOR_HEAD_NAMES_ALWAYS,
		LOCATOR_HEAD_NAMES_LOOKING_AT, LOCATOR_HEAD_NAMES_PLAYER_LIST);

	// 定位栏玩家过滤：全部 / 仅包含名单 / 排除名单。
	public static final String LOCATOR_HEAD_FILTER_ALL = "all";
	public static final String LOCATOR_HEAD_FILTER_INCLUDE = "include";
	public static final String LOCATOR_HEAD_FILTER_EXCLUDE = "exclude";
	public static final List<String> SUPPORTED_LOCATOR_HEAD_FILTERS = List.of(
		LOCATOR_HEAD_FILTER_ALL, LOCATOR_HEAD_FILTER_INCLUDE, LOCATOR_HEAD_FILTER_EXCLUDE);

	// 定位栏描边/指南针共用的颜色调色板（ARGB 在客户端枚举里映射）。
	public static final String LOCATOR_COLOR_WHITE = "white";
	public static final String LOCATOR_COLOR_BLACK = "black";
	public static final String LOCATOR_COLOR_GRAY = "gray";
	public static final String LOCATOR_COLOR_RED = "red";
	public static final String LOCATOR_COLOR_ORANGE = "orange";
	public static final String LOCATOR_COLOR_YELLOW = "yellow";
	public static final String LOCATOR_COLOR_GREEN = "green";
	public static final String LOCATOR_COLOR_BLUE = "blue";
	public static final String LOCATOR_COLOR_AQUA = "aqua";
	public static final List<String> SUPPORTED_LOCATOR_COLORS = List.of(
		LOCATOR_COLOR_WHITE, LOCATOR_COLOR_BLACK, LOCATOR_COLOR_GRAY, LOCATOR_COLOR_RED,
		LOCATOR_COLOR_ORANGE, LOCATOR_COLOR_YELLOW, LOCATOR_COLOR_GREEN, LOCATOR_COLOR_BLUE,
		LOCATOR_COLOR_AQUA);

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
	// 等待烟花加速结束时的最长锁定时间（tick）；0 = 不额外限制。
	public static final int MIN_ELYTRA_FIREWORK_MAX_TICKS = 0;
	public static final int MAX_ELYTRA_FIREWORK_MAX_TICKS = 400;
	public static final int DEFAULT_ELYTRA_FIREWORK_MAX_TICKS = 0;

	// 便捷焚烧默认可用的点燃物（物品 ID）；列表项由 ItemIdConfigStringList 校验与规范化。
	public static final List<String> DEFAULT_QUICK_BURN_IGNITERS = List.of(
		"minecraft:lava_bucket", "minecraft:flint_and_steel");

	// 日志清理：保留的日志归档数量范围与默认值。latest.log 永远不参与清理。
	public static final int MIN_LOG_CLEANUP_KEEP_COUNT = 0;
	public static final int MAX_LOG_CLEANUP_KEEP_COUNT = 100;
	public static final int DEFAULT_LOG_CLEANUP_KEEP_COUNT = 10;

	// 丢弃确认：两次按丢弃键之间的最大确认间隔（毫秒）。
	public static final long DROP_CONFIRM_WINDOW_MILLIS = 3000L;

	// 聊天屏蔽名单异步拉取的超时时间（秒）。
	public static final int MIN_CHAT_LAG_TIMEOUT_SECONDS = 1;
	public static final int MAX_CHAT_LAG_TIMEOUT_SECONDS = 30;
	public static final int DEFAULT_CHAT_LAG_TIMEOUT_SECONDS = 10;

	// 一次挖掘成功后额外锁定的时间（tick）。
	public static final int MIN_MINING_COOLDOWN_TICKS = 0;
	public static final int MAX_MINING_COOLDOWN_TICKS = 40;
	public static final int DEFAULT_MINING_COOLDOWN_TICKS = 0;

	// 聊天屏蔽名单异步缓存的刷新间隔（秒，0 = 永不过期）。
	public static final int MIN_CHAT_LAG_CACHE_SECONDS = 0;
	public static final int MAX_CHAT_LAG_CACHE_SECONDS = 3600;
	public static final int DEFAULT_CHAT_LAG_CACHE_SECONDS = 0;

	// 经验条高等级修复的生效等级下限。
	public static final int MIN_XP_HIGH_LEVEL_FIX_LEVEL = 0;
	public static final int MAX_XP_HIGH_LEVEL_FIX_LEVEL = 1000;
	public static final int DEFAULT_XP_HIGH_LEVEL_FIX_LEVEL = 0;

	// 冷却指示器背景的最小进度（像素，0 = 与原行为一致）。
	public static final int MIN_COOLDOWN_INDICATOR_PROGRESS = 0;
	public static final int MAX_COOLDOWN_INDICATOR_PROGRESS = 16;
	public static final int DEFAULT_COOLDOWN_INDICATOR_PROGRESS = 0;

	// 第三人称鱼线修复的下移量（格）。
	public static final double MIN_FISHING_LINE_FIX_OFFSET = 0.0;
	public static final double MAX_FISHING_LINE_FIX_OFFSET = 0.5;
	public static final double DEFAULT_FISHING_LINE_FIX_OFFSET = 0.1;

	// 超平坦世界禁用史莱姆生成的 Y 上限（-64 ~ 320）。
	public static final int MIN_FLAT_SLIME_Y = -64;
	public static final int MAX_FLAT_SLIME_Y = 320;
	public static final int DEFAULT_FLAT_SLIME_Y = 320;

	// 存档游玩时长的小数位数（0 ~ 2）。
	public static final int MIN_SAVE_PLAY_TIME_DECIMALS = 0;
	public static final int MAX_SAVE_PLAY_TIME_DECIMALS = 2;
	public static final int DEFAULT_SAVE_PLAY_TIME_DECIMALS = 1;

	// 药水时间显示：低时长过滤阈值（秒，0 = 不过滤）与文字配色模式。
	public static final int MIN_STATUS_DURATION_THRESHOLD = 0;
	public static final int MAX_STATUS_DURATION_THRESHOLD = 600;
	public static final int DEFAULT_STATUS_DURATION_THRESHOLD = 0;
	public static final String STATUS_COLOR_AUTO = "auto";
	public static final String STATUS_COLOR_WHITE = "white";
	public static final String STATUS_COLOR_YELLOW = "yellow";
	public static final String STATUS_COLOR_RED = "red";
	public static final List<String> SUPPORTED_STATUS_COLOR_MODES = List.of(
		STATUS_COLOR_AUTO, STATUS_COLOR_WHITE, STATUS_COLOR_YELLOW, STATUS_COLOR_RED);

	// 药水效果显示整体缩放百分比（50 ~ 200，100 = 原版大小）。
	public static final int MIN_STATUS_EFFECT_SCALE = 50;
	public static final int MAX_STATUS_EFFECT_SCALE = 200;
	public static final int DEFAULT_STATUS_EFFECT_SCALE = 100;

	// 聊天框选背景透明度（百分比）与透明通道重写：保留调色板的 RGB，只替换 alpha。
	public static final int MIN_CHAT_SELECTION_OPACITY = 10;
	public static final int MAX_CHAT_SELECTION_OPACITY = 100;
	public static final int DEFAULT_CHAT_SELECTION_OPACITY = 33;

	// 聊天输入记忆的最大保留长度（字符）；原版输入框上限为 256。
	public static final int MIN_CHAT_INPUT_MEMORY_LENGTH = 1;
	public static final int MAX_CHAT_INPUT_MEMORY_LENGTH = 256;
	public static final int DEFAULT_CHAT_INPUT_MEMORY_LENGTH = 256;

	// 配置界面搜索高亮颜色。
	public static final String SEARCH_HIGHLIGHT_GOLD = "gold";
	public static final String SEARCH_HIGHLIGHT_YELLOW = "yellow";
	public static final String SEARCH_HIGHLIGHT_GREEN = "green";
	public static final String SEARCH_HIGHLIGHT_AQUA = "aqua";
	public static final String SEARCH_HIGHLIGHT_RED = "red";
	public static final String SEARCH_HIGHLIGHT_WHITE = "white";
	public static final List<String> SUPPORTED_SEARCH_HIGHLIGHT_COLORS = List.of(
		SEARCH_HIGHLIGHT_GOLD, SEARCH_HIGHLIGHT_YELLOW, SEARCH_HIGHLIGHT_GREEN,
		SEARCH_HIGHLIGHT_AQUA, SEARCH_HIGHLIGHT_RED, SEARCH_HIGHLIGHT_WHITE);
	public static final String DEFAULT_SEARCH_HIGHLIGHT_COLOR = SEARCH_HIGHLIGHT_GOLD;

	public static int withOpacity(int argb, int percent) {
		int clamped = Math.max(MIN_CHAT_SELECTION_OPACITY,
			Math.min(MAX_CHAT_SELECTION_OPACITY, percent));
		int alpha = Math.round(clamped * 255.0F / 100.0F);
		return (alpha << 24) | (argb & 0xFFFFFF);
	}

	// 刷怪蛋投掷：速度百分比（100 = 原版鸡蛋的 1.5 格/刻）与基础速度。
	public static final int MIN_SPAWN_EGG_VELOCITY = 50;
	public static final int MAX_SPAWN_EGG_VELOCITY = 200;
	public static final int DEFAULT_SPAWN_EGG_VELOCITY = 100;
	public static final float SPAWN_EGG_BASE_VELOCITY = 1.5F;

	// 百分比 → 投掷速度：先夹紧再换算，手工改坏配置也不会掷出离谱速度。
	public static float spawnEggThrowVelocity(int percent) {
		int clamped = Math.max(MIN_SPAWN_EGG_VELOCITY, Math.min(MAX_SPAWN_EGG_VELOCITY, percent));
		return SPAWN_EGG_BASE_VELOCITY * clamped / 100.0F;
	}

	// 中键获得液体桶的范围：水与岩浆 / 仅水 / 仅岩浆。
	public static final String FLUID_PICK_WATER_AND_LAVA = "water_and_lava";
	public static final String FLUID_PICK_WATER_ONLY = "water_only";
	public static final String FLUID_PICK_LAVA_ONLY = "lava_only";
	public static final List<String> SUPPORTED_FLUID_PICK_SCOPES = List.of(
		FLUID_PICK_WATER_AND_LAVA, FLUID_PICK_WATER_ONLY, FLUID_PICK_LAVA_ONLY);

	// 实体高亮：描边样式（常亮 / 闪烁）、闪烁周期（tick）与描边颜色。
	public static final String ENTITY_HIGHLIGHT_STYLE_OUTLINE = "outline";
	public static final String ENTITY_HIGHLIGHT_STYLE_BLINK = "blink";
	public static final List<String> SUPPORTED_ENTITY_HIGHLIGHT_STYLES = List.of(
		ENTITY_HIGHLIGHT_STYLE_OUTLINE, ENTITY_HIGHLIGHT_STYLE_BLINK);
	public static final int MIN_ENTITY_HIGHLIGHT_BLINK_PERIOD = 10;
	public static final int MAX_ENTITY_HIGHLIGHT_BLINK_PERIOD = 200;
	public static final int DEFAULT_ENTITY_HIGHLIGHT_BLINK_PERIOD = 60;
	// 颜色为 vanilla 时保留原版队伍色（无队伍即白色），其余使用固定调色板。
	public static final String ENTITY_HIGHLIGHT_COLOR_VANILLA = "vanilla";
	public static final String ENTITY_HIGHLIGHT_COLOR_WHITE = "white";
	public static final String ENTITY_HIGHLIGHT_COLOR_RED = "red";
	public static final String ENTITY_HIGHLIGHT_COLOR_GREEN = "green";
	public static final String ENTITY_HIGHLIGHT_COLOR_BLUE = "blue";
	public static final String ENTITY_HIGHLIGHT_COLOR_YELLOW = "yellow";
	public static final String ENTITY_HIGHLIGHT_COLOR_CYAN = "cyan";
	public static final String ENTITY_HIGHLIGHT_COLOR_MAGENTA = "magenta";
	public static final List<String> SUPPORTED_ENTITY_HIGHLIGHT_COLORS = List.of(
		ENTITY_HIGHLIGHT_COLOR_VANILLA, ENTITY_HIGHLIGHT_COLOR_WHITE, ENTITY_HIGHLIGHT_COLOR_RED,
		ENTITY_HIGHLIGHT_COLOR_GREEN, ENTITY_HIGHLIGHT_COLOR_BLUE, ENTITY_HIGHLIGHT_COLOR_YELLOW,
		ENTITY_HIGHLIGHT_COLOR_CYAN, ENTITY_HIGHLIGHT_COLOR_MAGENTA);

	// 禁用疾跑 FOV 时保留的原版强度百分比（0 = 完全禁用，100 = 等同原版）。
	public static final int MIN_SPRINT_FOV_INTENSITY = 0;
	public static final int MAX_SPRINT_FOV_INTENSITY = 100;
	public static final int DEFAULT_SPRINT_FOV_INTENSITY = 0;

	// 不使用最后一根火把：剩余数量阈值（≤ 该值时阻止放置）。
	public static final int MIN_KEEP_LAST_TORCH_COUNT = 1;
	public static final int MAX_KEEP_LAST_TORCH_COUNT = 8;
	public static final int DEFAULT_KEEP_LAST_TORCH_COUNT = 1;

	// 切换物品保留挖掘进度的最大距离（格，0 = 不限制）。
	public static final int MIN_KEEP_MINING_PROGRESS_DISTANCE = 0;
	public static final int MAX_KEEP_MINING_PROGRESS_DISTANCE = 16;
	public static final int DEFAULT_KEEP_MINING_PROGRESS_DISTANCE = 0;

	// 平滑滚动时长（毫秒）：越大越柔和，越小越跟手。
	public static final int MIN_SMOOTH_SCROLL_DURATION = 30;
	public static final int MAX_SMOOTH_SCROLL_DURATION = 400;
	public static final int DEFAULT_SMOOTH_SCROLL_DURATION = 60;

	// 铁砧“过于昂贵”的费用上限（经验等级，0 = 不限制）。
	public static final int MIN_TOO_EXPENSIVE_MAX_LEVEL_COST = 0;
	public static final int MAX_TOO_EXPENSIVE_MAX_LEVEL_COST = 1000;
	public static final int DEFAULT_TOO_EXPENSIVE_MAX_LEVEL_COST = 0;

	// 疣块分解产出数量（1 ~ 9）。
	public static final int MIN_WART_DECOMPOSITION_OUTPUT = 1;
	public static final int MAX_WART_DECOMPOSITION_OUTPUT = 9;
	public static final int DEFAULT_WART_DECOMPOSITION_OUTPUT = 9;

	// 挖掘惩罚保留百分比（0 = 完全去掉惩罚，100 = 与原版相同）。
	public static final int MIN_MINING_PENALTY_KEEP_PERCENT = 0;
	public static final int MAX_MINING_PENALTY_KEEP_PERCENT = 100;
	public static final int DEFAULT_MINING_PENALTY_KEEP_PERCENT = 0;

	// 允许入睡的最大距离（格，0 = 不限制）。
	public static final int MIN_BED_TOO_FAR_DISTANCE = 0;
	public static final int MAX_BED_TOO_FAR_DISTANCE = 128;
	public static final int DEFAULT_BED_TOO_FAR_DISTANCE = 0;

	// 诅咒附魔移除范围：全部 / 仅绑定诅咒 / 仅消失诅咒。
	public static final String CURSE_SCOPE_ALL = "all";
	public static final String CURSE_SCOPE_BINDING_ONLY = "binding_only";
	public static final String CURSE_SCOPE_VANISHING_ONLY = "vanishing_only";
	public static final List<String> SUPPORTED_CURSE_SCOPES = List.of(
		CURSE_SCOPE_ALL, CURSE_SCOPE_BINDING_ONLY, CURSE_SCOPE_VANISHING_ONLY);

	// 锻造模板保留概率（0 ~ 100，默认 100 = 必定保留）。
	public static final int MIN_SMITHING_TEMPLATE_KEEP_CHANCE = 0;
	public static final int MAX_SMITHING_TEMPLATE_KEEP_CHANCE = 100;
	public static final int DEFAULT_SMITHING_TEMPLATE_KEEP_CHANCE = 100;

	// 附魔等级阿拉伯数字样式：plain（锋利 3）/ parentheses（锋利 (3)）。
	public static final String ENCHANTMENT_LEVEL_STYLE_PLAIN = "plain";
	public static final String ENCHANTMENT_LEVEL_STYLE_PARENTHESES = "parentheses";
	public static final List<String> SUPPORTED_ENCHANTMENT_LEVEL_STYLES = List.of(
		ENCHANTMENT_LEVEL_STYLE_PLAIN, ENCHANTMENT_LEVEL_STYLE_PARENTHESES);

	// 附魔等级阿拉伯数字的生效等级下限（1 ~ 255）。
	public static final int MIN_ENCHANTMENT_ARABIC_LEVEL = 1;
	public static final int MAX_ENCHANTMENT_ARABIC_LEVEL = 255;
	public static final int DEFAULT_ENCHANTMENT_ARABIC_LEVEL = 1;

	// 更好的粗体偏移百分比（10 ~ 100，默认 50 = 0.5 像素）。
	public static final int MIN_BETTER_BOLD_OFFSET_PERCENT = 10;
	public static final int MAX_BETTER_BOLD_OFFSET_PERCENT = 100;
	public static final int DEFAULT_BETTER_BOLD_OFFSET_PERCENT = 50;

	// MiniHUD HUD 行刷新间隔（毫秒，50 ~ 1000，默认 150）。
	public static final int MIN_MINIHUD_REFRESH_INTERVAL_MS = 50;
	public static final int MAX_MINIHUD_REFRESH_INTERVAL_MS = 1000;
	public static final int DEFAULT_MINIHUD_REFRESH_INTERVAL_MS = 150;

	// 雕刻书架可视化：HUD 面板缩放百分比（50 ~ 200，100 = 原始大小）与检测距离（2 ~ 10 格）。
	public static final int MIN_CHISELED_BOOKSHELF_HUD_SCALE = 50;
	public static final int MAX_CHISELED_BOOKSHELF_HUD_SCALE = 200;
	public static final int DEFAULT_CHISELED_BOOKSHELF_HUD_SCALE = 100;
	public static final int MIN_CHISELED_BOOKSHELF_MAX_DISTANCE = 2;
	public static final int MAX_CHISELED_BOOKSHELF_MAX_DISTANCE = 10;
	public static final int DEFAULT_CHISELED_BOOKSHELF_MAX_DISTANCE = 5;

	// 快速滚动：按住 Shift 时滚轮增量的倍率（2 ~ 10，默认 4）。
	public static final int MIN_FAST_SCROLL_MULTIPLIER = 2;
	public static final int MAX_FAST_SCROLL_MULTIPLIER = 10;
	public static final int DEFAULT_FAST_SCROLL_MULTIPLIER = 4;

	// 盔甲架姿态预设：代码顺序即循环顺序；配置列表按代码过滤后沿用此顺序。
	public static final String ARMOR_STAND_POSE_DEFAULT = "default";
	public static final String ARMOR_STAND_POSE_SITTING = "sitting";
	public static final String ARMOR_STAND_POSE_WAVING = "waving";
	public static final String ARMOR_STAND_POSE_BOWING = "bowing";
	public static final String ARMOR_STAND_POSE_THINKING = "thinking";
	public static final String ARMOR_STAND_POSE_CHEERING = "cheering";
	public static final List<String> SUPPORTED_ARMOR_STAND_POSE_PRESETS = List.of(
		ARMOR_STAND_POSE_DEFAULT, ARMOR_STAND_POSE_SITTING, ARMOR_STAND_POSE_WAVING,
		ARMOR_STAND_POSE_BOWING, ARMOR_STAND_POSE_THINKING, ARMOR_STAND_POSE_CHEERING);

	// 盔甲架姿态预设的默认循环列表（与完整预设表一致）。
	public static List<String> defaultArmorStandPosePresets() {
		return new ArrayList<>(SUPPORTED_ARMOR_STAND_POSE_PRESETS);
	}

	// 使用物品时的移动减速保留百分比（0 ~ 100，默认 0 = 完全取消）。
	public static final int MIN_USE_ITEM_SLOWDOWN_KEEP_PERCENT = 0;
	public static final int MAX_USE_ITEM_SLOWDOWN_KEEP_PERCENT = 100;
	public static final int DEFAULT_USE_ITEM_SLOWDOWN_KEEP_PERCENT = 0;

	// 配置界面默认标签代码（关闭标签记忆时使用）。
	public static final String CONFIG_TAB_ALL = "all";
	public static final String CONFIG_TAB_GENERAL = "general";
	public static final String CONFIG_TAB_FUNCTIONS = "functions";
	// 显示标签位于“功能”右侧：世界内可视化与渲染叠加层。
	public static final String CONFIG_TAB_DISPLAY = "display";
	public static final String CONFIG_TAB_MODIFIED = "modified";
	public static final String CONFIG_TAB_FIX = "fix";
	public static final String CONFIG_TAB_OPTIMIZATION = "optimization";
	public static final String CONFIG_TAB_TESTING = "testing";
	// 收藏标签位于“测试”右侧：只显示已被收藏的配置项。
	public static final String CONFIG_TAB_FAVORITES = "favorites";
	public static final List<String> SUPPORTED_CONFIG_TABS = List.of(
		CONFIG_TAB_ALL, CONFIG_TAB_GENERAL, CONFIG_TAB_FUNCTIONS, CONFIG_TAB_DISPLAY,
		CONFIG_TAB_MODIFIED, CONFIG_TAB_FIX, CONFIG_TAB_OPTIMIZATION, CONFIG_TAB_TESTING,
		CONFIG_TAB_FAVORITES);

	// 收藏星样式：自绘星形 / 原版下界之星。
	public static final String FAVORITE_STAR_STYLE_STAR = "star";
	public static final String FAVORITE_STAR_STYLE_NETHER_STAR = "nether_star";
	public static final String DEFAULT_FAVORITE_STAR_STYLE = FAVORITE_STAR_STYLE_STAR;
	public static final List<String> SUPPORTED_FAVORITE_STAR_STYLES = List.of(
		FAVORITE_STAR_STYLE_STAR, FAVORITE_STAR_STYLE_NETHER_STAR);

	// 铁傀儡自动上车的最大玩家距离（0 ~ 64 格，0 = 不限制）。
	public static final int MIN_IRON_GOLEM_PICKUP_DISTANCE = 0;
	public static final int MAX_IRON_GOLEM_PICKUP_DISTANCE = 64;
	public static final int DEFAULT_IRON_GOLEM_PICKUP_DISTANCE = 0;

	// 范围判定：客户端与服务端注入点共用同一份规则，未知取值按“都允许”处理。
	public static boolean fluidPickAllows(String scope, boolean water, boolean lava) {
		return switch (scope == null ? "" : scope) {
			case FLUID_PICK_WATER_ONLY -> water;
			case FLUID_PICK_LAVA_ONLY -> lava;
			default -> water || lava;
		};
	}

	// 配置文件路径。测试时可通过包私有构造器注入；反序列化（Gson）时不参与。
	private transient Path configPath;

	// /tweakify 命令在服务端线程改值，而 MaLiLib 选项属于客户端线程：
	// 这里只挂一个标记，由客户端 tick 统一做一次全量同步，避免跨线程直接改界面控件。
	private transient final AtomicBoolean optionSyncRequested = new AtomicBoolean();

	public TweakifyModConfig() {
		this.configPath = null;
	}

	// 指定路径的构造器：测试与需要显式路径的调用方使用；常规启动走 load()。
	public TweakifyModConfig(Path configPath) {
		this.configPath = configPath;
	}

	private Path configPathOrDefault() {
		return configPath != null ? configPath : ConfigStorage.defaultPath();
	}

	// 配置文件版本号：读取旧文件（缺少该字段时为 0）后自动升级到当前版本。
	public volatile int configVersion = CURRENT_CONFIG_VERSION;

	// 分区实例：字段按界面标签分组，字段表按它们递归索引，JSON 键仍是扁平字段名。
	public final GeneralConfig general = new GeneralConfig();
	public final FunctionConfig functions = new FunctionConfig();
	public final ModifiedConfig modified = new ModifiedConfig();
	public final FixConfig fix = new FixConfig();
	public final OptimizationConfig optimization = new OptimizationConfig();
	public final TestingConfig testing = new TestingConfig();

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
	private transient volatile Set<String> disabledSpawnEntitiesCache;
	private transient volatile Set<String> peacefulModeMonsterEntitiesCache;
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

	// 默认实例：不读盘，列表与缓存按 load() 的默认分支初始化。
	// 供测试或注册表就绪后的调用方使用；调用方不得修改这个共享实例。
	public static TweakifyModConfig createDefaultInstance() {
		TweakifyModConfig config = new TweakifyModConfig();
		config.applyDefaultItemLists();
		config.rebuildCache();
		return config;
	}

	// 轻量占位实例：只做字段初始化与缓存重建，不填充默认物品列表（不扫描注册表、
	// 不解析物品 ID）。供 TweakifyMod 类初始化阶段兜底：类初始化可能被语言 mixin
	// 在注册表初始化期间回调，重活会与第三方 mixin 形成初始化循环。
	public static TweakifyModConfig createPlaceholderInstance() {
		TweakifyModConfig config = new TweakifyModConfig();
		config.rebuildCache();
		return config;
	}

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
				TweakifyModConfig config = ConfigStorage.fromJsonTree(root);
				if (config == null) {
					throw new JsonParseException("Config root cannot be null");
				}
				config.configPath = configPath;
				// JSON null 视为缺省：Gson 对原始类型保留字段默认值，不能当成“已是当前版本”。
				JsonElement versionElement = root.get(CONFIG_VERSION_FIELD);
				int loadedVersion = versionElement != null && !versionElement.isJsonNull()
					? config.configVersion : 0;
				// 前置迁移：只处理版本号与缺失键，不做字段值校验。
				boolean corrected = ConfigMigration.sanitizeConfigVersion(config, root.has("configVersion"))
					| ConfigMigration.migrateScrollListMemoryDefault(config, loadedVersion)
					| ConfigMigration.migrateLegacyFeatureKeys(config, root)
					| ConfigMigration.migrateLocatorHeadOptions(config, root)
					| ConfigMigration.migrateArmorStandGroup(config, root, loadedVersion)
					| ConfigMigration.applyMissingListDefaults(config, root);
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

	// 写盘时按扁平字段表构建不可变 JSON 树：树在配置锁内生成，pretty printing 与磁盘 IO
	// 都在写盘线程执行（见 ConfigStorage.writeAtomically），等待在配置锁外进行
	//（见 save/applyAndSave），因此慢盘与大字段量格式化都不会在持有配置锁时阻塞客户端 tick。
	// 超时视为失败：尚未开始的任务会被取消；已经开始的任务会延长等待一次。
	// 仍超时则由 applyAndSave 字段级回滚后提交一份兜底快照，保证磁盘最终与内存一致。
	public boolean save() {
		AsyncFileWriter.WriteHandle handle = prepareSaveHandle();
		return Boolean.TRUE.equals(AsyncFileWriter.await(handle, SAVE_TIMEOUT_MILLIS, true));
	}

	// 退出同步版保存：用于测试、诊断与收尾线程超时后的同步兜底。
	// 兜底场景下写盘队列已关闭，任务可能无法入队；调用方随后改用 writeSynchronously()
	// 完成最后一次保存，保证慢磁盘下不会静默丢弃。
	public boolean saveForShutdown() {
		AsyncFileWriter.WriteHandle handle = prepareSaveHandle();
		return Boolean.TRUE.equals(AsyncFileWriter.await(handle, SAVE_TIMEOUT_MILLIS, false));
	}

	// 锁内完成「快照树构建 + 入队」，返回可在锁外等待的句柄。
	// 树是字段值的不可变深拷贝；pretty printing 在写盘线程执行，调用线程不再格式化全部字段。
	// 调用方若需要与其它写盘任务保持提交顺序（Options.save 的检查点），应在外层持锁调用。
	public AsyncFileWriter.WriteHandle prepareSaveHandle() {
		synchronized (this) {
			JsonObject jsonTree = toJsonTreeSnapshot();
			Path path = configPathOrDefault();
			long sequence = ConfigStorage.nextWriteSequence();
			return AsyncFileWriter.submit(() -> ConfigStorage.writeAtomically(jsonTree, path, sequence));
		}
	}

	// 为一次后台写盘分配提交序号；调用方应在配置锁内调用，使提交顺序等于序号顺序。
	// 退出兜底写会分配更晚的序号，因此更早提交的任务即使延迟执行也会被落盘层跳过。
	public long allocateWriteSequence() {
		return ConfigStorage.nextWriteSequence();
	}

	// 退出最后兜底：不经过写盘队列，在当前线程同步写一份完整快照。
	// 序号在锁内分配，晚于所有已提交的写任务；写锁限时等待，避免旧任务卡在
	// 不可中断的 I/O 段时让客户端无限期卡在退出界面。
	// 返回 true=已写入，false=写盘失败，null=写锁超时（无法确认，未写）。
	public Boolean writeSynchronously() {
		String json;
		long sequence;
		synchronized (this) {
			json = ConfigStorage.toJson(this);
			sequence = ConfigStorage.nextWriteSequence();
		}
		return ConfigStorage.tryWriteAtomically(
			json, configPathOrDefault(), sequence, SAVE_TIMEOUT_MILLIS);
	}

	// 异步提交当前内存快照（不等待）：保存失败/超时回滚后用它兜底重写。
	// 返回是否成功入队；执行器已关闭时返回 false，调用方需要走同步兜底或回滚。
	public synchronized boolean submitSaveAsync() {
		AsyncFileWriter.WriteHandle handle = submitSaveAsyncHandle();
		if (handle == null) {
			LOGGER.error("Fallback config write could not be queued; the writer is already shut down");
			return false;
		}
		return true;
	}

	// 提交兜底快照并返回可等待句柄；执行器已关闭时返回 null。
	// 调用方可在后续任务中 await 这个句柄，拿到真实的写盘结果（而不是只看“已入队”）。
	public synchronized AsyncFileWriter.WriteHandle submitSaveAsyncHandle() {
		JsonObject jsonTree = toJsonTreeSnapshot();
		Path path = configPathOrDefault();
		long sequence = ConfigStorage.nextWriteSequence();
		return AsyncFileWriter.submit(() -> {
			boolean written = ConfigStorage.writeAtomically(jsonTree, path, sequence);
			if (!written) {
				LOGGER.error("Fallback config write also failed; the config on disk may be out of sync with memory");
			}
			return written;
		});
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

	// 锁内构建不可变 JSON 树快照：字段值已由 snapshot() 复制（列表 List.copyOf），
	// 树本身交给写盘线程做 pretty printing；后台线程不再访问配置字段。
	public synchronized JsonObject toJsonTreeSnapshot() {
		return ConfigStorage.toJsonTree(snapshot().values);
	}

	// 只落盘给定文本，不读写字段：供诊断或测试直接写已准备的文本。
	// 序号必须在提交时分配（prepareSaveHandle / flushPendingSaveNow），不能用执行期序号。
	public boolean writeJson(String json, long sequence) {
		return ConfigStorage.writeAtomically(json, configPathOrDefault(), sequence);
	}

	// 只格式化并落盘已冻结的 JSON 树，不读写字段：供后台写盘线程调用。
	public boolean writeJsonTree(JsonObject jsonTree, long sequence) {
		return ConfigStorage.writeAtomically(jsonTree, configPathOrDefault(), sequence);
	}

	// 新建配置文件：三个物品列表直接使用默认值（写入时统一走清洗）。
	private void applyDefaultItemLists() {
		setStringList(RESTRICTED_OFFHAND_ITEMS_FIELD, defaultLightSourceItemIds());
		setStringList(MAIN_HAND_USE_COOLDOWN_ITEMS_FIELD, defaultInteractableItemIds());
		setStringList(OFFHAND_USE_COOLDOWN_ITEMS_FIELD, defaultInteractableItemIds());
	}

	// 按表驱动执行所有字段级校验：返回是否修正了值。
	// 校验器已随字段搬到各分区，这里只汇总。
	private boolean sanitizeAllFields() {
		return general.sanitize() | functions.sanitize() | modified.sanitize()
			| fix.sanitize() | optimization.sanitize();
	}

	// 包私有：已登记字段级校验器的字段名，供测试校验覆盖度。
	List<String> sanitizedFieldNames() {
		List<String> names = new ArrayList<>(general.sanitizedFieldNames());
		names.addAll(functions.sanitizedFieldNames());
		names.addAll(modified.sanitizedFieldNames());
		names.addAll(fix.sanitizedFieldNames());
		names.addAll(optimization.sanitizedFieldNames());
		return names;
	}

	// 包私有：按类型规则推出“必须登记校验器”的字段名，供测试校验覆盖度。
	static List<String> fieldsRequiringSanitizer() {
		List<String> names = new ArrayList<>();
		for (Map.Entry<String, ConfigFieldRef> entry : CONFIG_FIELDS.entrySet()) {
			String name = entry.getKey();
			if (!MIGRATION_FIELDS.contains(name) && !isImplicitlySanitized(name, entry.getValue().type())) {
				names.add(name);
			}
		}
		return names;
	}

	// 清洗所有 List<String> 配置字段（物品/实体 ID 列表）：新增列表字段时自动覆盖，不再手工维护。
	private boolean sanitizeAllItems() {
		boolean changed = false;
		for (Map.Entry<String, ConfigFieldRef> entry : CONFIG_FIELDS.entrySet()) {
			if (!List.class.isAssignableFrom(entry.getValue().type())) {
				continue;
			}
			// 配置里写成 null 时 Gson 会直接置空字段：此时“空列表 == 空列表”不会触发回写，
			// 留到 rebuildCache 就是 new HashSet<>(null) 的启动 NPE，这里强制回写一份空列表。
			Object raw = read(entry.getKey());
			List<String> original = asList(raw);
			// 自定义物品组等自由格式列表不做 ID 规范化，只补 null。
			List<String> sanitized = isFreeFormListField(entry.getKey())
				? List.copyOf(original)
				: sanitizeItemList(original);
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
		for (Map.Entry<String, ConfigFieldRef> entry : CONFIG_FIELDS.entrySet()) {
			if (!entry.getKey().startsWith(HOTKEY_FIELD_PREFIX)
				|| entry.getValue().type() != String.class) {
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

	// 配置字符串为 null 时 List.of(...).contains(null) 会抛 NPE，统一用这个入口判断。
	// 分区（如 GeneralConfig）的校验器复用这个入口。
	static boolean isSupportedValue(List<String> supported, String value) {
		return value != null && supported.contains(value);
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

	// 自定义物品组定义等自由格式列表：整行内容不当作 ID 处理。
	public static boolean isFreeFormListField(String name) {
		return CUSTOM_ITEM_GROUPS_FIELD.equals(name)
			|| LOCATOR_HEAD_INCLUDED_PLAYERS_FIELD.equals(name)
			|| LOCATOR_HEAD_EXCLUDED_PLAYERS_FIELD.equals(name)
			|| ARMOR_STAND_POSE_PRESETS_FIELD.equals(name);
	}

	private void rebuildCache() {
		restrictedCache = new HashSet<>(functions.restrictedOffhandItems);
		mainHandItemsCache = new HashSet<>(functions.mainHandUseCooldownItems);
		offhandItemsCache = new HashSet<>(functions.offhandUseCooldownItems);
		preset1EntitiesCache = new HashSet<>(functions.highlightPreset1Entities);
		preset2EntitiesCache = new HashSet<>(functions.highlightPreset2Entities);
		preset3EntitiesCache = new HashSet<>(functions.highlightPreset3Entities);
		netherPortalBlacklistCache = new HashSet<>(functions.netherPortalTeleportBlacklist);
		endPortalBlacklistCache = new HashSet<>(functions.endPortalTeleportBlacklist);
		endGatewayBlacklistCache = new HashSet<>(functions.endGatewayTeleportBlacklist);
		attackRangeEntitiesCache = new HashSet<>(functions.attackRangeEntities);
		disabledSpawnEntitiesCache = new HashSet<>(functions.disabledSpawnEntities);
		peacefulModeMonsterEntitiesCache = new HashSet<>(functions.peacefulModeMonsterEntities);
		useItemSlowdownExceptionCache = new HashSet<>(modified.useItemSlowdownExceptions);
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
		List<String> copy = values == null ? List.of() : values;
		write(name, isFreeFormListField(name) ? List.copyOf(copy) : List.copyOf(sanitizeItemList(copy)));
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

	// 包私有：扁平字段表（字段名 -> 定位信息），供序列化、快照与测试共用。
	static Map<String, ConfigFieldRef> fieldRefs() {
		return CONFIG_FIELDS;
	}

	// 包私有：按字段名写入未经清洗的原始值，模拟 Gson 把 JSON null / 非法内容
	// 直接赋给字段的结果，仅供测试使用。
	void setFieldRaw(String name, Object value) {
		requireField(name).set(this, value);
	}

	// 字段是否为布尔开关：配置目录用它校验“命令项必须是开关”，避免注册出无法切换的命令。
	public static boolean isBooleanField(String name) {
		ConfigFieldRef ref = CONFIG_FIELDS.get(name);
		return ref != null && ref.type() == boolean.class;
	}

	private Object read(String name) {
		return requireField(name).get(this);
	}

	private Object readField(ConfigFieldRef ref) {
		return ref.get(this);
	}

	private void write(String name, Object value) {
		requireField(name).set(this, value);
		// 开关或列表变化都可能推翻高亮判定，按实体类型的缓存直接失效。
		entityHighlightCache = null;
	}

	private static ConfigFieldRef requireField(String name) {
		ConfigFieldRef ref = CONFIG_FIELDS.get(name);
		if (ref == null) {
			throw new IllegalArgumentException("Unknown config field: " + name);
		}
		return ref;
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

	// 关闭指定生物生成：列表为空时不生效。
	public boolean isSpawnDisabled(EntityType<?> type) {
		ensureCache();
		if (disabledSpawnEntitiesCache.isEmpty()) {
			return false;
		}
		Identifier id = entityTypeId(type);
		return id != null && disabledSpawnEntitiesCache.contains(id.toString());
	}

	// 和平模式怪物：列表为空时不生效。
	public boolean isPeacefulModeMonster(EntityType<?> type) {
		ensureCache();
		if (peacefulModeMonsterEntitiesCache.isEmpty()) {
			return false;
		}
		Identifier id = entityTypeId(type);
		return id != null && peacefulModeMonsterEntitiesCache.contains(id.toString());
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
		for (Map.Entry<String, ConfigFieldRef> entry : CONFIG_FIELDS.entrySet()) {
			// 字段表里已经缓存了字段定位信息，直接读可以省掉 read(name) 的一次 Map 查找。
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
				write(name, isFreeFormListField(name)
					? List.copyOf(raw)
					: List.copyOf(sanitizeItemList(raw)));
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

	// 命令专用：mutation 返回是否真的发生改动；改动、快照与提交在锁内完成，等待在锁外。
	// 失败时按 before/after 差异做字段级回滚（只回滚仍等于本次写入值的字段），
	// 不会覆盖等待期间其它线程的并发修改。
	public ApplyResult applyAndSave(Supplier<Boolean> mutation) {
		Snapshot before;
		Snapshot after;
		AsyncFileWriter.WriteHandle handle;
		synchronized (this) {
			before = snapshot();
			if (!mutation.get()) {
				return ApplyResult.UNCHANGED;
			}
			after = snapshot();
			handle = prepareSaveHandle();
		}
		if (Boolean.TRUE.equals(AsyncFileWriter.await(handle, SAVE_TIMEOUT_MILLIS, true))) {
			return ApplyResult.SAVED;
		}
		synchronized (this) {
			rollbackChangedFieldsIfUnchanged(before, after);
			cacheDirty = true;
			// 超时的写任务可能已经开始执行并最终把新值写盘；回滚后再提交一份当前快照，
			// 排在超时任务之后，保证磁盘最终与内存一致。
			submitSaveAsync();
		}
		return ApplyResult.SAVE_FAILED;
	}

	// 只回滚本次确实改动、且当前值仍等于 after 的字段；等待期间被其它线程改过的字段保留。
	// public：异步写盘失败结果处理与单元测试共用同一条回滚路径。
	public synchronized void rollbackChangedFieldsIfUnchanged(Snapshot before, Snapshot after) {
		for (Map.Entry<String, Object> entry : after.values.entrySet()) {
			String name = entry.getKey();
			Object afterValue = entry.getValue();
			Object beforeValue = before.values.get(name);
			if (beforeValue == null || Objects.equals(beforeValue, afterValue)) {
				continue;
			}
			if (!Objects.equals(read(name), afterValue)) {
				// 当前值已不是本次写入的值：并发修改优先，不能回滚覆盖。
				continue;
			}
			write(name, beforeValue instanceof List<?> list ? new ArrayList<>(list) : beforeValue);
		}
		cacheDirty = true;
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
		if (!functions.entityHighlight || type == null) {
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
		if (cachedEntityHighlight == functions.entityHighlight
			&& cachedHighlightPreset1 == functions.highlightPreset1
			&& cachedHighlightPreset2 == functions.highlightPreset2
			&& cachedHighlightPreset3 == functions.highlightPreset3) {
			return;
		}
		cachedEntityHighlight = functions.entityHighlight;
		cachedHighlightPreset1 = functions.highlightPreset1;
		cachedHighlightPreset2 = functions.highlightPreset2;
		cachedHighlightPreset3 = functions.highlightPreset3;
		entityHighlightCache = null;
	}

	// 互斥后最多只有一个预设开启；手工改坏配置时按 1→2→3 的优先级取第一个。
	private Set<String> activeHighlightEntities() {
		ensureCache();
		if (functions.highlightPreset1) {
			return preset1EntitiesCache;
		}
		if (functions.highlightPreset2) {
			return preset2EntitiesCache;
		}
		if (functions.highlightPreset3) {
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

	// 快捷焚烧的点燃物判定：列表项为规范化后的物品 ID。
	public boolean isQuickBurnIgniter(Item item) {
		if (item == null || item == Items.AIR) {
			return false;
		}
		Identifier id = idOf(item);
		return id != null && functions.quickBurnItemIgniters.contains(id.toString());
	}

	// 添加一个物品 ID，返回是否新增了（已存在返回 false）。
	// 写时复制：列表整体替换为新实例，渲染线程不会遍历到被就地修改的旧列表。
	public synchronized boolean addItem(String id) {
		String normalized = normalize(id);
		if (normalized == null || functions.restrictedOffhandItems.contains(normalized)) {
			return false;
		}
		List<String> updated = new ArrayList<>(functions.restrictedOffhandItems);
		updated.add(normalized);
		functions.restrictedOffhandItems = List.copyOf(updated);
		cacheDirty = true;
		return true;
	}

	// 移除一个物品 ID，返回是否移除了。
	public synchronized boolean removeItem(String id) {
		String normalized = normalize(id);
		if (normalized == null || !functions.restrictedOffhandItems.contains(normalized)) {
			return false;
		}
		List<String> updated = new ArrayList<>(functions.restrictedOffhandItems);
		updated.remove(normalized);
		functions.restrictedOffhandItems = List.copyOf(updated);
		cacheDirty = true;
		return true;
	}

	// 重置为默认光源物品列表，返回数量。
	public synchronized int resetToLightSources() {
		functions.restrictedOffhandItems = List.copyOf(defaultLightSourceItemIds());
		cacheDirty = true;
		return functions.restrictedOffhandItems.size();
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

	// 不可变快照：仅用于写盘失败后的回滚与一致性校验，不对外暴露具体字段。
	public static final class Snapshot {
		private final Map<String, Object> values;

		private Snapshot(Map<String, Object> values) {
			this.values = values;
		}

		// 按字段值比较两份快照：用于确认待写快照是否已经反映回滚后的内存状态。
		public boolean hasSameValuesAs(Snapshot other) {
			return other != null && this.values.equals(other.values);
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
