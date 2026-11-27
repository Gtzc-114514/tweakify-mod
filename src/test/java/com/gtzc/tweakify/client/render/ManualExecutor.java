package com.gtzc.tweakify.client.render;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 测试用执行器：只记录任务、不自动执行。测试用 runAt/runAll 显式控制完成顺序，
 * 用于验证几何后台构建的「旧任务不覆盖新快照」与乱序完成语义。
 */
final class ManualExecutor extends AbstractExecutorService {
	private final List<Runnable> tasks = new ArrayList<>();
	private volatile boolean shutdown;

	@Override
	public void execute(Runnable command) {
		if (this.shutdown) {
			throw new RejectedExecutionException("executor is shut down");
		}
		this.tasks.add(command);
	}

	@Override
	public Future<?> submit(Runnable task) {
		FutureTask<Object> future = new FutureTask<>(task, null);
		execute(future);
		return future;
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		FutureTask<T> future = new FutureTask<>(task);
		execute(future);
		return future;
	}

	// 按队列位置执行单个任务：乱序完成用它模拟「旧任务晚于新任务执行」。
	void runAt(int index) {
		this.tasks.remove(index).run();
	}

	// 按提交顺序执行全部待办任务。
	void runAll() {
		while (!this.tasks.isEmpty()) {
			this.tasks.remove(0).run();
		}
	}

	int pendingCount() {
		return this.tasks.size();
	}

	@Override
	public void shutdown() {
		this.shutdown = true;
	}

	@Override
	public List<Runnable> shutdownNow() {
		this.shutdown = true;
		List<Runnable> pending = new ArrayList<>(this.tasks);
		this.tasks.clear();
		return pending;
	}

	@Override
	public boolean isShutdown() {
		return this.shutdown;
	}

	@Override
	public boolean isTerminated() {
		return this.shutdown && this.tasks.isEmpty();
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) {
		return isTerminated();
	}
}
