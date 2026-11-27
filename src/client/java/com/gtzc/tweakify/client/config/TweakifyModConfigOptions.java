package com.gtzc.tweakify.client.config;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.state.ChatInputMemoryState;
import com.gtzc.tweakify.config.ConfigCatalog;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.AsyncFileWriter;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.config.IConfigDouble;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.IConfigInteger;
import fi.dy.masa.malilib.config.IConfigOptionList;
import fi.dy.masa.malilib.config.IConfigString;
import fi.dy.masa.malilib.config.IConfigStringList;
import fi.dy.masa.malilib.config.options.ConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.util.StringUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

// 配置项字段通过 OPTIONS_BY_NAME 反射按名字索引，配置界面、快捷键与命令表都按名字派生；
// IDE 无法追踪这种“按字符串使用”，会把大量公开配置声明误报为未使用。
// 这些字段不能删除（漏一项会让启动期双向覆盖校验直接失败）。
// 注意：类级注解也会抑制方法体内的未使用提示；当前私有成员与局部变量均已核对有调用，
// 后续新增逻辑请自行确认没有引入真正的死代码。
@SuppressWarnings("unused")
public final class TweakifyModConfigOptions implements IConfigHandler {

	// 双向同步进行中时为 true：屏蔽值变更回调，避免递归与重复写盘。
	private static boolean syncing;

	// 三个预设互斥：开启一个时关闭其他两个，被关闭项的回调不再重复保存。
	private static boolean enforcingPresetExclusivity;

	private static final ImmutableList<ConfigBoolean> HIGHLIGHT_PRESETS = ImmutableList.of(
		FunctionConfigOptions.HIGHLIGHT_PRESET_1, FunctionConfigOptions.HIGHLIGHT_PRESET_2,
		FunctionConfigOptions.HIGHLIGHT_PRESET_3);

	private static void onHighlightPresetChanged(ConfigBoolean changed) {
		if (enforcingPresetExclusivity) {
			return;
		}
		if (changed.getBooleanValue()) {
			enforcingPresetExclusivity = true;
			try {
				for (ConfigBoolean preset : HIGHLIGHT_PRESETS) {
					if (preset != changed) {
						preset.setBooleanValue(false);
					}
				}
			} finally {
				enforcingPresetExclusivity = false;
			}
		}
		syncAndSave();
	}

	// 配置项实例按名字索引：反射收集各分片类的静态 IConfigBase 字段。
	// 新增配置项只需在对应标签页文件声明字段 + 在 ConfigCatalog 登记一行，不必再同步维护多份清单。
	// 仅界面使用的操作项不写入配置文件，也不参与目录/字段校验。
	private static final Set<String> UI_ONLY_OPTION_FIELDS = Set.of("PROFILE_EDITOR", "LOG_CLEANUP_NOW");

	private static final Map<String, IConfigBase> OPTIONS_BY_NAME = indexOptions();

	private static Map<String, IConfigBase> indexOptions() {
		Map<String, IConfigBase> options = new LinkedHashMap<>();
		for (Class<?> owner : List.of(
			GeneralConfigOptions.class,
			FunctionConfigOptions.class,
			ModifiedConfigOptions.class,
			FixConfigOptions.class,
			OptimizationConfigOptions.class,
			TestingConfigOptions.class)) {
			collectOptions(owner, options);
		}
		return Collections.unmodifiableMap(options);
	}

	private static void collectOptions(Class<?> owner, Map<String, IConfigBase> options) {
		for (Field field : owner.getDeclaredFields()) {
			if (!Modifier.isStatic(field.getModifiers())
				|| !IConfigBase.class.isAssignableFrom(field.getType())) {
				continue;
			}
			// 仅界面使用的操作项不参与配置字段/目录校验。
			if (UI_ONLY_OPTION_FIELDS.contains(field.getName())) {
				continue;
			}
			try {
				if (field.get(null) instanceof IConfigBase option) {
					options.put(option.getName(), option);
				}
			} catch (IllegalAccessException e) {
				throw new IllegalStateException("Cannot read option field " + field.getName(), e);
			}
		}
	}

	// 全部配置项，顺序与 ConfigCatalog 一致（界面树、快捷键与统计都按这个顺序）。
	public static final ImmutableList<IConfigBase> ALL_OPTIONS = buildAllOptions();

