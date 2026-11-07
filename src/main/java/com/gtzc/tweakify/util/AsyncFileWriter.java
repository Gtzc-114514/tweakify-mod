package com.gtzc.tweakify.util;

import com.gtzc.tweakify.TweakifyMod;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 单线程后台写盘器：配置与滚动记忆共用同一条写盘队列。
 *
 * <p>写盘是纯 IO，放到客户端 tick 之外执行，避免拖动滑块时每 250 ms 掉一帧。
 * 单线程保证提交顺序与落盘顺序一致；daemon 线程不阻止 JVM 退出。
 *
 * <p>退出路径必须先 {@link #drain(long)} 再同步写最终状态：
 * 否则后台正在写的旧内容可能在同步写之后落盘，把新内容覆盖掉。
 */
public final class AsyncFileWriter {
	private static final ExecutorService WRITER = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "tweakify-file-writer");
		thread.setDaemon(true);
		return thread;
	});

	private AsyncFileWriter() {
	}

	// 提交一个写盘任务；执行器已关闭时静默丢弃（退出阶段不再接受新任务）。
	public static void execute(Runnable task) {
		try {
			WRITER.execute(task);
		} catch (RejectedExecutionException e) {
			TweakifyMod.LOGGER.warn("File writer already shut down; dropping a pending write");
		}
	}

	// 等待此前提交的任务全部执行完；返回是否在超时前排空。
	public static boolean drain(long timeoutMillis) {
		try {
			WRITER.submit(() -> {
			}).get(timeoutMillis, TimeUnit.MILLISECONDS);
			return true;
		} catch (RejectedExecutionException e) {
			// 已经关闭：队列自然已排空。
			return true;
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

	// 退出时调用：停止接受新任务并等待队列排空。
	public static void shutdown(long timeoutMillis) {
		WRITER.shutdown();
		try {
			if (!WRITER.awaitTermination(timeoutMillis, TimeUnit.MILLISECONDS)) {
				WRITER.shutdownNow();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			WRITER.shutdownNow();
		}
	}
}
