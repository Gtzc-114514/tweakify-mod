package com.gtzc.tweakify.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncFileWriterTest {
	// drain 必须等到此前提交的任务全部执行完，否则退出路径会漏掉最后一次写盘。
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
}