	private static ImmutableList<IConfigBase> buildAllOptions() {
		ImmutableList.Builder<IConfigBase> builder = ImmutableList.builder();
		for (String name : ConfigCatalog.optionNames()) {
			IConfigBase option = OPTIONS_BY_NAME.get(name);
			if (option == null) {
				throw new IllegalStateException("Missing MaLiLib option for config entry: " + name);
			}
			builder.add(option);
		}
		return builder.build();
	}

	// MaLiLib 的正统持久化机制：配置值一旦变化（setModified -> onValueChanged）
	// 就同步到模组配置并延迟写盘，不依赖编辑对话框关闭时的修改判定。
	// 统一按 ALL_OPTIONS 注册，新增配置项（含快捷键项）不必再手工补一行回调。
	static {
		for (IConfigBase option : ALL_OPTIONS) {
			registerValueChangeCallback(option);
		}
		registerPresetCallback(FunctionConfigOptions.HIGHLIGHT_PRESET_1);
		registerPresetCallback(FunctionConfigOptions.HIGHLIGHT_PRESET_2);
		registerPresetCallback(FunctionConfigOptions.HIGHLIGHT_PRESET_3);
	}

	// 按名字取配置项实例：配置树由 ConfigCatalog 的登记顺序构建，不再手写一份结构。
	public static IConfigBase optionOf(String name) {
		return OPTIONS_BY_NAME.get(name);
	}

	// ConfigBase.setValueChangeCallback 的泛型参数是各配置项自身，遍历注册时需要擦除类型。
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void registerValueChangeCallback(IConfigBase option) {
		if (option instanceof ConfigBase base) {
			base.setValueChangeCallback(ignored -> syncAndSave(option));
		}
	}

	// 三个预设互斥：开启一个时关闭其他两个，再统一同步并保存。
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void registerPresetCallback(ConfigBoolean preset) {
		((ConfigBase) preset).setValueChangeCallback(ignored -> onHighlightPresetChanged(preset));
	}

	// 高亮分组的 prettyName 使用 highlight 前缀：按目录中的分组结构判断，不再手写一份名单。
	private static String prettyNamePrefix(String optionName) {
		return ConfigCatalog.isInsideGroup("entityHighlight", optionName)
			? ConfigOptionKeys.HIGHLIGHT_KEY : ConfigOptionKeys.GENERAL_KEY;
	}

	private static void requireField(String fieldName) {
		if (!TweakifyModConfig.hasField(fieldName)) {
			throw new IllegalStateException("Missing TweakifyModConfig field for option: " + fieldName);
		}
	}

	// 带快捷键的配置项，注册到 MaLiLib 快捷键系统（由 ALL_OPTIONS 派生，不再手工枚举）。
	public static final ImmutableList<IHotkey> HOTKEY_OPTIONS = collectHotkeys();

	private static ImmutableList<IHotkey> collectHotkeys() {
		ImmutableList.Builder<IHotkey> builder = ImmutableList.builder();
		for (IConfigBase option : ALL_OPTIONS) {
			if (option instanceof IHotkey hotkey) {
				builder.add(hotkey);
			}
		}
		return builder.build();
	}

	// 布尔开关配置项，供配置界面统计“总数/可用/已修改/已禁用”（由 ALL_OPTIONS 派生）。
	public static final ImmutableList<IConfigBoolean> TOGGLE_OPTIONS = collectToggles();

	private static ImmutableList<IConfigBoolean> collectToggles() {
		ImmutableList.Builder<IConfigBoolean> builder = ImmutableList.builder();
		for (IConfigBase option : ALL_OPTIONS) {
			if (option instanceof IConfigBoolean toggle) {
				builder.add(toggle);
			}
		}
		return builder.build();
	}

	// 版本号只用于迁移，不对外提供界面选项。
	private static final String CONFIG_VERSION_FIELD = "configVersion";

