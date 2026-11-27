package com.gtzc.tweakify.client.render;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 几何重建的共享后台队列：单个 daemon 线程按提交顺序构建顶点缓存，
 * 渲染线程只通过各缓存发布的不可变快照读取几何，不在渲染帧内做三角/法线计算。
 *
 * <p>构建输入必须是不可变快照（状态层已用 {@code List.copyOf} 发布列表）。
 * 任务开始与发布两侧各做一次 generation 校验：过期任务直接丢弃，发布侧只接受
 * 更新的代，因此旧任务的结果永远不会覆盖新快照。执行器关闭时静默丢弃本次提交，
 * 缓存仍未发布，渲染线程下一帧会重新提交。
 */
final class GeometryBuildWorker {
	private static final String THREAD_NAME = "tweakify-geometry-worker";
	// volatile：测试可临时替换执行器，验证乱序完成与过期结果丢弃。
	private static volatile ExecutorService worker = newWorker();

	private static ExecutorService newWorker() {
		return Executors.newSingleThreadExecutor(runnable -> {
			Thread thread = new Thread(runnable, THREAD_NAME);
			thread.setDaemon(true);
			return thread;
		});
	}

	private GeometryBuildWorker() {
	}

	static void submit(Runnable task) {
		try {
			worker.execute(task);
		} catch (RejectedExecutionException e) {
			// 执行器已关闭（只在测试/退出阶段出现）：本次构建丢弃，缓存保持上一份快照。
		}
	}

	// 测试钩子：替换/恢复全局几何队列；生产代码不得调用。
	static void installExecutorForTest(ExecutorService executor) {
		worker = executor;
	}

	static ExecutorService currentExecutorForTest() {
		return worker;
	}

	// 等待此前提交的任务全部完成；返回是否在超时前排空。
	static boolean drainForTest(long timeoutMillis) {
		try {
			worker.submit(() -> {
			}).get(timeoutMillis, TimeUnit.MILLISECONDS);
			return true;
		} catch (RejectedExecutionException e) {
			return false;
		} catch (TimeoutException e) {
			return false;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		} catch (ExecutionException e) {
			return false;
		}
	}
}
