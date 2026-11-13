package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.AsyncFileWriter;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.config.IConfigDouble;
import fi.dy.masa.malilib.config.IConfigInteger;
import fi.dy.masa.malilib.config.IConfigOptionList;
import fi.dy.masa.malilib.config.IConfigStringList;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 校验 MaLiLib 配置项与 TweakifyModConfig 字段的对应关系：
 * 名称必须能解析到字段，默认值必须与字段初始化值一致。
 * 两处默认值一旦漂移，“已修改”统计与重置行为就会出错。
 */
class TweakifyModConfigOptionsTest {
	// CI 的共享 runner 可能在后台写盘调度上停顿数百毫秒；等待上限放宽只影响失败场景的耗时，
	// 正常路径仍然立即返回，不会拖慢测试。
	private static final long DRAIN_TIMEOUT_MILLIS = 15_000L;
	private static final long START_TIMEOUT_SECONDS = 10L;

	@TempDir
	Path tempDir;

	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@BeforeEach
	void resetSharedState() {
		// 写盘节流/序号/结果队列是进程级静态；逐个用例清理，避免顺序依赖。
		TweakifyModConfigOptions.resetForTest();
	}

	@Test
	void toggleDefaultsMatchConfigDefaults() {
		TweakifyModConfig defaults = new TweakifyModConfig();
		for (IConfigBoolean toggle : TweakifyModConfigOptions.TOGGLE_OPTIONS) {
			assertEquals(defaults.getBoolean(toggle.getName()), toggle.getDefaultBooleanValue(),
				"default value mismatch for " + toggle.getName());
		}
	}

	@Test
	void optionListAndIntegerDefaultsMatchConfigDefaults() {
		TweakifyModConfig defaults = new TweakifyModConfig();
		for (IConfigBase option : TweakifyModConfigOptions.ALL_OPTIONS) {
			if (option instanceof IConfigOptionList optionList) {
				assertEquals(defaults.getString(option.getName()),
					optionList.getDefaultOptionListValue().getStringValue(),
					"default option list value mismatch for " + option.getName());
			} else if (option instanceof IConfigInteger integer) {
				assertEquals(defaults.getInteger(option.getName()), integer.getDefaultIntegerValue(),
					"default integer value mismatch for " + option.getName());
			} else if (option instanceof IConfigDouble doubleOption) {
				assertEquals(defaults.getDouble(option.getName()), doubleOption.getDefaultDoubleValue(),
					"default double value mismatch for " + option.getName());
			}
		}
	}

	@Test
	void everyOptionMapsToConfigField() {
		for (IConfigBase option : TweakifyModConfigOptions.ALL_OPTIONS) {
			// 纯快捷键项（如 openConfigGui）没有同名的值字段。
			if (option instanceof IConfigBoolean || option instanceof IConfigStringList
				|| option instanceof IConfigOptionList || option instanceof IConfigInteger
				|| option instanceof IConfigDouble) {
				assertTrue(TweakifyModConfig.hasField(option.getName()),
					"missing config field for " + option.getName());
			}
			if (option instanceof IHotkey) {
				assertTrue(TweakifyModConfig.hasField(hotkeyFieldName(option.getName())),
					"missing hotkey field for " + option.getName());
			}
		}
	}

	// 配置界面的统计与快捷键注册都依赖这两份清单，新增开关漏登记时直接失败。
	@Test
	void everyBooleanOptionIsInToggleList() {
		Set<IConfigBoolean> toggles = new HashSet<>(TweakifyModConfigOptions.TOGGLE_OPTIONS);
		for (IConfigBase option : TweakifyModConfigOptions.ALL_OPTIONS) {
			if (option instanceof IConfigBoolean toggle) {
				assertTrue(toggles.contains(toggle), "missing toggle option: " + option.getName());
			}
		}
	}

	@Test
	void blockOutlineWidthUsesSteppedSliderConfig() {
		assertTrue(TweakifyModConfigOptions.BLOCK_OUTLINE_WIDTH instanceof BlockOutlineWidthConfig);
		assertTrue(TweakifyModConfigOptions.BLOCK_OUTLINE_WIDTH.shouldUseSlider());
	}

	@Test
	void blockOutlineWidthOptionRangeMatchesConstants() {
		IConfigDouble width = TweakifyModConfigOptions.BLOCK_OUTLINE_WIDTH;
		assertEquals(TweakifyModConfig.BLOCK_OUTLINE_WIDTH_FIELD, width.getName());
		assertEquals(TweakifyModConfig.DEFAULT_BLOCK_OUTLINE_WIDTH, width.getDefaultDoubleValue(), 1.0e-9);
		assertEquals(TweakifyModConfig.MIN_BLOCK_OUTLINE_WIDTH, width.getMinDoubleValue(), 1.0e-9);
		assertEquals(TweakifyModConfig.MAX_BLOCK_OUTLINE_WIDTH, width.getMaxDoubleValue(), 1.0e-9);
	}

