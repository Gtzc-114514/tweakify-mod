package com.gtzc.tweakify.util;

import com.gtzc.tweakify.TweakifyMod;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 单线程后台写盘器：配置与滚动记忆共用同一条写盘队列。
 *
 * <p>写盘是纯 IO，放到客户端 tick 之外执行，避免拖动滑块时每 250 ms 掉一帧。
 * 单线程保证提交顺序与落盘顺序一致；daemon 线程不阻止 JVM 退出。
 *
 * <p>同步保存（{@link #submitAndWait}）也提交到同一条队列：即使之前排队的旧写还没完成，
 * 本次保存也会排在它们之后，不会出现后台旧内容在同步写之后落盘、把新内容反向覆盖的情况。
 * 退出路径先写入最终状态，再用 {@link #shutdown(long)} 停止接受新任务并等待队列排空。
 */
public final class AsyncFileWriter {
	// volatile：测试可临时替换执行器，验证“提交成功但回调无法入队”等关闭分支。
	private static volatile ExecutorService writer = newWriter();

	private static ExecutorService newWriter() {
		return Executors.newSingleThreadExecutor(runnable -> {
			Thread thread = new Thread(runnable, "tweakify-file-writer");
			thread.setDaemon(true);
			return thread;
		});
	}

	private AsyncFileWriter() {
	}

	// 测试钩子：替换全局写盘执行器；生产代码不得调用。
	public static void installExecutorForTest(ExecutorService executor) {
		writer = executor;
	}

	public static ExecutorService currentExecutorForTest() {
		return writer;
	}

	// 提交一个写盘任务；返回是否成功入队。执行器已关闭时静默丢弃（退出阶段不再接受新任务）。
	public static boolean execute(Runnable task) {
		try {
			writer.execute(task);
			return true;
		} catch (RejectedExecutionException e) {
			TweakifyMod.LOGGER.warn("File writer already shut down; dropping a pending write");
			return false;
		}
	}

	/**
	 * 一次已提交、可等待结果的写盘。{@link #submit} 在调用方线程入队，
	 * {@link #await} 在锁外等待，二者拆开才能避免“序列化/入队需持锁、等待又不必持锁”的冲突。
	 */
	public static final class WriteHandle {
		private final Future<Boolean> future;
		private final AtomicBoolean started;

		private WriteHandle(Future<Boolean> future, AtomicBoolean started) {
			this.future = future;
			this.started = started;
		}
	}

	/**
	 * 提交一个带结果的写盘任务，不等待。返回 {@code null} 表示执行器已关闭。
	 */
	public static WriteHandle submit(Callable<Boolean> task) {
		AtomicBoolean started = new AtomicBoolean();
		try {
			Future<Boolean> future = writer.submit(() -> {
				started.set(true);
				return task.call();
			});
			return new WriteHandle(future, started);
		} catch (RejectedExecutionException e) {
			TweakifyMod.LOGGER.warn("File writer already shut down; dropping a pending write");
			return null;
		}
	}

	/**
	 * 提交一个带结果的写盘任务并等待完成，超时时取消尚未开始的任务。
	 *
	 * @return 任务结果；{@code null} 表示执行器已关闭或等待超时
	 */
	public static Boolean submitAndWait(Callable<Boolean> task, long timeoutMillis) {
		return submitAndWait(task, timeoutMillis, true);
	}

	/**
	 * 提交一个带结果的写盘任务并等待完成。
	 *
	 * @param cancelOnTimeout 超时时是否取消尚未开始的任务。普通保存传 {@code true}；
	 *     退出路径传 {@code false}，让任务留在队列里由 {@link #shutdown(long)} 等待完成。
	 * @return 任务结果；{@code null} 表示执行器已关闭或等待超时
	 */
	public static Boolean submitAndWait(Callable<Boolean> task, long timeoutMillis, boolean cancelOnTimeout) {
		return await(submit(task), timeoutMillis, cancelOnTimeout);
	}

	/**
	 * 等待一个已经提交的写盘任务。等待本身不持有调用方的任何锁。
	 * 超时后：未开始的任务会被取消；已开始的任务会延长等待一次。
	 *
	 * @return 任务结果；{@code null} 表示执行器已关闭或等待超时
	 */
	public static Boolean await(WriteHandle handle, long timeoutMillis, boolean cancelOnTimeout) {
		if (handle == null) {
			return null;
		}
		Future<Boolean> future = handle.future;
		try {
			return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
		} catch (TimeoutException e) {
			if (!cancelOnTimeout) {
				TweakifyMod.LOGGER.warn("Timed out waiting for a write; it will finish in order");
				return null;
			}
			if (!handle.started.get()) {
				// 任务还没开始执行：直接取消，不再延长等待（避免把持锁/卡顿窗口翻倍）。
				future.cancel(false);
				TweakifyMod.LOGGER.warn("Timed out waiting for a write; the task was cancelled before it started");
				return null;
			}
			// 任务已经开始执行：延长等待一次拿真实结果。cancel(false) 不能用来判断是否已开始
			//（FutureTask 对运行中的任务也可能 CAS 成功并丢弃结果，但任务仍会跑完）。
			TweakifyMod.LOGGER.warn("Timed out waiting for a write; waiting once more");
			Boolean extended = awaitInFlightWrite(future, timeoutMillis);
			if (extended != null) {
				return extended;
			}
			// 延长等待后仍未完成：尽力取消（未完成时取消成功；任务仍可能继续写盘），
			// 调用方回滚后由 submitSaveAsync 的兜底快照收敛磁盘。
			boolean cancelled = future.cancel(false);
			TweakifyMod.LOGGER.warn("Write is still pending; {}",
				cancelled ? "it was cancelled before completion" : "it will finish in submission order");
			return null;
		} catch (CancellationException e) {
			return null;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		} catch (ExecutionException e) {
			TweakifyMod.LOGGER.warn("Unexpected error while waiting for a write", e.getCause());
			return Boolean.FALSE;
		}
	}

	// 超时后对任务做一次延长等待；仍超时返回 null，由调用方回滚并提交兜底写盘。
	private static Boolean awaitInFlightWrite(Future<Boolean> future, long timeoutMillis) {
		try {
			return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
		} catch (TimeoutException e) {
			return null;
		} catch (CancellationException e) {
			return null;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		} catch (ExecutionException e) {
			TweakifyMod.LOGGER.warn("Unexpected error while waiting for an in-flight write", e.getCause());
			return Boolean.FALSE;
		}
	}

	// 等待此前提交的任务全部执行完；返回是否在超时前排空。
	// 生产退出路径已改用 submitAndWait + shutdown；此方法主要供测试与诊断使用。
	public static boolean drain(long timeoutMillis) {
		try {
			writer.submit(() -> {
			}).get(timeoutMillis, TimeUnit.MILLISECONDS);
			return true;
		} catch (RejectedExecutionException e) {
			// 执行器已关闭：无法再提交 barrier，不能确认队列是否真正排空
			//（shutdownNow 可能丢弃了排队任务），返回 false 而不是误报成功。
			TweakifyMod.LOGGER.warn("File writer already shut down; cannot confirm the queue is empty");
			return false;
		} catch (TimeoutException e) {
			TweakifyMod.LOGGER.warn("Timed out waiting for pending config writes");
			return false;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		} catch (ExecutionException e) {
			// 空任务不会抛异常；真有异常也只能记日志，退出路径不能因此中断。
			TweakifyMod.LOGGER.warn("Unexpected error while draining config writes", e.getCause());
			return false;
		}
	}

	// 退出时调用：停止接受新任务并等待队列排空；返回是否在超时前完全终止。
	// 返回 false 表示已执行 shutdownNow：排队任务可能被丢弃，调用方需要做同步兜底。
	public static boolean shutdown(long timeoutMillis) {
		return shutdown(writer, timeoutMillis);
	}

	// 包私有：测试可注入独立执行器；同样返回是否在超时前完全终止。
	static boolean shutdown(ExecutorService executor, long timeoutMillis) {
		executor.shutdown();
		try {
			if (executor.awaitTermination(timeoutMillis, TimeUnit.MILLISECONDS)) {
				return true;
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		executor.shutdownNow();
		return false;
	}
}