	// 带值的配置项名即 TweakifyModConfig 字段名，启动时校验一次，避免改名后同步静默失效；
	// 纯快捷键项（如 openConfigGui）只校验对应的 hotkey 字段。
	// 同时校验目录与字段双向覆盖：漏登记或漏声明都在启动时直接失败，而不是静默丢失一项。
	// 放在最后：上面的派生列表已全部初始化，静态块执行顺序不会拿到 null。
	static {
		for (IConfigBase option : ALL_OPTIONS) {
			if (option instanceof IConfigBoolean || option instanceof IConfigStringList
				|| option instanceof IConfigString
				|| option instanceof IConfigOptionList || option instanceof IConfigInteger
				|| option instanceof IConfigDouble) {
				requireField(option.getName());
			}
			if (option instanceof IHotkey) {
				requireField(hotkeyFieldName(option.getName()));
			}
		}

		if (OPTIONS_BY_NAME.size() != ALL_OPTIONS.size()) {
			Set<String> orphans = new LinkedHashSet<>(OPTIONS_BY_NAME.keySet());
			ALL_OPTIONS.forEach(option -> orphans.remove(option.getName()));
			throw new IllegalStateException("Options not registered in ConfigCatalog: " + orphans);
		}

		Set<String> coveredFields = new HashSet<>(OPTIONS_BY_NAME.keySet());
		for (IHotkey hotkey : HOTKEY_OPTIONS) {
			coveredFields.add(hotkeyFieldName(hotkey.getName()));
		}
		coveredFields.add(CONFIG_VERSION_FIELD);
		for (String fieldName : TweakifyModConfig.configFieldNames()) {
			if (!coveredFields.contains(fieldName)) {
				throw new IllegalStateException("Config field without option: " + fieldName);
			}
		}
	}

	// 配置项名与 TweakifyModConfig 字段名一一对应，同步时按名字读写；
	// 新增开关只需添加字段与配置项，不必再分别维护两段镜像代码。
	public static void syncFromConfig() {
		if (!TweakifyMod.isConfigLoaded()) {
			return;
		}
		TweakifyModConfig config = TweakifyMod.config();

		// 同步期间屏蔽值变更回调，避免每个赋值都触发一次写盘。
		boolean previousSyncing = syncing;
		syncing = true;
		try {
			for (IConfigBase option : ALL_OPTIONS) {
				applyToOption(config, option);
			}
			refreshPrettyNames();
		} finally {
			syncing = previousSyncing;
		}
		resetChatMemoryIfDisabled();
	}

	// 把配置对象的值写进 MaLiLib 选项（含可选快捷键）。
	private static void applyToOption(TweakifyModConfig config, IConfigBase option) {
		String name = option.getName();
		if (option instanceof IConfigOptionList optionList) {
			optionList.setOptionListValue(
				optionList.getDefaultOptionListValue().fromString(config.getString(name)));
		} else if (option instanceof IConfigStringList list) {
			list.setStrings(config.getStringList(name));
		} else if (option instanceof IConfigString stringOption) {
			String value = config.getString(name);
			stringOption.setStringValue(value == null ? "" : value);
		} else if (option instanceof IConfigInteger integer) {
			integer.setIntegerValue(config.getInteger(name));
		} else if (option instanceof IConfigDouble doubleOption) {
			doubleOption.setDoubleValue(config.getDouble(name));
		} else if (option instanceof IConfigBoolean toggle) {
			toggle.setBooleanValue(config.getBoolean(name));
		}
		if (option instanceof IHotkey hotkey) {
			String bound = config.getString(hotkeyFieldName(name));
			// 配置写成 null 时（手工编辑、迁移工具）MaLiLib 会在 split 处抛 NPE；未绑定用空字符串表示。
			hotkey.getKeybind().setValueFromString(bound == null ? "" : bound);
		}
	}

	// 把 MaLiLib 选项的值写回配置对象（赋值触发的回调由 syncing 拦截）。
	private static void applyToConfig(TweakifyModConfig config, IConfigBase option) {
		String name = option.getName();
		if (option instanceof IConfigOptionList optionList) {
			config.setString(name, optionList.getOptionListValue().getStringValue());
		} else if (option instanceof IConfigStringList list) {
			config.setStringList(name, list.getStrings());
		} else if (option instanceof IConfigString stringOption) {
			config.setString(name, stringOption.getStringValue());
		} else if (option instanceof IConfigInteger integer) {
			config.setInteger(name, integer.getIntegerValue());
		} else if (option instanceof IConfigDouble doubleOption) {
			config.setDouble(name, doubleOption.getDoubleValue());
		} else if (option instanceof IConfigBoolean toggle) {
			config.setBoolean(name, toggle.getBooleanValue());
		}
		if (option instanceof IHotkey hotkey) {
			config.setString(hotkeyFieldName(name), hotkey.getKeybind().getStringValue());
		}
	}

