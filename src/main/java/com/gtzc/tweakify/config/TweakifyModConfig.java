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
	public static final int CURRENT_CONFIG_VERSION = 3;

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
				int loadedVersion = root.has("configVersion") ? config.configVersion : 0;
				// 前置迁移：只处理版本号与缺失键，不做字段值校验。
				boolean corrected = config.sanitizeConfigVersion(root.has("configVersion"))
					| config.migrateScrollListMemoryDefault(loadedVersion)
					| config.migrateLegacyFeatureKeys(root)
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

	// 写盘时按扁平字段表序列化全部配置项：序列化在配置锁内完成，得到不可变 JSON 字符串；
	// 磁盘 IO 在写盘线程上通过 ConfigStorage 的写锁串行执行，等待在配置锁外进行
	//（见 save/applyAndSave），因此慢盘不会在持有配置锁时阻塞客户端 tick 或其它线程。
	// 超时视为失败：尚未开始的任务会被取消；已经开始的任务会延长等待一次。
	// 仍超时则由 applyAndSave 字段级回滚后提交一份兜底快照，保证磁盘最终与内存一致。
	public boolean save() {
		AsyncFileWriter.WriteHandle handle = prepareSaveHandle();
		return Boolean.TRUE.equals(AsyncFileWriter.await(handle, SAVE_TIMEOUT_MILLIS, true));
	}

	// 退出路径：超时不取消任务，交给 AsyncFileWriter.shutdown 等待队列排空，
	// 保证慢磁盘下最后一次保存不会因为超时被静默丢弃。
	public boolean saveForShutdown() {
		AsyncFileWriter.WriteHandle handle = prepareSaveHandle();
		return Boolean.TRUE.equals(AsyncFileWriter.await(handle, SAVE_TIMEOUT_MILLIS, false));
	}

	// 锁内完成「序列化 + 入队」，返回可在锁外等待的句柄。
	// 调用方若需要与其它写盘任务保持提交顺序（Options.save 的检查点），应在外层持锁调用。
	public AsyncFileWriter.WriteHandle prepareSaveHandle() {
		synchronized (this) {
			String json = ConfigStorage.toJson(this);
			Path path = configPathOrDefault();
			long sequence = ConfigStorage.nextWriteSequence();
			return AsyncFileWriter.submit(() -> ConfigStorage.writeAtomically(json, path, sequence));
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
		String json = ConfigStorage.toJson(this);
		Path path = configPathOrDefault();
		long sequence = ConfigStorage.nextWriteSequence();
		return AsyncFileWriter.submit(() -> {
			boolean written = ConfigStorage.writeAtomically(json, path, sequence);
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

	// 只落盘，不读写字段：供后台写盘线程调用（不能在这里同步整个对象）。
	// 序号必须在提交时分配（prepareSaveHandle / flushPendingSaveNow），不能用执行期序号。
	public boolean writeJson(String json, long sequence) {
		return ConfigStorage.writeAtomically(json, configPathOrDefault(), sequence);
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
		if (loadedVersion >= 2 || general.scrollListMemory) {
			return false;
		}
		LOGGER.info("Enabling scrollListMemory for configs older than version 2");
		general.scrollListMemory = true;
		return true;
	}

	// 版本 3：相机运动与物品分组取代旧的“运动相机横移倾斜”与“创造物品栏条目合并”。
	// 旧字段已从配置类移除，Gson 会忽略它们，因此需要从原始 JSON 里读出旧值迁移。
	private boolean migrateLegacyFeatureKeys(JsonObject root) {
		boolean changed = false;
		if (!root.has("cameraMotion") && root.has("motionCameraTilt")) {
			functions.cameraMotion = legacyBoolean(root, "motionCameraTilt");
			changed = true;
		}
		if (!root.has("hotkeyCameraMotion") && root.has("hotkeyMotionCameraTilt")) {
			functions.hotkeyCameraMotion = legacyString(root, "hotkeyMotionCameraTilt");
			changed = true;
		}
		if (!root.has("itemGroups") && root.has("mergeCreativeItemEntries")) {
			functions.itemGroups = legacyBoolean(root, "mergeCreativeItemEntries");
			changed = true;
		}
		if (!root.has("hotkeyItemGroups") && root.has("hotkeyMergeCreativeItemEntries")) {
			functions.hotkeyItemGroups = legacyString(root, "hotkeyMergeCreativeItemEntries");
			changed = true;
		}
		// 优化标签已移除快捷键：旧文件里的字段没有对应配置字段，Gson 会忽略；
		// 这里标记一次修正，让下一次写盘把旧键清掉。
		if (root.has("hotkeyMiniHudFpsOptimization")) {
			LOGGER.info("Removing obsolete config hotkey: hotkeyMiniHudFpsOptimization");
			changed = true;
		}
		if (changed) {
			LOGGER.info("Migrating legacy config keys");
		}
		return changed;
	}

	private static boolean legacyBoolean(JsonObject root, String key) {
		JsonElement value = root.get(key);
		return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()
			&& value.getAsBoolean();
	}

	private static String legacyString(JsonObject root, String key) {
		JsonElement value = root.get(key);
		return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
			? value.getAsString() : "";
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
