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

		assertTrue(started.await(5, TimeUnit.SECONDS), "task should start on the writer thread");
		assertTrue(AsyncFileWriter.drain(5000L), "drain should complete within the timeout");
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

		assertTrue(AsyncFileWriter.drain(5000L), "drain should complete within the timeout");
		assertTrue(ordered.get(), "writer must execute tasks in submission order");
		assertEquals(count, next.get(), "every submitted task must run exactly once");
	}

	// 同步保存与后台异步写共用同一条队列：submitAndWait 必须排在此前提交的任务之后，
	// 否则后台旧写可能在同步保存之后落盘，把新内容反向覆盖。
	@Test
	void submitAndWaitRunsAfterPreviouslySubmittedTasks() {
		AtomicInteger order = new AtomicInteger();
		AtomicBoolean asyncRanFirst = new AtomicBoolean();
		AsyncFileWriter.execute(() -> asyncRanFirst.set(order.getAndIncrement() == 0));

		Boolean result = AsyncFileWriter.submitAndWait(() -> order.getAndIncrement() == 1, 5000L);

		assertEquals(Boolean.TRUE, result, "submitAndWait should return the task result");
		assertTrue(asyncRanFirst.get(), "the synchronous write must run after the queued async write");
		assertEquals(2, order.get(), "both writes must run exactly once");
	}

	// 任务已开始后超时：不能取消，必须再等一段拿到真实结果，
	// 否则调用方会回滚内存，而磁盘稍后却被写成新值。
	@Test
	void submitAndWaitExtendsWaitForTaskThatAlreadyStarted() throws InterruptedException {
		AtomicBoolean started = new AtomicBoolean();
		AtomicBoolean result = new AtomicBoolean();
		Thread caller = new Thread(() -> result.set(Boolean.TRUE.equals(AsyncFileWriter.submitAndWait(() -> {
			started.set(true);
			try {
				Thread.sleep(300L);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return false;
			}
			return true;
		}, 200L))));
		caller.start();

		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (!started.get() && System.nanoTime() < deadline) {
			Thread.sleep(1L);
		}
		assertTrue(started.get(), "the write task should start before the timeout");
		caller.join(5000L);

		assertFalse(caller.isAlive(), "submitAndWait should return after the extended wait");
		assertTrue(result.get(), "an in-flight write must be awaited instead of reported as a timeout");
	}

	// 任务尚未开始就超时：取消它并返回超时，调用方回滚后不会再被旧任务写盘。
	@Test
	void submitAndWaitCancelsTaskThatHasNotStarted() throws InterruptedException {
		CountDownLatch blockerStarted = new CountDownLatch(1);
		AsyncFileWriter.execute(() -> {
			blockerStarted.countDown();
			try {
				Thread.sleep(150L);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
		assertTrue(blockerStarted.await(5, TimeUnit.SECONDS), "the blocking task should start");

		AtomicBoolean ran = new AtomicBoolean();
		Boolean result = AsyncFileWriter.submitAndWait(() -> {
			ran.set(true);
			return true;
		}, 20L);

		assertNull(result, "a task that has not started should report a timeout");
		assertTrue(AsyncFileWriter.drain(5000L));
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
			assertTrue(started.await(5, TimeUnit.SECONDS), "the blocking task should start");
			executor.execute(() -> queuedRan.set(true));

			assertFalse(AsyncFileWriter.shutdown(executor, 100L),
				"shutdown must report that the queue was not drained");
			assertFalse(queuedRan.get(), "shutdownNow must drop queued tasks");
		} finally {
			executor.shutdownNow();
		}
	}
}