	// 快捷键字段统一命名为 hotkey + 配置项名首字母大写。
	private static String hotkeyFieldName(String optionName) {
		return TweakifyModConfig.HOTKEY_FIELD_PREFIX
			+ Character.toUpperCase(optionName.charAt(0)) + optionName.substring(1);
	}

	// 快捷键切换提示使用 prettyName，按当前语言重新翻译一次。
	private static void refreshPrettyNames() {
		for (IHotkey hotkey : HOTKEY_OPTIONS) {
			hotkey.setPrettyName(StringUtils.translate(
				prettyNamePrefix(hotkey.getName()) + ".prettyName." + hotkey.getName()));
		}
	}

	@Override
	public void load() {
		syncFromConfig();
	}

	@Override
	public void save() {
		// 有尚未写盘的修改时先提交；随后把完整快照排到同一串行队列。
		if (savePending) {
			flushPendingSaveNow();
		}
		if (!TweakifyMod.isConfigLoaded()) {
			return;
		}
		TweakifyModConfig config = TweakifyMod.config();
		long covered;
		AsyncFileWriter.WriteHandle handle;
		synchronized (config) {
			covered = WRITE_SEQUENCE.get();
			handle = config.prepareSaveHandle();
			if (handle != null) {
				// 完整快照已包含当前节流修改，不用再单独写一遍。
				savePending = false;
				pendingSnapshot = null;
			}
		}
		if (handle == null) {
			TweakifyMod.LOGGER.error("Failed to submit Tweakify Mod configuration save");
			// 写盘队列已关闭：直接同步写当前内存，避免修改只留在内存里。
			Boolean sync = config.writeSynchronously();
			if (!Boolean.TRUE.equals(sync)) {
				TweakifyMod.LOGGER.error("Failed to save Tweakify Mod configuration synchronously");
			}
			return;
		}
		long previousSynced = lastSyncedSequence.get();
		long advancedSynced = lastSyncedSequence.accumulateAndGet(covered, Math::max);
		// 完整快照已提交：先乐观标记覆盖本次提交前的所有异步写。
		boolean callbackQueued = AsyncFileWriter.execute(() -> {
			Boolean written = AsyncFileWriter.await(handle, SAVE_CALLBACK_WAIT_MILLIS, true);
			if (Boolean.TRUE.equals(written)) {
				discardDeferredUpTo(advancedSynced);
				return;
			}
			TweakifyMod.LOGGER.error("Failed to save Tweakify Mod configuration");
			handleSaveFailure(config, advancedSynced, previousSynced);
		});
		if (!callbackQueued) {
			// 执行器已关闭，失败回调不会执行：先确认整份写是否已经成功，
			// 避免把可能已落盘的新值在内存中回滚（内存旧值/磁盘新值的反向不一致）。
			Boolean written = AsyncFileWriter.await(handle, SAVE_CALLBACK_WAIT_MILLIS, false);
			if (Boolean.TRUE.equals(written)) {
				discardDeferredUpTo(advancedSynced);
				return;
			}
			if (written == null) {
				TweakifyMod.LOGGER.warn("Unable to confirm the final config write before shutdown; keeping the in-memory state");
				return;
			}
			// 明确失败：同步兜底；成功则丢弃被覆盖的延迟结果，仍失败才回退并重放。
			// 锁超时（null）不触发回退：竞争者可能正在写当前内存快照，回退会造成反向漂移。
			Boolean sync = config.writeSynchronously();
			handleSaveFailure(sync, advancedSynced, previousSynced);
		}
	}

