package com.gtzc.tweakify.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncFileWriterTest {
	// CI 的共享 runner 可能在任务调度上停顿数百毫秒；等待上限放宽到 15 秒只影响失败场景的耗时，
	// 正常路径仍然立即返回，因此不会拖慢测试。
	// 屏障排空上限：drain / shutdown 这类「等队列清空」的等待。
	private static final long DRAIN_TIMEOUT_MILLIS = 15_000L;
	// 业务等待上限：submitAndWait 这类单次写任务的等待，保持与生产超时同量级，
	// 避免把「排队异常慢」的回归掩盖成通过。
	private static final long SUBMIT_TIMEOUT_MILLIS = 5_000L;
	private static final long START_TIMEOUT_SECONDS = 10L;

	// 预热：确保写盘线程已创建，避免首个任务的线程创建开销挤占后续用例的超时窗口。
	private static void warmUpWriter() {
		assertTrue(AsyncFileWriter.drain(DRAIN_TIMEOUT_MILLIS), "the writer thread should start");
	}

	private static void awaitQuietly(CountDownLatch latch, long timeoutSeconds) {
		try {
			latch.await(timeoutSeconds, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	// drain 必须等到此前提交的任务全部执行完，供测试确认后台队列已清空。
	@Test
	void drainWaitsForSubmittedTasks() throws InterruptedException {
		CountDownLatch started = new CountDownLatch(1);
		AtomicBoolean finished = new AtomicBoolean();
		AsyncFileWriter.execute(() -> {
			started.countDown();
			try {
				Thread.sleep(50L);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			finished.set(true);
		});

		assertTrue(started.await(START_TIMEOUT_SECONDS, TimeUnit.SECONDS), "task should start on the writer thread");
		assertTrue(AsyncFileWriter.drain(DRAIN_TIMEOUT_MILLIS), "drain should complete within the timeout");
		assertTrue(finished.get(), "drain must not return before the task finishes");
	}

	// 单线程执行器必须保证提交顺序与执行顺序一致，否则后写的旧内容会覆盖新内容。
	@Test
	void tasksRunInSubmissionOrder() {
		int count = 20;
		AtomicInteger next = new AtomicInteger();
		AtomicBoolean ordered = new AtomicBoolean(true);
		for (int i = 0; i < count; i++) {
			int expected = i;
			AsyncFileWriter.execute(() -> {
				if (next.getAndIncrement() != expected) {
					ordered.set(false);
				}
			});
		}

		assertTrue(AsyncFileWriter.drain(DRAIN_TIMEOUT_MILLIS), "drain should complete within the timeout");
		assertTrue(ordered.get(), "writer must execute tasks in submission order");
		assertEquals(count, next.get(), "every submitted task must run exactly once");
	}

	// 同步保存与后台异步写共用同一条队列：submitAndWait 必须排在此前提交的任务之后，
	// 否则后台旧写可能在同步保存之后落盘，把新内容反向覆盖。
	@Test
	void submitAndWaitRunsAfterPreviouslySubmittedTasks() {
		warmUpWriter();
		AtomicInteger order = new AtomicInteger();
		AtomicBoolean asyncRanFirst = new AtomicBoolean();
		AsyncFileWriter.execute(() -> asyncRanFirst.set(order.getAndIncrement() == 0));

		Boolean result = AsyncFileWriter.submitAndWait(() -> order.getAndIncrement() == 1, SUBMIT_TIMEOUT_MILLIS);

		assertEquals(Boolean.TRUE, result, "submitAndWait should return the task result");
		assertTrue(asyncRanFirst.get(), "the synchronous write must run after the queued async write");
		assertEquals(2, order.get(), "both writes must run exactly once");
	}

	// 任务已开始后超时：不能取消，必须再等一段拿到真实结果，
	// 否则调用方会回滚内存，而磁盘稍后却被写成新值。
	//
	// 任务的完成时机由 release 闸门控制：只要任务已经在跑，延长等待就必然能等到结果，
	// 不再依赖「任务能在多久内跑完」这种会被 CI 调度延迟打破的假设。
	@Test
	void submitAndWaitExtendsWaitForTaskThatAlreadyStarted() throws InterruptedException {
		warmUpWriter();
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		AtomicBoolean result = new AtomicBoolean();
		Thread caller = new Thread(() -> result.set(Boolean.TRUE.equals(AsyncFileWriter.submitAndWait(() -> {
			started.countDown();
			awaitQuietly(release, START_TIMEOUT_SECONDS);
			return true;
		}, 1000L))));
		caller.start();

		assertTrue(started.await(START_TIMEOUT_SECONDS, TimeUnit.SECONDS),
			"the write task should start before the timeout");
		try {
			// 任务被闸门挡住，必然活过第一次超时；等它进入延长等待后再放行。
			Thread.sleep(1200L);
		} finally {
			// 断言失败也要放行：写盘线程是两个测试类共用的静态单线程，
			// 卡在闸门上会把后续用例的等待窗口一起吃掉。
			release.countDown();
		}
		caller.join(TimeUnit.SECONDS.toMillis(START_TIMEOUT_SECONDS));

		assertFalse(caller.isAlive(), "submitAndWait should return after the extended wait");
		assertTrue(result.get(), "an in-flight write must be awaited instead of reported as a timeout");
	}

	// 任务尚未开始就超时：取消它并返回超时，调用方回滚后不会再被旧任务写盘。
	//
	// 阻塞任务用闸门占住写盘线程，确保排队任务在超时窗口内一定不会被执行；
	// 原先的固定 sleep 在 CI 调度延迟下可能提前结束，让被测任务直接跑完。
	@Test
	void submitAndWaitCancelsTaskThatHasNotStarted() throws InterruptedException {
		CountDownLatch blockerStarted = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		AsyncFileWriter.execute(() -> {
			blockerStarted.countDown();
			awaitQuietly(release, START_TIMEOUT_SECONDS);
		});
		assertTrue(blockerStarted.await(START_TIMEOUT_SECONDS, TimeUnit.SECONDS), "the blocking task should start");

		AtomicBoolean ran = new AtomicBoolean();
		Boolean result = AsyncFileWriter.submitAndWait(() -> {
			ran.set(true);
			return true;
		}, 50L);

		try {
			assertNull(result, "a task that has not started should report a timeout");
		} finally {
			// 断言失败也要放行阻塞任务，避免占住共用的写盘线程。
			release.countDown();
		}
		assertTrue(AsyncFileWriter.drain(DRAIN_TIMEOUT_MILLIS));
		assertFalse(ran.get(), "the cancelled task must not run");
	}

	// 退出时 shutdown 超时必须如实上报，并丢弃未开始的排队任务；
	// 调用方据此执行同步兜底写，而不是误以为已排空。
	@Test
	void shutdownDropsQueuedTasksWhenTimeoutExpires() throws InterruptedException {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		CountDownLatch started = new CountDownLatch(1);
		AtomicBoolean queuedRan = new AtomicBoolean();
		try {
			executor.execute(() -> {
				started.countDown();
				try {
					Thread.sleep(3000L);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			});
			assertTrue(started.await(START_TIMEOUT_SECONDS, TimeUnit.SECONDS), "the blocking task should start");
			executor.execute(() -> queuedRan.set(true));

			assertFalse(AsyncFileWriter.shutdown(executor, 100L),
				"shutdown must report that the queue was not drained");
			assertFalse(queuedRan.get(), "shutdownNow must drop queued tasks");
		} finally {
			executor.shutdownNow();
		}
	}
}