	@Test
	void everyHotkeyOptionIsRegistered() {
		Set<IHotkey> hotkeys = new HashSet<>(TweakifyModConfigOptions.HOTKEY_OPTIONS);
		for (IConfigBase option : TweakifyModConfigOptions.ALL_OPTIONS) {
			if (option instanceof IHotkey hotkey) {
				assertTrue(hotkeys.contains(hotkey), "missing hotkey registration: " + option.getName());
			}
		}
	}

	// 异步写盘失败回滚后，待写快照必须刷新为回滚后的内存状态；
	// 否则下一批写盘失败的回滚会把刚回滚的字段当成“本次修改”再次写回内存/磁盘（静默复活）。
	@Test
	void rollbackAfterAsyncFailureRefreshesPendingSnapshot() throws Exception {
		// 目标是带内容的目录：每次写盘都会失败，用于驱动连续的失败回滚。
		Path blocked = tempDir.resolve("blocked-config");
		Files.createDirectories(blocked);
		Files.writeString(blocked.resolve("occupied"), "x");

		TweakifyModConfig config = new TweakifyModConfig(blocked);
		TweakifyMod.CONFIG = config;
		try {
			config.functions.blockOffhandFallback = false;
			TweakifyModConfig.Snapshot before = config.snapshot();
			config.functions.blockOffhandFallback = true;
			TweakifyModConfig.Snapshot after = config.snapshot();

			// 模拟后续批次已经以“修改后”的值拍下了 before。
			TweakifyModConfigOptions.setPendingForTest(config.snapshot());
			TweakifyModConfigOptions.applyFailedWriteRollback(config, before, after);

			assertFalse(config.functions.blockOffhandFallback, "失败回滚应恢复字段");
			assertTrue(TweakifyModConfigOptions.pendingMatchesForTest(config),
				"pendingSnapshot must be refreshed to the rolled-back memory state");

			// 再改一个字段并让下一批写盘也失败：失败回滚不能把已回滚的字段复活。
			config.modified.disableHurtShake = true;
			TweakifyModConfigOptions.flushPendingSaveNow();
			assertTrue(AsyncFileWriter.drain(DRAIN_TIMEOUT_MILLIS));
			TweakifyModConfigOptions.pollSaveResults();

			assertFalse(config.functions.blockOffhandFallback,
				"a later failed batch must not revive the already rolled-back field");
			assertFalse(config.modified.disableHurtShake, "the later batch should roll back its own field");
		} finally {
			// 等待兜底异步写盘完成，避免后台任务在 @TempDir 清理时还持有临时目录。
			AsyncFileWriter.drain(DRAIN_TIMEOUT_MILLIS);
			TweakifyModConfigOptions.resetForTest();
			TweakifyMod.CONFIG = null;
		}
	}