	// 整份保存失败后的收敛：提交兜底写并等它的真实结果（不是“已入队”）；
	// 执行器已关闭时同步写；两者都失败才回退乐观推进的序号。
	static void handleSaveFailure(TweakifyModConfig config, long advancedSequence, long previousSequence) {
		AsyncFileWriter.WriteHandle fallback = config.submitSaveAsyncHandle();
		if (fallback == null) {
			Boolean sync = config.writeSynchronously();
			handleSaveFailure(sync, advancedSequence, previousSequence);
			return;
		}
		// 回调排在兜底写之后，await 立即返回，不会自等待。
		boolean callbackQueued = AsyncFileWriter.execute(() -> {
			Boolean written = AsyncFileWriter.await(fallback, SAVE_CALLBACK_WAIT_MILLIS, false);
			handleSaveFailure(Boolean.TRUE.equals(written), advancedSequence, previousSequence);
		});
		if (!callbackQueued) {
			Boolean written = AsyncFileWriter.await(fallback, SAVE_CALLBACK_WAIT_MILLIS, false);
			handleSaveFailure(Boolean.TRUE.equals(written), advancedSequence, previousSequence);
		}
	}

	// 包私有：decision + CAS 回退 + 重放被跳过的失败，测试可直接驱动。
	static void handleSaveFailure(boolean fallbackWritten, long advancedSequence, long previousSequence) {
		handleSaveFailure(Boolean.valueOf(fallbackWritten), advancedSequence, previousSequence);
	}

	// Boolean：true=写成功，false=写失败，null=锁超时/无法确认（不回退，避免反向漂移）。
	static void handleSaveFailure(Boolean fallbackWritten, long advancedSequence, long previousSequence) {
		if (Boolean.TRUE.equals(fallbackWritten)) {
			discardDeferredUpTo(advancedSequence);
			return;
		}
		if (fallbackWritten == null) {
			TweakifyMod.LOGGER.warn("Timed out waiting for the config write lock; keeping the optimistic sequence");
			return;
		}
		if (rollbackSyncedSequence(advancedSequence, previousSequence)) {
			replayDeferredFailures(previousSequence, advancedSequence);
		}
	}

	// 只回退本次乐观推进；若已有更新的成功推进，CAS 失败即保留新值。
	static boolean rollbackSyncedSequence(long advancedSequence, long previousSequence) {
		return lastSyncedSequence.compareAndSet(advancedSequence, previousSequence);
	}

	// 包私有：测试直接检查序号推进/回退。
	static void setSyncedSequenceForTest(long value) {
		lastSyncedSequence.set(value);
	}

	static long syncedSequenceForTest() {
		return lastSyncedSequence.get();
	}

	@Override
	public void onConfigsChanged() {
		syncAndSave();
	}

	// 写盘节流：滑块拖动等高频修改会连续触发几十次回调，
	// 这里合并成最多每 MIN_SAVE_INTERVAL_MS 写一次；客户端 tick 与配置保存入口负责兜底刷出。
	private static final long MIN_SAVE_INTERVAL_MS = 250L;
	// 异步保存回调等待上限：回调排在本份写盘之后，正常应瞬时返回。
	private static final long SAVE_CALLBACK_WAIT_MILLIS = 1000L;
	private static boolean savePending;
	private static long lastSaveAtMillis;
	private static TweakifyModConfig.Snapshot pendingSnapshot;

	// 异步写盘：客户端线程只做「快照 -> JSON 树」（瞬时可忽略），pretty printing 与磁盘 IO
	// 都交给后台线程。每次 flush 都在配置锁内完成「建树 + 提交到全局串行队列」，因此队列顺序
	// 等于快照新旧顺序；即使磁盘慢、前面还有旧写未完成，也不会出现旧快照晚于新快照落盘。
	private static final Queue<SaveResult> SAVE_RESULTS = new ConcurrentLinkedQueue<>();
	// 被乐观推进跳过的失败结果：整份保存失败并回退序号后，按序号重放，重新参与回滚判定。
	private static final Queue<SaveResult> DEFERRED_RESULTS = new ConcurrentLinkedQueue<>();
	private static final int MAX_DEFERRED_RESULTS = 8;

	// 写盘序号：每次 flush 提交 +1。Options.save() 提交完整快照时会乐观推进 lastSyncedSequence
	//（覆盖本次提交前已入队的异步写），用于识别“结果入队时已被更晚的整份保存覆盖”的过期失败，
	// 避免旧失败结果反向回滚新值。整份保存失败且无任何兜底写时，会用 CAS 回退这次推进。
	private static final AtomicLong WRITE_SEQUENCE = new AtomicLong();
	private static final AtomicLong lastSyncedSequence = new AtomicLong();

