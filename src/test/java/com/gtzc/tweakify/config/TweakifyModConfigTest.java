package com.gtzc.tweakify.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gtzc.tweakify.util.AsyncFileWriter;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TweakifyModConfigTest {
	// CI 的共享 runner 可能在线程调度与慢盘上停顿数百毫秒；等待上限放宽只影响失败场景的耗时，
	// 正常路径仍然立即返回。
	private static final long DRAIN_TIMEOUT_MILLIS = 15_000L;
	private static final long START_TIMEOUT_SECONDS = 10L;
	// 锁未被占用时 setter 应立即完成；上限取 1500ms（低于 save() 内部 2000ms 的等待超时），
	// 保留对「有界持锁」的判别力。
	private static final long PROBE_TIMEOUT_MILLIS = 1_500L;

	private static void awaitQuietly(CountDownLatch latch) {
		try {
			latch.await(START_TIMEOUT_SECONDS, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	// 轮询线程状态：只要状态在超时前到达就返回 true，不依赖单次采样时机。
	private static boolean awaitThreadState(Thread thread, Thread.State state, long timeoutSeconds)
			throws InterruptedException {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
		while (thread.getState() != state && System.nanoTime() < deadline) {
			Thread.sleep(5L);
		}
		return thread.getState() == state;
	}

	// 读取线程此刻持有的监视器列表：直接判定「等待期间是否持锁」，不依赖任何耗时阈值。
	// ThreadMXBean 的三参重载只接受 id 数组，且 Thread.getId() 在 Java 19 起已废弃。
	private static boolean holdsMonitor(Thread thread, Object monitor) {
		ThreadInfo[] infos = ManagementFactory.getThreadMXBean()
			.getThreadInfo(new long[] {thread.threadId()}, true, false);
		if (infos == null || infos.length == 0 || infos[0] == null) {
			return false;
		}
		int target = System.identityHashCode(monitor);
		return Arrays.stream(infos[0].getLockedMonitors())
			.anyMatch(locked -> locked.getIdentityHashCode() == target);
	}

	@BeforeAll
	static void bootstrapMinecraft() {
		// 注册表在首次访问前必须初始化，否则 BuiltInRegistries 会抛异常；
		// bootStrap 依赖版本信息，单测环境下需先手动探测。
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@TempDir
	Path tempDir;

	private Path configPath() {
		return tempDir.resolve("tweakify-mod.json");
	}

	// --- load() ---

	@Test
	void loadCreatesDefaultFileWhenMissing() {
		Path path = configPath();
		TweakifyModConfig config = TweakifyModConfig.load(path);

		assertTrue(Files.exists(path), "default config file should be created");
		assertTrue(config.functions.blockOffhandFallback);
		assertEquals("zh_cn", config.general.language);
		assertTrue(config.general.followSystemLanguage);
		assertFalse(config.functions.interceptBlockInteraction);
		assertFalse(config.functions.interceptEntityInteraction);
		assertFalse(config.functions.miningCooldown);
		assertTrue(config.functions.useCooldown);
		assertFalse(config.functions.mainHandUseCooldown);
		assertFalse(config.functions.offhandUseCooldown);
		assertFalse(config.modified.disableUseItemSlowdown);
		assertFalse(config.modified.disableHurtShake);
		assertFalse(config.functions.disableFlightDrift);
		assertFalse(config.functions.flightDriftVertical);
		assertFalse(config.modified.disableHurtTint);
		assertFalse(config.functions.disableDimensionTeleport);
		assertTrue(config.functions.disableNetherPortalTeleport);
		assertTrue(config.functions.disableEndPortalTeleport);
		assertFalse(config.modified.disableUnderwaterMiningPenalty);
		assertFalse(config.modified.disableAirMiningPenalty);
		assertFalse(config.functions.attackRangeDisplay);
		assertEquals("flat", config.functions.attackRangeDisplayMode);
		assertEquals("wireframe", config.functions.attackRangeRenderMode);
		assertFalse(config.functions.attackRangeIncludeSelf);
		assertEquals(48, config.functions.attackRangeMaxDistance);
		assertEquals(64, config.functions.attackRangeMaxTargets);
		assertEquals(32, config.functions.highlightSingleLayerBedrockMaxMarkers);
		assertTrue(config.getAttackRangeEntities().isEmpty());
		assertFalse(config.modified.nightVisionFadeOut);
		assertFalse(config.modified.enchantmentLevelArabic);
		assertFalse(config.fix.fixBoatStuckOnDirtPath);
		assertTrue(config.getNetherPortalTeleportBlacklist().isEmpty());
		assertTrue(config.getEndPortalTeleportBlacklist().isEmpty());
		assertTrue(config.getEndGatewayTeleportBlacklist().isEmpty());
		assertTrue(config.functions.disableEndGatewayTeleport, "end gateway teleport is blocked by default");
		assertEquals("G,C", config.general.hotkeyOpenConfigGui, "open config hotkey should default to G + C");
		assertEquals("", config.functions.hotkeyMiningCooldown, "toggle hotkeys should default to unset");
		assertEquals("", config.functions.hotkeyUseCooldown);
		assertEquals("", config.functions.hotkeyPickFluidBucket);
		assertEquals("", config.functions.hotkeySurvivalPickBlock);
		assertEquals("", config.modified.hotkeyDisableOverlayEffects);
		assertEquals("", config.functions.hotkeyDisableFlightDrift);
		List<String> items = config.getRestrictedOffhandItems();
		assertTrue(items.contains("minecraft:torch"), "default list should contain torch");
		assertEquals(items.size(), items.stream().distinct().count(), "default list must not contain duplicates");
		assertTrue(config.getMainHandUseCooldownItems().contains("minecraft:stone"), "main hand list should contain stone");
		assertTrue(config.getOffhandUseCooldownItems().contains("minecraft:stone"), "offhand list should contain stone");
	}

	@Test
	void loadUpgradesMissingConfigVersion() throws IOException {
		Path path = configPath();
		Files.writeString(path, "{\"language\":\"en_us\"}");

		TweakifyModConfig config = TweakifyModConfig.load(path);

		assertEquals(TweakifyModConfig.CURRENT_CONFIG_VERSION, config.configVersion);
		assertTrue(Files.readString(path).contains("\"configVersion\""),
			"upgraded version should be written back");
	}

	// 旧版“运动相机横移倾斜 / 创造物品栏条目合并”字段已移除，读取时必须迁移到新开关与快捷键。
	@Test
	void loadMigratesLegacyCameraAndItemGroupKeys() throws IOException {
		Path path = configPath();
		Files.writeString(path, """
			{
			  "configVersion": 2,
			  "motionCameraTilt": true,
			  "hotkeyMotionCameraTilt": "K",
			  "mergeCreativeItemEntries": true,
			  "hotkeyMergeCreativeItemEntries": "L"
			}
			""");

		TweakifyModConfig config = TweakifyModConfig.load(path);

		assertTrue(config.functions.cameraMotion, "legacy motionCameraTilt should migrate to cameraMotion");
		assertEquals("K", config.functions.hotkeyCameraMotion);
		assertTrue(config.functions.itemGroups, "legacy mergeCreativeItemEntries should migrate to itemGroups");
		assertEquals("L", config.functions.hotkeyItemGroups);
		assertEquals(TweakifyModConfig.CURRENT_CONFIG_VERSION, config.configVersion);
		String rewritten = Files.readString(path);
		assertTrue(rewritten.contains("\"cameraMotion\": true"), "migrated value should be written back");
		assertTrue(rewritten.contains("\"itemGroups\": true"));
		assertTrue(rewritten.contains("\"hotkeyCameraMotion\": \"K\""));
		assertTrue(rewritten.contains("\"hotkeyItemGroups\": \"L\""));
	}

	@Test
	void loadIgnoresRemovedMiniHudHotkey() throws IOException {
		Path path = configPath();
		Files.writeString(path, """
			{
			  "configVersion": 3,
			  "miniHudFpsOptimization": true,
			  "hotkeyMiniHudFpsOptimization": "M"
			}
			""");

		TweakifyModConfig config = TweakifyModConfig.load(path);

		assertTrue(config.optimization.miniHudFpsOptimization, "optimization switch should still load");
		String rewritten = Files.readString(path);
		assertFalse(rewritten.contains("hotkeyMiniHudFpsOptimization"),
			"obsolete hotkey field should be removed on the next save");
	}

	@Test
	void loadParsesValidConfig() throws IOException {
		Path path = configPath();
		Files.writeString(path, """
			{
			  "blockOffhandFallback": false,
			  "interceptBlockInteraction": false,
			  "interceptEntityInteraction": true,
			  "restrictedOffhandItems": ["minecraft:torch", "minecraft:lantern"]
			}
			""");

		TweakifyModConfig config = TweakifyModConfig.load(path);

		assertFalse(config.functions.blockOffhandFallback);
		assertFalse(config.functions.interceptBlockInteraction);
		assertTrue(config.functions.interceptEntityInteraction);
		assertEquals(List.of("minecraft:torch", "minecraft:lantern"), config.getRestrictedOffhandItems());
	}

	@Test
	void loadDefaultsMissingInteractionSwitches() throws IOException {
		Path path = configPath();
		Files.writeString(path, """
			{
			  "restrictedOffhandItems": ["minecraft:torch"]
			}
			""");

		TweakifyModConfig config = TweakifyModConfig.load(path);

		assertFalse(config.functions.interceptBlockInteraction, "block interaction interception should default to off");
		assertFalse(config.functions.interceptEntityInteraction, "entity interaction interception should default to off");
		assertEquals("G,C", config.general.hotkeyOpenConfigGui, "open config hotkey should default to G + C");
		assertEquals("", config.functions.hotkeyUseCooldown, "toggle hotkeys should default to unset");
		assertEquals("", config.functions.hotkeyMiningCooldown);
		assertEquals("", config.functions.hotkeySurvivalPickBlock);
	}

	// 旧配置缺少物品列表键：必须补回默认值，
	// 否则“受限副手物品”与“主/副手使用冷却”会因空列表而静默失效。
	@Test
	void loadFillsMissingItemListDefaults() throws IOException {
		Path path = configPath();
		Files.writeString(path, """
			{
			  "configVersion": 2,
			  "language": "en_us"
			}
			""");

		TweakifyModConfig config = TweakifyModConfig.load(path);

		assertTrue(config.getRestrictedOffhandItems().contains("minecraft:torch"),
			"missing list key should be filled with the light source defaults");
		assertTrue(config.getMainHandUseCooldownItems().contains("minecraft:stone"),
			"missing main hand list should be filled with the interactable defaults");
		assertTrue(config.getOffhandUseCooldownItems().contains("minecraft:stone"),
			"missing offhand list should be filled with the interactable defaults");
		String rewritten = Files.readString(path);
		assertTrue(rewritten.contains("\"restrictedOffhandItems\""),
			"filled defaults should be written back to disk");
	}

	// 用户主动写成空数组的列表必须保持为空，不能被默认值覆盖。
	@Test
	void loadKeepsExplicitlyEmptyItemLists() throws IOException {
		Path path = configPath();
		Files.writeString(path, """
			{
			  "configVersion": 2,
			  "restrictedOffhandItems": [],
			  "mainHandUseCooldownItems": [],
			  "offhandUseCooldownItems": []
			}
			""");

		TweakifyModConfig config = TweakifyModConfig.load(path);

		assertTrue(config.getRestrictedOffhandItems().isEmpty(),
			"explicitly empty list must be preserved");
		assertTrue(config.getMainHandUseCooldownItems().isEmpty());
		assertTrue(config.getOffhandUseCooldownItems().isEmpty());
	}

	@Test
	void loadParsesHotkeysAndSaveRoundTrips() throws IOException {
		Path path = configPath();
		Files.writeString(path, """
			{
			  "hotkeyMiningCooldown": "X",
			  "hotkeyUseCooldown": "Y",
			  "hotkeySurvivalPickBlock": "Z",
			  "hotkeyOpenConfigGui": "G,C"
			}
			""");

		TweakifyModConfig config = TweakifyModConfig.load(path);

		assertEquals("X", config.functions.hotkeyMiningCooldown);
		assertEquals("Y", config.functions.hotkeyUseCooldown);
		assertEquals("Z", config.functions.hotkeySurvivalPickBlock);
		assertEquals("G,C", config.general.hotkeyOpenConfigGui);

		config.general.hotkeyOpenConfigGui = "H,J";
		assertTrue(config.save());
		TweakifyModConfig reloaded = TweakifyModConfig.load(path);
		assertEquals("H,J", reloaded.general.hotkeyOpenConfigGui);
		assertEquals("X", reloaded.functions.hotkeyMiningCooldown);
	}

	@Test
	void loadBacksUpBrokenJsonAndWritesDefaults() throws IOException {
		Path path = configPath();
		Files.writeString(path, "{ this is not json");

		TweakifyModConfig config = TweakifyModConfig.load(path);

		assertTrue(config.functions.blockOffhandFallback, "fallback defaults should apply");
		assertTrue(Files.exists(path), "a new default config should be written");
		try (Stream<Path> backups = Files.list(tempDir)) {
			long backupCount = backups
				.filter(p -> p.getFileName().toString().startsWith("tweakify-mod.json.broken-"))
				.count();
			assertEquals(1, backupCount, "broken config should be backed up exactly once");
		}
	}

	// 列表里混入数字/布尔元素时，旧版 Gson 会按 List<String> 泛型把它们转成字符串再清洗；
	// 拆分后必须保持同样行为，不能因为元素类型不匹配把整份配置判定为损坏。
	@Test
	void loadCoercesNonStringListElements() throws IOException {
		Path path = configPath();
		Files.writeString(path, "{\"restrictedOffhandItems\": [1, true]}");

		TweakifyModConfig config = TweakifyModConfig.load(path);

		assertEquals(List.of("minecraft:1", "minecraft:true"), config.getRestrictedOffhandItems());
		try (Stream<Path> files = Files.list(tempDir)) {
			assertFalse(files.anyMatch(file -> file.getFileName().toString().contains(".broken-")),
				"non-string list elements must not be treated as a broken config");
		}
	}

	// 列表字段在配置文件里写成 null：必须回写空列表，不能留到 rebuildCache 抛 NPE。
	@Test
	void loadReplacesNullItemListWithEmptyList() throws IOException {
		Path path = configPath();
		Files.writeString(path, """
			{
			  "restrictedOffhandItems": null,
			  "highlightPreset1Entities": null
			}
			""");

		TweakifyModConfig config = TweakifyModConfig.load(path);

		assertTrue(config.getRestrictedOffhandItems().isEmpty(), "null list should load as an empty list");
		assertTrue(config.getHighlightPreset1Entities().isEmpty());
		String rewritten = Files.readString(path);
		assertTrue(rewritten.contains("\"restrictedOffhandItems\": []"),
			"null list should be rewritten as an empty array");
	}

	// 快捷键字段在配置文件里写成 null：必须回写空字符串。
	// 否则 MaLiLib 的 KeybindMulti.setValueFromString 会在 String.split 处抛 NPE，进游戏即崩。
	@Test
	void loadReplacesNullHotkeyWithEmptyString() throws IOException {
		Path path = configPath();
		Files.writeString(path, """
			{
			  "hotkeyOpenConfigGui": null,
			  "hotkeyMiningCooldown": null
			}
			""");

		TweakifyModConfig config = TweakifyModConfig.load(path);

		assertEquals("", config.general.hotkeyOpenConfigGui, "null hotkey should load as an empty binding");
		assertEquals("", config.functions.hotkeyMiningCooldown);
		String rewritten = Files.readString(path);
		assertTrue(rewritten.contains("\"hotkeyOpenConfigGui\": \"\""),
			"null hotkey should be rewritten as an empty string");
	}

	// 列表字段写成非数组（类型不匹配）：整个配置回退默认值，并把损坏文件备份。
	@Test
	void loadFallsBackWhenListHasWrongType() throws IOException {
		Path path = configPath();
		Files.writeString(path, """
			{
			  "restrictedOffhandItems": 42
			}
			""");

		TweakifyModConfig config = TweakifyModConfig.load(path);

		assertTrue(config.getRestrictedOffhandItems().contains("minecraft:torch"),
			"defaults should be restored when a list has the wrong JSON type");
		try (Stream<Path> backups = Files.list(tempDir)) {
			assertEquals(1, backups
				.filter(p -> p.getFileName().toString().startsWith("tweakify-mod.json.broken-"))
				.count(), "broken config should be backed up once");
		}
	}

	@Test
	void loadHandlesNullRoot() throws IOException {
		Path path = configPath();
		Files.writeString(path, "null");

		TweakifyModConfig config = TweakifyModConfig.load(path);

		assertTrue(config.functions.blockOffhandFallback);
		assertTrue(Files.exists(path));
	}

	@Test
	void loadSanitizesAndRewritesConfig() throws IOException {
		Path path = configPath();
		Files.writeString(path, """
			{
			  "restrictedOffhandItems": ["torch", "minecraft:torch", "not a valid id!", null],
			  "attackRangeMaxDistance": 9999,
			  "attackRangeMaxTargets": 1,
			  "highlightSingleLayerBedrockMaxMarkers": 9999
			}
			""");

		TweakifyModConfig config = TweakifyModConfig.load(path);

		assertEquals(List.of("minecraft:torch"), config.getRestrictedOffhandItems(),
			"ids should be normalized, de-duplicated and invalid entries dropped");
		assertEquals(128, config.functions.attackRangeMaxDistance, "attack range distance should be clamped");
		assertEquals(8, config.functions.attackRangeMaxTargets, "attack range target count should be clamped");
		assertEquals(256, config.functions.highlightSingleLayerBedrockMaxMarkers, "bedrock marker count should be clamped");
		String rewritten = Files.readString(path);
		assertFalse(rewritten.contains("not a valid id"), "corrected config should be rewritten to disk");
		assertFalse(rewritten.contains("9999"), "clamped values should be rewritten to disk");
	}

	// 新功能开关的默认值与快捷键默认值。
	@Test
	void loadDefaultsNewFeatureFields() {
		TweakifyModConfig config = TweakifyModConfig.load(configPath());

		assertFalse(config.functions.piglinGoldArmorSwap);
		assertEquals(TweakifyModConfig.PIGLIN_ARMOR_SWAP_AUTO, config.functions.piglinGoldArmorSwapMode);
		assertFalse(config.functions.chatInputMemory);
		assertTrue(config.functions.disabledSpawnAllowPlayerSpawn, "player spawning should be allowed by default");
		assertEquals("", config.functions.hotkeyPiglinGoldArmorSwap);
		assertEquals("", config.functions.hotkeyChatInputMemory);
	}

	// 新功能的枚举字段同样需要清洗：非法模式回退。
	@Test
	void loadSanitizesNewFeatureFields() throws IOException {
		Path path = configPath();
		Files.writeString(path, """
			{
			  "piglinGoldArmorSwapMode": "not-a-mode"
			}
			""");

		TweakifyModConfig config = TweakifyModConfig.load(path);

		assertEquals(TweakifyModConfig.PIGLIN_ARMOR_SWAP_AUTO, config.functions.piglinGoldArmorSwapMode,
			"unsupported mode should fall back to auto");
	}

	@Test
	void loadKeepsExistingFileOnReadError() throws IOException {
		// 把配置路径指向一个目录：读取会抛 IOException，原路径必须原样保留。
		Path dir = tempDir.resolve("config-is-a-directory");
		Files.createDirectories(dir);

		TweakifyModConfig config = TweakifyModConfig.load(dir);

		assertTrue(config.functions.blockOffhandFallback, "in-memory defaults should still apply");
		assertTrue(Files.isDirectory(dir), "existing path must not be replaced or backed up");
	}

	// --- save() / restoreSnapshot() ---

	@Test
	void saveWritesReadableConfig() {
		Path path = configPath();
		TweakifyModConfig config = new TweakifyModConfig(path);
		config.functions.blockOffhandFallback = false;
		config.functions.interceptBlockInteraction = false;
		config.functions.interceptEntityInteraction = true;
		config.replaceRestrictedOffhandItems(List.of("minecraft:torch"));

		assertTrue(config.save());

		TweakifyModConfig reloaded = TweakifyModConfig.load(path);
		assertFalse(reloaded.functions.blockOffhandFallback);
		assertFalse(reloaded.functions.interceptBlockInteraction);
		assertTrue(reloaded.functions.interceptEntityInteraction);
		assertEquals(List.of("minecraft:torch"), reloaded.getRestrictedOffhandItems());
	}

	// 退出兜底：不经过写盘队列，直接同步写当前内存快照。
	@Test
	void writeSynchronouslyWritesCurrentSnapshot() {
		Path path = configPath();
		TweakifyModConfig config = new TweakifyModConfig(path);
		config.functions.blockOffhandFallback = false;
		config.general.hotkeyOpenConfigGui = "H,J";

		assertEquals(Boolean.TRUE, config.writeSynchronously());

		TweakifyModConfig reloaded = TweakifyModConfig.load(path);
		assertFalse(reloaded.functions.blockOffhandFallback);
		assertEquals("H,J", reloaded.general.hotkeyOpenConfigGui);
	}

	// 退出同步兜底写分配更晚的落盘序号：真正入队但延迟执行的旧任务必须被跳过，
	// 不能在同步写之后把磁盘覆盖回旧快照。
	@Test
	void writeSynchronouslyWinsOverStaleQueuedWrite() throws Exception {
		Path path = configPath();
		TweakifyModConfig config = new TweakifyModConfig(path);
		config.functions.blockOffhandFallback = false;

		CountDownLatch blocked = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		AsyncFileWriter.execute(() -> {
			blocked.countDown();
			awaitQuietly(release);
		});

		try {
			assertTrue(blocked.await(START_TIMEOUT_SECONDS, TimeUnit.SECONDS), "the writer should be blocked");
			// 真正入队：序号必须在 prepareSaveHandle 提交时分配；
			// 若改成执行期分配，旧任务会在同步写之后拿到更大序号并把磁盘覆盖回旧值。
			AsyncFileWriter.WriteHandle stale = config.prepareSaveHandle();
			assertTrue(stale != null, "the stale snapshot should be queued");

			config.functions.blockOffhandFallback = true;
			assertEquals(Boolean.TRUE, config.writeSynchronously());

			release.countDown();
			assertTrue(Boolean.TRUE.equals(AsyncFileWriter.await(stale, DRAIN_TIMEOUT_MILLIS, false)),
				"the stale queued task should be skipped as already covered");

			assertTrue(TweakifyModConfig.load(path).functions.blockOffhandFallback,
				"the stale queued task must not overwrite the synchronous snapshot");
		} finally {
			release.countDown();
			AsyncFileWriter.drain(DRAIN_TIMEOUT_MILLIS);
		}
	}

	// 写锁被占用时必须区分“锁超时”（null）与“写盘失败”（false）。
	@Test
	void writeSynchronouslyReportsLockTimeout() throws Exception {
		TweakifyModConfig config = new TweakifyModConfig(configPath());
		ReentrantLock lock = ConfigStorage.writeLockForTest();
		CountDownLatch locked = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		Thread holder = new Thread(() -> {
			lock.lock();
			try {
				locked.countDown();
				// 有界等待：即使 finally 前的异常导致 release 未被调用，也不能永久持住全局写锁。
				release.await(START_TIMEOUT_SECONDS, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} finally {
				lock.unlock();
			}
		});
		holder.start();
		try {
			assertTrue(locked.await(START_TIMEOUT_SECONDS, TimeUnit.SECONDS), "the write lock should be held");
			assertNull(config.writeSynchronously(),
				"持有写锁时同步兜底应报告锁超时，而不是写盘失败");
		} finally {
			release.countDown();
			holder.join(TimeUnit.SECONDS.toMillis(START_TIMEOUT_SECONDS));
		}
	}

	@Test
	void saveFailureRollsBackToSnapshot() throws IOException {
		// 目标是带内容的目录：原子移动会失败，状态必须回滚到快照。
		Path dir = tempDir.resolve("unwritable-target");
		Files.createDirectories(dir);
		Files.writeString(dir.resolve("occupied"), "x");

		TweakifyModConfig config = new TweakifyModConfig(dir);
		config.functions.blockOffhandFallback = false;
		config.functions.interceptBlockInteraction = false;
		config.general.hotkeyOpenConfigGui = "H,J";
		TweakifyModConfig.Snapshot snapshot = config.snapshot();
		config.functions.blockOffhandFallback = true;
		config.functions.interceptBlockInteraction = true;
		config.general.hotkeyOpenConfigGui = "X,Y";
		config.general.language = "ru_ru";
		config.general.followSystemLanguage = false;
		config.modified.disableUseItemSlowdown = true;
		config.modified.disableHurtShake = true;
		config.functions.disableFlightDrift = true;
		config.functions.flightDriftVertical = true;
		config.modified.disableHurtTint = true;
		config.functions.disableDimensionTeleport = true;
		config.functions.disableNetherPortalTeleport = false;
		config.functions.disableEndPortalTeleport = false;
		config.modified.disableUnderwaterMiningPenalty = true;
		config.modified.disableAirMiningPenalty = true;
		config.replaceNetherPortalTeleportBlacklist(List.of("minecraft:zombie"));
		config.replaceEndPortalTeleportBlacklist(List.of("minecraft:item"));

		assertFalse(config.save(), "writing to a directory must fail");
		config.restoreSnapshot(snapshot);
		assertFalse(config.functions.blockOffhandFallback, "state must roll back to snapshot");
		assertFalse(config.functions.interceptBlockInteraction, "block interaction switch must roll back to snapshot");
		assertFalse(config.functions.interceptEntityInteraction);
		assertFalse(config.modified.disableUseItemSlowdown, "new switches must roll back to snapshot");
		assertFalse(config.modified.disableHurtShake);
		assertFalse(config.functions.disableFlightDrift);
		assertFalse(config.functions.flightDriftVertical);
		assertFalse(config.modified.disableHurtTint);
		assertFalse(config.functions.disableDimensionTeleport);
		assertTrue(config.functions.disableNetherPortalTeleport);
		assertTrue(config.functions.disableEndPortalTeleport);
		assertFalse(config.modified.disableUnderwaterMiningPenalty);
		assertFalse(config.modified.disableAirMiningPenalty);
		assertTrue(config.getNetherPortalTeleportBlacklist().isEmpty());
		assertTrue(config.getEndPortalTeleportBlacklist().isEmpty());
		assertEquals("zh_cn", config.general.language);
		assertTrue(config.general.followSystemLanguage);
		assertEquals("H,J", config.general.hotkeyOpenConfigGui, "hotkeys must roll back to snapshot");
	}

	// 等待写盘时必须释放配置锁：慢盘场景下客户端 tick 的 setter 不能被 save() 阻塞。
	@Test
	void saveDoesNotHoldConfigLockWhileWaiting() throws Exception {
		TweakifyModConfig config = new TweakifyModConfig(configPath());
		CountDownLatch writerBusy = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		AsyncFileWriter.execute(() -> {
			writerBusy.countDown();
			awaitQuietly(release);
		});

		AtomicBoolean saveResult = new AtomicBoolean();
		Thread saver = new Thread(() -> saveResult.set(config.save()));
		saver.start();
		try {
			assertTrue(writerBusy.await(START_TIMEOUT_SECONDS, TimeUnit.SECONDS),
				"the blocking write should start");

			// 等到 save 真正进入 Future.get 的等待，而不是还没入队（避免假通过）。
			assertTrue(awaitThreadState(saver, Thread.State.TIMED_WAITING, START_TIMEOUT_SECONDS),
				"save should be waiting for the writer thread");

			// 核心契约：等待写盘期间不能持有配置监视器。
			// 直接读线程转储里的 lockedMonitors，不依赖耗时阈值：
			// 只要 await 被移进 synchronized 块，无论持锁多久都会在这里被捕获。
			assertFalse(holdsMonitor(saver, config),
				"save() must not hold the config monitor while waiting for the writer");

			// 补充证据：另一个线程的 setter 必须能在有限时间内完成（锁未被占用）。
			CountDownLatch probeDone = new CountDownLatch(1);
			Thread probe = new Thread(() -> {
				config.setBoolean("betterBold", true);
				probeDone.countDown();
			});
			probe.start();
			assertTrue(probeDone.await(PROBE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS),
				"setBoolean must not block while save() is waiting for the writer");
			assertTrue(saver.isAlive(), "save should still be waiting when the setter finishes");
		} finally {
			// 任一断言失败也要放行：写盘线程是两个测试类共用的静态单线程，
			// 卡在闸门上会把后续用例的等待窗口一起吃掉。
			release.countDown();
		}
		saver.join(TimeUnit.SECONDS.toMillis(START_TIMEOUT_SECONDS));
		assertFalse(saver.isAlive(), "save should finish after the blocking write");
		assertTrue(saveResult.get(), "save should succeed once the queue drains");
	}

	// 异步写盘路径：序列化与落盘分开后，两者组合必须与同步 save() 结果一致。
	@Test
	void toJsonAndWriteJsonRoundTripLikeSave() {
		Path path = configPath();
		TweakifyModConfig config = new TweakifyModConfig(path);
		config.functions.blockOffhandFallback = false;
		config.general.hotkeyOpenConfigGui = "H,J";
		config.replaceMainHandUseCooldownItems(List.of("minecraft:apple"));

		String json = config.toJson();
		assertTrue(json.contains("\"blockOffhandFallback\": false"),
			"toJson should serialize the current values");
		assertTrue(config.writeJson(json, config.allocateWriteSequence()),
			"writeJson should write the given text");

		TweakifyModConfig reloaded = TweakifyModConfig.load(path);
		assertFalse(reloaded.functions.blockOffhandFallback);
		assertEquals("H,J", reloaded.general.hotkeyOpenConfigGui);
		assertEquals(List.of("minecraft:apple"), reloaded.getMainHandUseCooldownItems());
	}

	// 保存失败/超时回滚后提交的兜底快照最终必须落盘（异步，不阻塞调用方）。
	@Test
	void submitSaveAsyncWritesCurrentSnapshot() throws IOException {
		Path path = configPath();
		TweakifyModConfig config = new TweakifyModConfig(path);
		config.functions.blockOffhandFallback = false;
		config.general.hotkeyOpenConfigGui = "H,J";

		config.submitSaveAsync();

		// 写盘队列是串行的：drain 提交的屏障排在此次写盘之后，返回即代表写盘已结束。
		// 直接读文件断言，不再轮询文件内容（轮询会引入与写盘时机无关的 flaky）。
		assertTrue(AsyncFileWriter.drain(DRAIN_TIMEOUT_MILLIS), "the queued write should finish");

		// 写盘失败时文件不存在：先单独断言存在性，避免只抛一个没有上下文的 NoSuchFileException。
		assertTrue(Files.exists(path), "submitSaveAsync should create the config file");
		String written = Files.readString(path);
		assertTrue(written.contains("\"blockOffhandFallback\": false")
			&& written.contains("\"hotkeyOpenConfigGui\": \"H,J\""),
			"submitSaveAsync should write the current snapshot; file content: " + written);
	}

	// restoreSnapshot 只回滚内存：异步写盘失败时由客户端线程调用，不能又触发一次磁盘写入。
	@Test
	void restoreSnapshotRollsBackMemoryWithoutWritingDisk() throws IOException {
		Path path = configPath();
		TweakifyModConfig config = new TweakifyModConfig(path);
		config.functions.blockOffhandFallback = true;
		config.replaceRestrictedOffhandItems(List.of("minecraft:torch"));
		assertTrue(config.save());
		String onDisk = Files.readString(path);

		TweakifyModConfig.Snapshot snapshot = config.snapshot();
		config.functions.blockOffhandFallback = false;
		config.general.hotkeyOpenConfigGui = "X,Y";
		config.replaceRestrictedOffhandItems(List.of("minecraft:lantern"));

		config.restoreSnapshot(snapshot);

		assertTrue(config.functions.blockOffhandFallback, "restoreSnapshot must roll back in-memory values");
		assertEquals("G,C", config.general.hotkeyOpenConfigGui);
		assertEquals(List.of("minecraft:torch"), config.getRestrictedOffhandItems());
		assertEquals(onDisk, Files.readString(path), "restoreSnapshot must not touch the config file");
	}

	// 命令路径：没有真正改变时不写盘，成功时改动会落盘。
	@Test
	void applyAndSaveReportsUnchangedWithoutWriting() {
		Path path = configPath();
		TweakifyModConfig config = new TweakifyModConfig(path);
		config.functions.blockOffhandFallback = true;

		assertEquals(TweakifyModConfig.ApplyResult.UNCHANGED,
			config.applyAndSave(() -> false), "no change should skip writing");
		assertTrue(config.functions.blockOffhandFallback);

		assertEquals(TweakifyModConfig.ApplyResult.SAVED, config.applyAndSave(() -> {
			config.setBoolean("blockOffhandFallback", false);
			return true;
		}));
		assertFalse(config.functions.blockOffhandFallback);
		assertFalse(TweakifyModConfig.load(path).functions.blockOffhandFallback, "saved value must reach disk");
	}

	// 写盘失败时只回滚命令改动的那个字段，其它字段保持调用方设置的值。
	@Test
	void applyAndSaveRollsBackOnlyMutatedFieldOnFailure() throws IOException {
		// 目标是带内容的目录：原子移动会失败，从而触发回滚分支。
		Path dir = tempDir.resolve("command-rollback-target");
		Files.createDirectories(dir);
		Files.writeString(dir.resolve("occupied"), "x");

		TweakifyModConfig config = new TweakifyModConfig(dir);
		config.functions.blockOffhandFallback = true;
		config.modified.disableHurtShake = true;

		TweakifyModConfig.ApplyResult result = config.applyAndSave(() -> {
			config.setBoolean("blockOffhandFallback", false);
			return true;
		});

		assertEquals(TweakifyModConfig.ApplyResult.SAVE_FAILED, result);
		assertTrue(config.functions.blockOffhandFallback, "the mutated field must roll back");
		assertTrue(config.modified.disableHurtShake, "unrelated fields must not be touched by the rollback");
		// applyAndSave 失败后会异步提交兜底快照；等待它结束，避免临时文件残留影响 @TempDir 清理。
		assertTrue(AsyncFileWriter.drain(DRAIN_TIMEOUT_MILLIS));
	}

	// 写盘等待期间被其它线程改过的字段不能被回滚覆盖：只有仍未变的字段回到 before。
	@Test
	void rollbackChangedFieldsKeepsConcurrentEdits() {
		TweakifyModConfig config = new TweakifyModConfig(configPath());
		config.functions.blockOffhandFallback = true;
		config.modified.disableHurtShake = false;
		TweakifyModConfig.Snapshot before = config.snapshot();

		config.functions.blockOffhandFallback = false;
		config.modified.disableHurtShake = true;
		TweakifyModConfig.Snapshot after = config.snapshot();

		// 模拟等待期间另一线程把 blockOffhandFallback 改回 true（当前值 != after）。
		config.functions.blockOffhandFallback = true;

		config.rollbackChangedFieldsIfUnchanged(before, after);

		assertTrue(config.functions.blockOffhandFallback, "concurrent edit must be preserved, not rolled back");
		assertFalse(config.modified.disableHurtShake, "unchanged field should roll back to the before snapshot");
	}

	// 档案加载只覆盖档案里存在的字段：旧档案缺少的新字段保持当前值，
	// 版本号与未知字段名都会被跳过。
	@Test
	void applyFromOnlyOverridesFieldsPresentInProfile() {
		TweakifyModConfig current = new TweakifyModConfig(configPath());
		current.functions.miningCooldown = true;
		current.modified.disableHurtShake = true;

		TweakifyModConfig profile = new TweakifyModConfig(configPath());
		profile.modified.disableHurtShake = false;

		current.applyFrom(profile, Set.of("disableHurtShake", "configVersion", "noSuchField"));

		assertFalse(current.modified.disableHurtShake, "field present in profile must be overridden");
		assertTrue(current.functions.miningCooldown, "field missing from profile must keep current value");
	}

	// 档案里列表写成 null 时，applyFrom 必须保持当前值而不是写入 null：
	// 否则 rebuildCache 会在每 tick 的热路径上抛 NPE。
	@Test
	void applyFromIgnoresNullFields() {
		TweakifyModConfig current = new TweakifyModConfig(configPath());
		current.replaceRestrictedOffhandItems(List.of("minecraft:torch"));

		TweakifyModConfig profile = new TweakifyModConfig(configPath());
		setRawField(profile, "restrictedOffhandItems", null);
		profile.general.language = null;

		current.applyFrom(profile, Set.of("restrictedOffhandItems", "language"));

		assertEquals(List.of("minecraft:torch"), current.getRestrictedOffhandItems(),
			"null list in profile must not overwrite the current list");
		assertEquals("zh_cn", current.general.language,
			"null string in profile must not clear the current value");
	}

	// 档案列表里含 null 元素时，applyFrom 必须过滤掉而不是让 List.copyOf 抛 NPE。
	@Test
	void applyFromFiltersNullElementsInList() {
		List<String> withNull = new ArrayList<>();
		withNull.add("minecraft:torch");
		withNull.add(null);

		TweakifyModConfig current = new TweakifyModConfig(configPath());
		TweakifyModConfig profile = new TweakifyModConfig(configPath());
		setRawField(profile, "restrictedOffhandItems", withNull);

		current.applyFrom(profile, Set.of("restrictedOffhandItems"));

		assertEquals(List.of("minecraft:torch"), current.getRestrictedOffhandItems());
	}

	// 序列化必须保持扁平：字段拆进分区对象后 JSON 键仍等于字段名，不允许出现嵌套对象。
	@Test
	void jsonStaysFlatAndCoversEveryNonNullField() {
		TweakifyModConfig config = new TweakifyModConfig(configPath());

		JsonObject json = JsonParser.parseString(config.toJson()).getAsJsonObject();

		Set<String> expected = TweakifyModConfig.fieldRefs().entrySet().stream()
			.filter(entry -> entry.getValue().get(config) != null)
			.map(Map.Entry::getKey)
			.collect(Collectors.toCollection(LinkedHashSet::new));
		assertEquals(expected, json.keySet(), "JSON keys must match the flat field table");
		for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
			assertFalse(entry.getValue().isJsonObject(),
				"nested JSON object found for field: " + entry.getKey());
		}
	}

	// 直接按字段名写入原始值：模拟 Gson 把 JSON null / 非法内容赋给字段的结果。
	private static void setRawField(TweakifyModConfig config, String name, Object value) {
		config.setFieldRaw(name, value);
	}

	// 档案里数值越界时，applyFrom 必须复用 load() 的夹紧逻辑。
	@Test
	void applyFromClampsOutOfRangeNumbers() {
		TweakifyModConfig current = new TweakifyModConfig(configPath());
		TweakifyModConfig profile = new TweakifyModConfig(configPath());
		profile.functions.attackRangeMaxDistance = 9999;
		profile.functions.attackRangeMaxTargets = 1;

		current.applyFrom(profile, Set.of("attackRangeMaxDistance", "attackRangeMaxTargets"));

		assertEquals(TweakifyModConfig.MAX_ATTACK_RANGE_DISTANCE, current.functions.attackRangeMaxDistance);
		assertEquals(TweakifyModConfig.MIN_ATTACK_RANGE_TARGETS, current.functions.attackRangeMaxTargets);
	}

	// --- 分区校验器覆盖断言 ---

	// 测试用分区：只声明一个数值字段，构造期执行覆盖断言。
	private static final class ProbeSection extends ConfigSection {
		volatile int value = 0;

		ProbeSection(List<String> sanitizedFieldNames) {
			assertSanitizerCoverage(sanitizedFieldNames);
		}
	}

	// 登记了不存在的字段名（拼错或跨分区）时必须启动失败，而不是静默跳过。
	@Test
	void sanitizerRegisteredForUnknownFieldFailsAtConstruction() {
		IllegalStateException error = assertThrows(IllegalStateException.class,
			() -> new ProbeSection(List.of("valeu")));

		assertTrue(error.getMessage().contains("unknown config field"),
			"拼错的字段名必须被识别为未知字段：" + error.getMessage());
	}

	// 同一字段被重复登记时必须启动失败，避免 sanitize() 静默执行两遍。
	@Test
	void duplicateSanitizerRegistrationFailsAtConstruction() {
		IllegalStateException error = assertThrows(IllegalStateException.class,
			() -> new ProbeSection(List.of("value", "value")));

		assertTrue(error.getMessage().contains("more than one sanitizer"),
			"重复登记必须被识别：" + error.getMessage());
	}

	// 数值字段漏登记校验器时必须启动失败。
	@Test
	void missingSanitizerFailsAtConstruction() {
		IllegalStateException error = assertThrows(IllegalStateException.class,
			() -> new ProbeSection(List.of()));

		assertTrue(error.getMessage().contains("without sanitizer"),
			"漏登记的数值字段必须被识别：" + error.getMessage());
	}

	// 正常登记时构造成功：守卫断言本身不能误报。
	@Test
	void sanitizerCoverageAcceptsValidRegistration() {
		assertDoesNotThrow(() -> new ProbeSection(List.of("value")));
	}

	// --- 清洗 / 增删 ---

	@Test
	void addAndRemoveItemNormalizeIds() {
		TweakifyModConfig config = new TweakifyModConfig(configPath());

		assertTrue(config.addItem("torch"));
		assertEquals(List.of("minecraft:torch"), config.getRestrictedOffhandItems());
		assertFalse(config.addItem("minecraft:torch"), "duplicate must not be added");
		assertFalse(config.addItem(null));
		assertFalse(config.addItem("   "));
		assertFalse(config.addItem("invalid id with spaces!"));

		assertTrue(config.removeItem("minecraft:torch"));
		assertFalse(config.removeItem("minecraft:torch"), "already removed");
		assertFalse(config.removeItem(null));
	}

	@Test
	void getRestrictedItemsReturnsDefensiveCopy() {
		TweakifyModConfig config = new TweakifyModConfig(configPath());
		config.addItem("minecraft:torch");

		List<String> copy = config.getRestrictedOffhandItems();
		assertThrows(UnsupportedOperationException.class, () -> copy.add("minecraft:lantern"));
		assertEquals(List.of("minecraft:torch"), config.getRestrictedOffhandItems());
	}

	// 快照必须覆盖所有可持久化字段：新增字段后若回滚漏存，这里会直接失败。
	@Test
	void snapshotRestoresEveryPersistentField() throws IOException {
		// 目标是带内容的目录：原子移动会失败，从而触发回滚分支。
		Path dir = tempDir.resolve("rollback-target");
		Files.createDirectories(dir);
		Files.writeString(dir.resolve("occupied"), "x");

		TweakifyModConfig config = new TweakifyModConfig(dir);
		Map<String, Object> before = new LinkedHashMap<>();
		// 用扁平字段表而不是 getDeclaredFields()：字段拆进分区对象后，
		// 后者只能看到分区字段本身，这条测试会静默失去判别力。
		Map<String, ConfigFieldRef> fields = TweakifyModConfig.fieldRefs();
		for (Map.Entry<String, ConfigFieldRef> entry : fields.entrySet()) {
			Object value = entry.getValue().get(config);
			before.put(entry.getKey(), value instanceof List<?> list ? List.copyOf(list) : value);
		}

		TweakifyModConfig.Snapshot snapshot = config.snapshot();
		for (ConfigFieldRef ref : fields.values()) {
			Object value = ref.get(config);
			if (value instanceof Boolean bool) {
				ref.set(config, !bool);
			} else if (value instanceof String text) {
				ref.set(config, text + "-changed");
			} else if (value instanceof Integer number) {
				ref.set(config, number + 1);
			} else if (value instanceof Double number) {
				ref.set(config, number + 0.5);
			} else if (value instanceof List<?>) {
				ref.set(config, new ArrayList<>(List.of("minecraft:stone")));
			}
		}

		assertFalse(config.save(), "writing to a directory must fail");
		config.restoreSnapshot(snapshot);
		for (Map.Entry<String, ConfigFieldRef> entry : fields.entrySet()) {
			assertEquals(before.get(entry.getKey()), entry.getValue().get(config),
				entry.getKey() + " must roll back to the snapshot value");
		}
	}

	@Test
	void replaceItemsSanitizesInput() {
		TweakifyModConfig config = new TweakifyModConfig(configPath());

		config.replaceRestrictedOffhandItems(List.of("torch", "minecraft:torch", "bad id!"));

		assertEquals(List.of("minecraft:torch"), config.getRestrictedOffhandItems());

		config.replaceRestrictedOffhandItems(null);
		assertTrue(config.getRestrictedOffhandItems().isEmpty());
	}

	// 表驱动校验器必须覆盖所有需要清洗的字段：
	// 类加载期断言已经保证一次，这里再做一次正向校验，
	// 防止有人把断言降级为日志后新增数值字段再次静默漏检。
	@Test
	void fieldSanitizersCoverEveryValueField() {
		TweakifyModConfig config = new TweakifyModConfig(configPath());
		Set<String> covered = new HashSet<>(config.sanitizedFieldNames());
		for (String name : TweakifyModConfig.fieldsRequiringSanitizer()) {
			assertTrue(covered.contains(name), "config field without sanitizer: " + name);
		}
	}

	// --- 注册表相关 ---

	@Test
	void loadSanitizesBlockOutlineSettings() throws IOException {
		Path path = configPath();
		Files.writeString(path, """
			{
			  "blockOutlineWidth": 99,
			  "blockOutlineAnimationSpeed": 999,
			  "blockOutlinePalette": "not-a-color",
			  "blockOutlineColorMode": "not-a-mode"
			}
			""");

		TweakifyModConfig config = TweakifyModConfig.load(path);

		assertEquals(TweakifyModConfig.MAX_BLOCK_OUTLINE_WIDTH, config.functions.blockOutlineWidth);
		assertEquals(TweakifyModConfig.MAX_BLOCK_OUTLINE_ANIMATION_SPEED, config.functions.blockOutlineAnimationSpeed);
		assertEquals(TweakifyModConfig.BLOCK_OUTLINE_PALETTE_WHITE, config.functions.blockOutlinePalette);
		assertEquals(TweakifyModConfig.BLOCK_OUTLINE_MODE_PALETTE, config.functions.blockOutlineColorMode);
		assertTrue(Files.readString(path).contains(
			"\"blockOutlineWidth\": " + TweakifyModConfig.MAX_BLOCK_OUTLINE_WIDTH));

		Files.writeString(path, "{\"blockOutlineWidth\": -5, \"blockOutlineAnimationSpeed\": 1}");
		TweakifyModConfig low = TweakifyModConfig.load(path);
		assertEquals(TweakifyModConfig.MIN_BLOCK_OUTLINE_WIDTH, low.functions.blockOutlineWidth);
		assertEquals(TweakifyModConfig.MIN_BLOCK_OUTLINE_ANIMATION_SPEED, low.functions.blockOutlineAnimationSpeed);

		Files.writeString(path, "{\"blockOutlineWidth\": 2.3}");
		TweakifyModConfig rounded = TweakifyModConfig.load(path);
		assertEquals(2.5, rounded.functions.blockOutlineWidth, 1.0e-6);
	}

	@Test
	void setDoubleRoundsToConfiguredStep() {
		TweakifyModConfig config = new TweakifyModConfig(configPath());

		config.setDouble(TweakifyModConfig.BLOCK_OUTLINE_WIDTH_FIELD, 2.3);
		assertEquals(2.5, config.functions.blockOutlineWidth, 1.0e-6);

		config.setDouble(TweakifyModConfig.BLOCK_OUTLINE_WIDTH_FIELD, 2.2);
		assertEquals(TweakifyModConfig.DEFAULT_BLOCK_OUTLINE_WIDTH, config.functions.blockOutlineWidth, 1.0e-6);

		config.setDouble(TweakifyModConfig.BLOCK_OUTLINE_WIDTH_FIELD, 99.0);
		assertEquals(TweakifyModConfig.MAX_BLOCK_OUTLINE_WIDTH, config.functions.blockOutlineWidth, 1.0e-6);

		config.setDouble(TweakifyModConfig.BLOCK_OUTLINE_WIDTH_FIELD, -5.0);
		assertEquals(TweakifyModConfig.MIN_BLOCK_OUTLINE_WIDTH, config.functions.blockOutlineWidth, 1.0e-6);

		config.setDouble(TweakifyModConfig.BLOCK_OUTLINE_WIDTH_FIELD, Double.NaN);
		assertEquals(TweakifyModConfig.DEFAULT_BLOCK_OUTLINE_WIDTH, config.functions.blockOutlineWidth, 1.0e-6);

		config.setDouble(TweakifyModConfig.BLOCK_OUTLINE_WIDTH_FIELD, Double.POSITIVE_INFINITY);
		assertEquals(TweakifyModConfig.DEFAULT_BLOCK_OUTLINE_WIDTH, config.functions.blockOutlineWidth, 1.0e-6);
	}

	@Test
	void outlineWidthHelpersRoundAndClamp() {
		assertEquals(2.5, TweakifyModConfig.roundBlockOutlineWidth(2.3), 1.0e-6);
		assertEquals(TweakifyModConfig.MIN_BLOCK_OUTLINE_WIDTH,
			TweakifyModConfig.clampBlockOutlineWidth(-5.0), 1.0e-6);
		assertEquals(TweakifyModConfig.MAX_BLOCK_OUTLINE_WIDTH,
			TweakifyModConfig.clampBlockOutlineWidth(99.0), 1.0e-6);
		assertEquals(TweakifyModConfig.DEFAULT_BLOCK_OUTLINE_WIDTH,
			TweakifyModConfig.roundBlockOutlineWidth(Double.NaN), 1.0e-6);
		assertEquals(TweakifyModConfig.DEFAULT_BLOCK_OUTLINE_WIDTH,
			TweakifyModConfig.clampBlockOutlineWidth(Double.POSITIVE_INFINITY), 1.0e-6);
	}

	@Test
	void outlineWidthConstantsAreSelfConsistent() {
		assertTrue(TweakifyModConfig.BLOCK_OUTLINE_WIDTH_STEP > 0.0, "step must be positive");
		assertTrue((TweakifyModConfig.MAX_BLOCK_OUTLINE_WIDTH - TweakifyModConfig.MIN_BLOCK_OUTLINE_WIDTH)
			/ TweakifyModConfig.BLOCK_OUTLINE_WIDTH_STEP >= 1.0, "range must contain at least one step");
		assertTrue(TweakifyModConfig.MIN_BLOCK_OUTLINE_WIDTH < TweakifyModConfig.DEFAULT_BLOCK_OUTLINE_WIDTH);
		assertTrue(TweakifyModConfig.DEFAULT_BLOCK_OUTLINE_WIDTH < TweakifyModConfig.MAX_BLOCK_OUTLINE_WIDTH);
		assertEquals(TweakifyModConfig.MIN_BLOCK_OUTLINE_WIDTH,
			TweakifyModConfig.clampBlockOutlineWidth(TweakifyModConfig.MIN_BLOCK_OUTLINE_WIDTH), 1.0e-6);
		assertEquals(TweakifyModConfig.DEFAULT_BLOCK_OUTLINE_WIDTH,
			TweakifyModConfig.clampBlockOutlineWidth(TweakifyModConfig.DEFAULT_BLOCK_OUTLINE_WIDTH), 1.0e-6);
		assertEquals(TweakifyModConfig.MAX_BLOCK_OUTLINE_WIDTH,
			TweakifyModConfig.clampBlockOutlineWidth(TweakifyModConfig.MAX_BLOCK_OUTLINE_WIDTH), 1.0e-6);
		assertEquals(0.0, TweakifyModConfig.MIN_BLOCK_OUTLINE_WIDTH / TweakifyModConfig.BLOCK_OUTLINE_WIDTH_STEP
			- Math.rint(TweakifyModConfig.MIN_BLOCK_OUTLINE_WIDTH / TweakifyModConfig.BLOCK_OUTLINE_WIDTH_STEP), 1.0e-9);
		assertEquals(0.0, TweakifyModConfig.DEFAULT_BLOCK_OUTLINE_WIDTH / TweakifyModConfig.BLOCK_OUTLINE_WIDTH_STEP
			- Math.rint(TweakifyModConfig.DEFAULT_BLOCK_OUTLINE_WIDTH / TweakifyModConfig.BLOCK_OUTLINE_WIDTH_STEP), 1.0e-9);
		assertEquals(0.0, TweakifyModConfig.MAX_BLOCK_OUTLINE_WIDTH / TweakifyModConfig.BLOCK_OUTLINE_WIDTH_STEP
			- Math.rint(TweakifyModConfig.MAX_BLOCK_OUTLINE_WIDTH / TweakifyModConfig.BLOCK_OUTLINE_WIDTH_STEP), 1.0e-9);
	}

	@Test
	void loadHandlesNullBlockOutlineOptions() throws IOException {
		Path path = configPath();
		Files.writeString(path, """
			{
			  "blockOutlinePalette": null,
			  "blockOutlineColorMode": null,
			  "blockOutlineWidth": 2.5
			}
			""");

		TweakifyModConfig config = TweakifyModConfig.load(path);

		assertEquals(TweakifyModConfig.BLOCK_OUTLINE_PALETTE_WHITE, config.functions.blockOutlinePalette);
		assertEquals(TweakifyModConfig.BLOCK_OUTLINE_MODE_PALETTE, config.functions.blockOutlineColorMode);
		assertEquals(2.5, config.functions.blockOutlineWidth, 1.0e-6);
		try (Stream<Path> files = Files.list(tempDir)) {
			assertFalse(files.anyMatch(file -> file.getFileName().toString().contains(".broken-")),
				"null enum values must not be treated as a broken config");
		}
	}

	@Test
	void registryLookupWorksForRealItems() {
		assertTrue(TweakifyModConfig.isRegisteredItem("minecraft:torch"));
		assertFalse(TweakifyModConfig.isRegisteredItem("minecraft:definitely_not_an_item"));
		assertFalse(TweakifyModConfig.isRegisteredItem(null));
		assertFalse(TweakifyModConfig.isRegisteredItem("bad id!"));
	}

	@Test
	void defaultLightSourcesContainTorch() {
		List<String> defaults = TweakifyModConfig.defaultLightSourceItemIds();
		assertTrue(defaults.contains("minecraft:torch"));
	}

	@Test
	void isOffhandRestrictedChecksCache() {
		TweakifyModConfig config = new TweakifyModConfig(configPath());
		config.addItem("minecraft:torch");

		assertTrue(config.isOffhandRestricted(Items.TORCH));
		assertFalse(config.isOffhandRestricted(Items.STICK));
		assertFalse(config.isOffhandRestricted((Item) null));
		assertFalse(config.isOffhandRestricted(Items.AIR));
	}

	@Test
	void loadParsesNewCooldownFields() throws IOException {
		Path path = configPath();
		Files.writeString(path, """
			{
			  "miningCooldown": true,
			  "useCooldown": true,
			  "mainHandUseCooldown": false,
			  "offhandUseCooldown": false,
			  "hotkeyMiningCooldown": "X",
			  "hotkeyUseCooldown": "Y",
			  "mainHandUseCooldownItems": ["minecraft:apple"],
			  "offhandUseCooldownItems": ["minecraft:stone"]
			}
			""");

		TweakifyModConfig config = TweakifyModConfig.load(path);

		assertTrue(config.functions.miningCooldown);
		assertTrue(config.functions.useCooldown);
		assertFalse(config.functions.mainHandUseCooldown);
		assertFalse(config.functions.offhandUseCooldown);
		assertEquals("X", config.functions.hotkeyMiningCooldown);
		assertEquals("Y", config.functions.hotkeyUseCooldown);
		assertEquals(List.of("minecraft:apple"), config.getMainHandUseCooldownItems());
		assertEquals(List.of("minecraft:stone"), config.getOffhandUseCooldownItems());
	}

	@Test
	void sanitizesNewItemLists() throws IOException {
		Path path = configPath();
		Files.writeString(path, """
			{
			  "mainHandUseCooldownItems": ["apple", "minecraft:apple", "bad id!"],
			  "offhandUseCooldownItems": [null],
			  "attackRangeEntities": ["player", "minecraft:player", "bad id!"]
			}
			""");

		TweakifyModConfig config = TweakifyModConfig.load(path);

		assertEquals(List.of("minecraft:apple"), config.getMainHandUseCooldownItems());
		assertTrue(config.getOffhandUseCooldownItems().isEmpty());
		assertEquals(List.of("minecraft:player"), config.getAttackRangeEntities());
	}

	@Test
	void replaceNewItemListsSanitizeInput() {
		TweakifyModConfig config = new TweakifyModConfig(configPath());

		config.replaceMainHandUseCooldownItems(List.of("apple", "minecraft:apple", "bad id!"));
		assertEquals(List.of("minecraft:apple"), config.getMainHandUseCooldownItems());

		config.replaceOffhandUseCooldownItems(null);
		assertTrue(config.getOffhandUseCooldownItems().isEmpty());
	}

	@Test
	void defaultInteractableItemsContainCommonItems() {
		List<String> defaults = TweakifyModConfig.defaultInteractableItemIds();
		assertTrue(defaults.contains("minecraft:stone"), "blocks are placeable");
		assertTrue(defaults.contains("minecraft:snowball"), "snowball is a projectile");
		assertTrue(defaults.contains("minecraft:fishing_rod"), "fishing rod is usable");
	}

	@Test
	void isMainAndOffhandUseRestrictedCheckCache() {
		TweakifyModConfig config = new TweakifyModConfig(configPath());
		config.replaceMainHandUseCooldownItems(List.of("minecraft:apple"));
		config.replaceOffhandUseCooldownItems(List.of("minecraft:stone"));

		assertTrue(config.isMainHandUseRestricted(Items.APPLE));
		assertFalse(config.isMainHandUseRestricted(Items.STICK));
		assertTrue(config.isOffhandUseRestricted(Items.STONE));
		assertFalse(config.isOffhandUseRestricted(Items.STICK));
	}

	// --- 实体高亮 ---

	@Test
	void highlightRequiresMasterSwitchPresetAndMatchingId() {
		TweakifyModConfig config = new TweakifyModConfig(configPath());
		config.replaceHighlightPreset1Entities(List.of("minecraft:item"));

		assertFalse(config.shouldHighlightEntity(EntityTypes.ITEM), "master switch is off by default");
		config.functions.entityHighlight = true;
		assertFalse(config.shouldHighlightEntity(EntityTypes.ITEM), "no preset is enabled");

		config.functions.highlightPreset1 = true;
		assertTrue(config.shouldHighlightEntity(EntityTypes.ITEM), "dropped item entity should match");
		assertFalse(config.shouldHighlightEntity(EntityTypes.ZOMBIE), "unlisted entity should not match");

		config.functions.highlightPreset1 = false;
		assertFalse(config.shouldHighlightEntity(EntityTypes.ITEM), "disabling the preset stops highlighting");
	}

	@Test
	void highlightUsesFirstEnabledPresetWhenConfigIsMalformed() {
		TweakifyModConfig config = new TweakifyModConfig(configPath());
		config.functions.entityHighlight = true;
		config.functions.highlightPreset1 = true;
		config.functions.highlightPreset2 = true;
		config.replaceHighlightPreset1Entities(List.of("minecraft:item"));
		config.replaceHighlightPreset2Entities(List.of("minecraft:zombie"));

		assertTrue(config.shouldHighlightEntity(EntityTypes.ITEM));
		assertFalse(config.shouldHighlightEntity(EntityTypes.ZOMBIE),
			"only the first enabled preset should be used");
	}

	@Test
	void replacePresetEntitiesSanitizesInput() {
		TweakifyModConfig config = new TweakifyModConfig(configPath());
		config.replaceHighlightPreset1Entities(List.of("cow", "minecraft:cow", "not a valid id!"));

		assertEquals(List.of("minecraft:cow"), config.getHighlightPreset1Entities());
	}

	@Test
	void portalTeleportBlacklistMatchesEntityIds() {
		TweakifyModConfig config = new TweakifyModConfig(configPath());
		config.replaceNetherPortalTeleportBlacklist(List.of("zombie", "minecraft:zombie", "bad id!"));
		config.replaceEndPortalTeleportBlacklist(List.of("minecraft:item"));
		config.replaceEndGatewayTeleportBlacklist(List.of("minecraft:player"));

		assertEquals(List.of("minecraft:zombie"), config.getNetherPortalTeleportBlacklist(),
			"ids should be normalized, de-duplicated and invalid entries dropped");
		assertTrue(config.isNetherPortalTeleportBlacklisted(EntityTypes.ZOMBIE));
		assertFalse(config.isNetherPortalTeleportBlacklisted(EntityTypes.ITEM));
		assertTrue(config.isEndPortalTeleportBlacklisted(EntityTypes.ITEM));
		assertFalse(config.isEndPortalTeleportBlacklisted(EntityTypes.ZOMBIE));
		assertTrue(config.isEndGatewayTeleportBlacklisted(EntityTypes.PLAYER));
		assertFalse(config.isEndGatewayTeleportBlacklisted(EntityTypes.ITEM));
	}
}