	// Options 的异步 flush 也必须在提交时分配落盘序号：
	// 若改成执行期分配，延迟执行的旧快照会在同步兜底写之后反超并覆盖新内容。
	@Test
	void flushPendingSaveNowAssignsWriteSequenceAtSubmitTime() throws Exception {
		Path path = tempDir.resolve("flush-order.json");
		TweakifyModConfig config = new TweakifyModConfig(path);
		TweakifyMod.CONFIG = config;

		CountDownLatch blocked = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		AsyncFileWriter.execute(() -> {
			blocked.countDown();
			// 有界等待：即使断言在 try 前失败、release 未被调用，也不能永久占住共享写盘线程。
			try {
				release.await(START_TIMEOUT_SECONDS, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});

		try {
			assertTrue(blocked.await(START_TIMEOUT_SECONDS, TimeUnit.SECONDS), "the writer should be blocked");
			config.functions.blockOffhandFallback = false;
			TweakifyModConfigOptions.setPendingForTest(config.snapshot());
			// flush 序列化当前 false，并在提交时分配落盘序号。
			TweakifyModConfigOptions.flushPendingSaveNow();

			// 同步兜底写拿到更晚的序号，写的是 true。
			config.functions.blockOffhandFallback = true;
			assertEquals(Boolean.TRUE, config.writeSynchronously());

			release.countDown();
			assertTrue(AsyncFileWriter.drain(DRAIN_TIMEOUT_MILLIS));

			String json = Files.readString(path);
			assertTrue(json.contains("\"blockOffhandFallback\": true"),
				"the stale queued flush must not overwrite the later synchronous snapshot");
		} finally {
			release.countDown();
			AsyncFileWriter.drain(DRAIN_TIMEOUT_MILLIS);
			TweakifyModConfigOptions.resetForTest();
			TweakifyMod.CONFIG = null;
		}
	}

	// 无任何兜底写（执行器已关闭且同步写失败）时必须回退乐观推进的序号。
	@Test
	void failedSaveWithNoFallbackRevertsSyncedSequence() {
		TweakifyModConfigOptions.setSyncedSequenceForTest(10L);

		TweakifyModConfigOptions.handleSaveFailure(false, 10L, 5L);

		assertEquals(5L, TweakifyModConfigOptions.syncedSequenceForTest(),
			"无兜底写时必须回退乐观推进的序号");
	}

	// 兜底写成功后磁盘已是新值，不能回退序号，否则旧失败结果会错误回滚内存。
	@Test
	void fallbackWrittenKeepsOptimisticSequence() {
		TweakifyModConfigOptions.setSyncedSequenceForTest(10L);

		TweakifyModConfigOptions.handleSaveFailure(true, 10L, 5L);

		assertEquals(10L, TweakifyModConfigOptions.syncedSequenceForTest(),
			"兜底写成功时不能回退序号");
	}

	// 已有更新的成功推进时，旧失败的回退必须 CAS 失败，不能把序号拉回旧值。
	@Test
	void rollbackDoesNotOverrideNewerSyncedSequence() {
		TweakifyModConfigOptions.setSyncedSequenceForTest(12L);

		assertFalse(TweakifyModConfigOptions.rollbackSyncedSequence(10L, 5L),
			"旧失败的回退必须 CAS 失败");
		assertEquals(12L, TweakifyModConfigOptions.syncedSequenceForTest(),
			"旧失败不能覆盖更新的成功推进");
	}

	// 守卫正方向：被整份保存覆盖的失败结果不能回滚内存。
	@Test
	void skippedFailureDoesNotRollBackCoveredMemory() {
		TweakifyModConfig config = new TweakifyModConfig(tempDir.resolve("covered.json"));
		TweakifyMod.CONFIG = config;
		try {
			config.functions.blockOffhandFallback = false;
			TweakifyModConfig.Snapshot before = config.snapshot();
			config.functions.blockOffhandFallback = true;
			TweakifyModConfig.Snapshot after = config.snapshot();

			TweakifyModConfigOptions.setSyncedSequenceForTest(10L);
			TweakifyModConfigOptions.enqueueSaveResultForTest(false, before, after, 5L);

			TweakifyModConfigOptions.pollSaveResults();

			assertTrue(config.functions.blockOffhandFallback,
				"已被整份保存覆盖的失败结果不能回滚内存");
		} finally {
			TweakifyModConfigOptions.resetForTest();
			TweakifyMod.CONFIG = null;
		}
	}

	// 被跳过的失败结果先延迟；整份保存失败回退序号后必须重放并回滚内存。
	@Test
	void rolledBackSequenceLetsFailedResultRollBackMemory() {
		TweakifyModConfig config = new TweakifyModConfig(tempDir.resolve("sequence-rollback.json"));
		TweakifyMod.CONFIG = config;
		try {
			config.functions.blockOffhandFallback = false;
			TweakifyModConfig.Snapshot before = config.snapshot();
			config.functions.blockOffhandFallback = true;
			TweakifyModConfig.Snapshot after = config.snapshot();

			TweakifyModConfigOptions.setSyncedSequenceForTest(10L);
			TweakifyModConfigOptions.enqueueSaveResultForTest(false, before, after, 5L);
			TweakifyModConfigOptions.pollSaveResults();
			assertTrue(config.functions.blockOffhandFallback, "延迟期间内存应保持新值");

			TweakifyModConfigOptions.handleSaveFailure(false, 10L, 0L);
			TweakifyModConfigOptions.pollSaveResults();

			assertFalse(config.functions.blockOffhandFallback,
				"回退后重放的失败结果应能把内存回滚到磁盘状态");
		} finally {
			AsyncFileWriter.drain(DRAIN_TIMEOUT_MILLIS);
			TweakifyModConfigOptions.resetForTest();
			TweakifyMod.CONFIG = null;
		}
	}

	// 兜底写本身失败时必须回退序号（不能把“入队成功”当成写盘成功）。
	@Test
	void failedFallbackWriteRevertsSyncedSequence() throws Exception {
		Path blocked = tempDir.resolve("blocked-fallback");
		Files.createDirectories(blocked);
		Files.writeString(blocked.resolve("occupied"), "x");
		TweakifyModConfig config = new TweakifyModConfig(blocked);
		TweakifyMod.CONFIG = config;
		try {
			TweakifyModConfigOptions.setSyncedSequenceForTest(10L);
			TweakifyModConfigOptions.handleSaveFailure(config, 10L, 5L);
			assertTrue(AsyncFileWriter.drain(DRAIN_TIMEOUT_MILLIS));
			assertEquals(5L, TweakifyModConfigOptions.syncedSequenceForTest(),
				"兜底写自身失败时必须回退序号，让旧失败结果可以回滚");
		} finally {
			AsyncFileWriter.drain(DRAIN_TIMEOUT_MILLIS);
			TweakifyModConfigOptions.resetForTest();
			TweakifyMod.CONFIG = null;
		}
	}

	// 兜底写成功时磁盘已是新值，不能回退序号。
	@Test
	void fallbackWriteSuccessKeepsSequence() throws Exception {
		TweakifyModConfig config = new TweakifyModConfig(tempDir.resolve("fallback-ok.json"));
		TweakifyMod.CONFIG = config;
		try {
			TweakifyModConfigOptions.setSyncedSequenceForTest(10L);
			TweakifyModConfigOptions.handleSaveFailure(config, 10L, 5L);
			assertTrue(AsyncFileWriter.drain(DRAIN_TIMEOUT_MILLIS));
			assertEquals(10L, TweakifyModConfigOptions.syncedSequenceForTest(),
				"兜底写成功时不能回退序号");
		} finally {
			AsyncFileWriter.drain(DRAIN_TIMEOUT_MILLIS);
			TweakifyModConfigOptions.resetForTest();
			TweakifyMod.CONFIG = null;
		}
	}

	// 锁超时无法确认写盘结果：不能回退乐观推进的序号，否则可能反向覆盖已落盘的新值。
	@Test
	void lockTimeoutKeepsOptimisticSequence() {
		TweakifyModConfigOptions.setSyncedSequenceForTest(10L);

		TweakifyModConfigOptions.handleSaveFailure((Boolean) null, 10L, 5L);

		assertEquals(10L, TweakifyModConfigOptions.syncedSequenceForTest(),
			"锁超时不能回退序号");
	}

	// save() 完整链路：整份写已提交（delegate 可执行），但回调 execute 被拒绝。
	// 必须 await 真实的写盘失败结果，走同步兜底并回退序号。
	@Test
	void saveRollsBackSequenceWhenCallbackCannotBeQueued() throws Exception {
		ExecutorService original = AsyncFileWriter.currentExecutorForTest();
		ExecutorService submitOnly = new SubmitOnlyExecutor();
		AsyncFileWriter.installExecutorForTest(submitOnly);
		// 安装静态执行器后立即进 try：中间的 IO/构造一旦抛异常，finally 也能恢复执行器。
		try {
			Path blocked = tempDir.resolve("callback-closed");
			Files.createDirectories(blocked);
			Files.writeString(blocked.resolve("occupied"), "x");
			TweakifyModConfig config = new TweakifyModConfig(blocked);
			TweakifyMod.CONFIG = config;

			// 产生一个早于整份保存的写批次，让 save 的 covered/advanced 非零。
			TweakifyModConfigOptions.setPendingForTest(config.snapshot());
			TweakifyModConfigOptions.flushPendingSaveNow();
			TweakifyModConfigOptions.setSyncedSequenceForTest(0L);

			new TweakifyModConfigOptions().save();

			assertEquals(0L, TweakifyModConfigOptions.syncedSequenceForTest(),
				"回调无法入队且同步兜底失败时必须回退乐观推进的序号");
		} finally {
			AsyncFileWriter.installExecutorForTest(original);
			submitOnly.shutdownNow();
			AsyncFileWriter.drain(DRAIN_TIMEOUT_MILLIS);
			TweakifyModConfigOptions.resetForTest();
			TweakifyMod.CONFIG = null;
		}
	}

	// 只允许 submit、拒绝 execute：模拟“写盘已提交，但回调无法再入队”。
	private static final class SubmitOnlyExecutor extends AbstractExecutorService {
		private final ExecutorService delegate = Executors.newSingleThreadExecutor();

		@Override
		public void execute(Runnable command) {
			throw new RejectedExecutionException("execute disabled for test");
		}

		@Override
		public Future<?> submit(Runnable task) {
			return delegate.submit(task);
		}

		@Override
		public <T> Future<T> submit(Runnable task, T result) {
			return delegate.submit(task, result);
		}

		@Override
		public <T> Future<T> submit(Callable<T> task) {
			return delegate.submit(task);
		}

		@Override
		public void shutdown() {
			delegate.shutdown();
		}

		@Override
		public List<Runnable> shutdownNow() {
			return delegate.shutdownNow();
		}

		@Override
		public boolean isShutdown() {
			return delegate.isShutdown();
		}

		@Override
		public boolean isTerminated() {
			return delegate.isTerminated();
		}

		@Override
		public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
			return delegate.awaitTermination(timeout, unit);
		}
	}

	private static String hotkeyFieldName(String optionName) {
		return "hotkey" + Character.toUpperCase(optionName.charAt(0)) + optionName.substring(1);
	}
}