	// 节流 flush 的在途上限：磁盘慢（网盘/杀软/机械盘）时，生产速度可能高于消费速度，
	// 超过上限就保留待写标记，等下一个节流窗口再提交，避免每 250ms 一份全量 JSON 无上限堆积。
	// 注意：显式保存（save/applyAndSave/退出兜底）不受此限制，必须入队或同步写。
	private static final int MAX_IN_FLIGHT_FLUSHES = 4;
	private static final AtomicInteger IN_FLIGHT_FLUSHES = new AtomicInteger();

	private record SaveResult(boolean success, TweakifyModConfig.Snapshot snapshot,
						 TweakifyModConfig.Snapshot afterSnapshot, long sequence) {
	}

	// 单配置项变化：只同步这一项，避免每次切换开关都清洗 8 个列表并重建缓存。
	static void syncAndSave(IConfigBase changed) {
		if (syncing || !TweakifyMod.isConfigLoaded() || changed == null) {
			return;
		}
		TweakifyModConfig config = TweakifyMod.config();
		syncing = true;
		try {
			markPending(config);
			applyToConfig(config, changed);
			resetChatMemoryIfDisabled();
			savePending = true;
		} finally {
			syncing = false;
		}
		flushPendingSave();
	}

	// 全量同步：快捷键编辑结束、配置界面重建等没有明确变化项时使用（低频）。
	public static void syncAndSave() {
		if (syncing || !TweakifyMod.isConfigLoaded()) {
			return;
		}
		TweakifyModConfig config = TweakifyMod.config();
		syncing = true;
		try {
			markPending(config);
			for (IConfigBase option : ALL_OPTIONS) {
				applyToConfig(config, option);
			}
			resetChatMemoryIfDisabled();
			savePending = true;
		} finally {
			syncing = false;
		}
		flushPendingSave();
	}

	// 关闭聊天输入记忆时立即清空草稿，再次开启后从空开始。
	private static void resetChatMemoryIfDisabled() {
		if (!FunctionConfigOptions.CHAT_INPUT_MEMORY.getBooleanValue()) {
			ChatInputMemoryState.reset();
		}
	}

	// 本批修改开始前才拍快照：同一批高频变化（如拖动滑块）共用一份回滚快照。
	private static void markPending(TweakifyModConfig config) {
		if (!savePending) {
			pendingSnapshot = config.snapshot();
		}
	}

	// 由客户端 tick 调用：超过节流间隔才真正写盘，避免高频 IO。
	public static void flushPendingSave() {
		if (savePending && System.currentTimeMillis() - lastSaveAtMillis >= MIN_SAVE_INTERVAL_MS) {
			flushPendingSaveNow();
		}
	}

	// 立即写盘：客户端线程只构建不可变 JSON 树，格式化与磁盘 IO 都交给后台线程，
	// 避免拖动滑块时掉帧。
	// 「建树 + 提交」必须在同一把配置锁内完成，保证提交顺序就是快照新旧顺序：
	// 否则另一个线程的同步保存可能先提交，而这里的旧快照稍后才入队，把新内容覆盖掉。
	public static void flushPendingSaveNow() {
		if (!savePending) {
			return;
		}
		if (!TweakifyMod.isConfigLoaded()) {
			savePending = false;
			pendingSnapshot = null;
			return;
		}
		TweakifyModConfig config = TweakifyMod.config();
		TweakifyModConfig.Snapshot snapshot = pendingSnapshot;
		boolean snapshotFailed = false;
		synchronized (config) {
			if (IN_FLIGHT_FLUSHES.get() >= MAX_IN_FLIGHT_FLUSHES) {
				// 队列积压：保留本批修改与回滚快照，等下一个节流窗口再提交。
				lastSaveAtMillis = System.currentTimeMillis();
				return;
			}
			JsonObject jsonTree = null;
			TweakifyModConfig.Snapshot after = null;
			try {
				jsonTree = config.toJsonTreeSnapshot();
				after = config.snapshot();
			} catch (RuntimeException e) {
				TweakifyMod.LOGGER.error("Failed to snapshot Tweakify Mod configuration for serialization", e);
				snapshotFailed = true;
			}
			savePending = false;
			pendingSnapshot = null;
			lastSaveAtMillis = System.currentTimeMillis();
			if (!snapshotFailed) {
				JsonObject tree = jsonTree;
				TweakifyModConfig.Snapshot afterSnapshot = after;
				long sequence = WRITE_SEQUENCE.incrementAndGet();
				// 落盘序号在提交时分配：任务延迟执行时不能覆盖更晚提交的写。
				long writeSequence = config.allocateWriteSequence();
				IN_FLIGHT_FLUSHES.incrementAndGet();
				// 只入队、不等待：队列由后台线程按提交顺序串行执行。
				// 入队失败（执行器已关闭）时立即退回计数，避免永久占掉一个名额。
				if (!AsyncFileWriter.execute(() -> {
					try {
						writeConfig(tree, snapshot, afterSnapshot, sequence, writeSequence);
					} finally {
						IN_FLIGHT_FLUSHES.decrementAndGet();
					}
				})) {
					IN_FLIGHT_FLUSHES.decrementAndGet();
					// 队列已关闭（退出收尾已开始）：直接同步写这份已冻结的快照，
					// 避免退出时停留在配置界面、最后一批修改静默丢失。
					try {
						if (!config.writeJsonTree(tree, writeSequence)) {
							TweakifyMod.LOGGER.error(
								"Failed to save the final pending config changes synchronously");
						}
					} catch (RuntimeException e) {
						TweakifyMod.LOGGER.error(
							"Unexpected error while saving the final pending config changes", e);
					}
				}
			}
		}
		if (snapshotFailed) {
			// 建树失败等同于序列化/写盘失败：在客户端线程回滚，不进入后台队列。
			config.restoreSnapshot(snapshot);
			syncFromConfig();
		}
	}

	// 后台写盘任务：只对提交时冻结的 JSON 树做格式化与落盘，不访问配置字段，
	// 因此不会与配置锁互相阻塞，也不会把任务执行期间的新修改写进这份快照。
	private static void writeConfig(JsonObject jsonTree, TweakifyModConfig.Snapshot snapshot,
								 TweakifyModConfig.Snapshot afterSnapshot, long sequence, long writeSequence) {
		boolean success;
		try {
			TweakifyModConfig config = TweakifyMod.config();
			success = TweakifyMod.isConfigLoaded() && config.writeJsonTree(jsonTree, writeSequence);
		} catch (RuntimeException e) {
			// 非 IOException 的写盘异常（路径非法、安全策略等）也必须产出失败结果：
			// 否则 pollSaveResults 永远收不到这次结果，回滚与日志会一起静默丢失。
			TweakifyMod.LOGGER.error("Unexpected error while writing Tweakify Mod configuration", e);
			success = false;
		}
		SAVE_RESULTS.add(new SaveResult(success, snapshot, afterSnapshot, sequence));
	}

	// 客户端 tick 调用：把后台写盘结果取回客户端线程处理。
	public static void pollSaveResults() {
		SaveResult result;
		while ((result = SAVE_RESULTS.poll()) != null) {
			if (result.success()) {
				continue;
			}
			TweakifyMod.LOGGER.error("Failed to save Tweakify Mod configuration");
			// 这份失败写已被更晚成功的整份同步写覆盖：转存到延迟队列，
			// 若该整份保存随后失败并回退序号，这些结果会被重新放回并回滚内存。
			if (result.sequence() <= lastSyncedSequence.get()) {
				deferFailure(result);
				continue;
			}
			// 之后已提交了新修改（可能尚未写盘）：不回滚，避免把更新的值一起冲掉。
			if (result.sequence() < WRITE_SEQUENCE.get()) {
				continue;
			}
			TweakifyModConfig config = TweakifyMod.config();
			if (TweakifyMod.isConfigLoaded()) {
				applyFailedWriteRollback(config, result.snapshot(), result.afterSnapshot());
			}
		}
	}

	// 异步写盘失败后的统一收敛：字段级回滚 + 刷新待写快照 + 兜底写当前内存。
	// 刷新 pendingSnapshot 很关键：否则下一批 flush 仍以“含回滚前值”的旧快照为 before，
	// 会把刚回滚的字段再次当成“本次修改”写回磁盘（静默复活）。
	static void applyFailedWriteRollback(TweakifyModConfig config, TweakifyModConfig.Snapshot before,
										 TweakifyModConfig.Snapshot after) {
		config.rollbackChangedFieldsIfUnchanged(before, after);
		syncFromConfig();
		if (savePending) {
			pendingSnapshot = config.snapshot();
		}
		config.submitSaveAsync();
	}

	// 包私有：测试模拟“一批修改尚未写盘”。
	static void setPendingForTest(TweakifyModConfig.Snapshot snapshot) {
		savePending = true;
		pendingSnapshot = snapshot;
	}

	// 包私有：测试确认 pending 快照已经与当前内存一致。
	static boolean pendingMatchesForTest(TweakifyModConfig config) {
		return pendingSnapshot != null && pendingSnapshot.hasSameValuesAs(config.snapshot());
	}

	// 包私有：测试注入一条写盘结果，验证序号守卫与回滚路径。
	static void enqueueSaveResultForTest(boolean success, TweakifyModConfig.Snapshot snapshot,
										 TweakifyModConfig.Snapshot afterSnapshot, long sequence) {
		SAVE_RESULTS.add(new SaveResult(success, snapshot, afterSnapshot, sequence));
	}

	// 整份保存期间被跳过的失败结果不能直接丢弃；有界保留，回退序号后重放。
	private static void deferFailure(SaveResult result) {
		DEFERRED_RESULTS.add(result);
		while (DEFERRED_RESULTS.size() > MAX_DEFERRED_RESULTS) {
			DEFERRED_RESULTS.poll();
			TweakifyMod.LOGGER.warn("Deferred save-result queue overflow; dropping the oldest failure");
		}
	}

	// 整份保存成功覆盖到 sequence：延迟结果已无回滚价值，丢弃。
	private static void discardDeferredUpTo(long sequence) {
		DEFERRED_RESULTS.removeIf(result -> result.sequence() <= sequence);
	}

	// 回退序号后重放本次推进覆盖的失败结果，让后续 poll 能重新回滚内存。
	private static void replayDeferredFailures(long previousSequence, long advancedSequence) {
		for (SaveResult result : DEFERRED_RESULTS) {
			if (result.sequence() > previousSequence && result.sequence() <= advancedSequence
				&& DEFERRED_RESULTS.remove(result)) {
				SAVE_RESULTS.add(result);
			}
		}
	}

	// 包私有：测试之间清理写盘节流与序号状态，避免跨测试互相影响。
	static void resetForTest() {
		savePending = false;
		pendingSnapshot = null;
		lastSaveAtMillis = 0L;
		WRITE_SEQUENCE.set(0L);
		lastSyncedSequence.set(0L);
		IN_FLIGHT_FLUSHES.set(0);
		SAVE_RESULTS.clear();
		DEFERRED_RESULTS.clear();
	}

	// 客户端退出（同步版）：用于测试、诊断与收尾线程超时后的同步兜底；
	// 正常退出路径改用 flushOnShutdownAsync，避免在关闭流程里等待磁盘 IO。
	public static void flushOnShutdown() {
		savePending = false;
		pendingSnapshot = null;
		if (!TweakifyMod.isConfigLoaded()) {
			return;
		}
		TweakifyModConfig config = TweakifyMod.config();
		if (config.saveForShutdown()) {
			return;
		}
		// 先停止写盘线程（等待/丢弃），再同步写，避免旧任务在同步写之后覆盖新内容。
		if (!AsyncFileWriter.shutdown(2000L)) {
			TweakifyMod.LOGGER.warn("File writer did not terminate before the synchronous fallback write");
		}
		Boolean sync = config.writeSynchronously();
		if (!Boolean.TRUE.equals(sync)) {
			TweakifyMod.LOGGER.error("Failed to write Tweakify Mod configuration synchronously on shutdown");
		}
	}

	// 客户端退出（非阻塞）：把最终完整快照提交到串行写盘队列后立即返回。
	// 队列由 AsyncFileWriter.shutdownAsync 的非 daemon 收尾线程在后台排空，
	// 关闭流程不再因磁盘 IO 卡住；磁盘异常时最多由收尾线程等待超时后放弃。
	public static void flushOnShutdownAsync() {
		savePending = false;
		pendingSnapshot = null;
		if (!TweakifyMod.isConfigLoaded()) {
			return;
		}
		TweakifyModConfig config = TweakifyMod.config();
		if (!config.submitSaveAsync()) {
			TweakifyMod.LOGGER.error("Failed to queue the final config snapshot on shutdown");
		}
	}
}
