package com.gtzc.tweakify.util;

import java.util.concurrent.ExecutorService;

/**
 * 写盘执行器的测试接缝：{@link AsyncFileWriter} 的替换方法保持包私有，
 * 其它包的测试经由本类访问。
 *
 * <p>本类只存在于 test 源集，生产代码无法引用；同包的 {@code AsyncFileWriterTest}
 * 仍可直接调用包私有方法。
 */
public final class AsyncFileWriterTestSupport {
	private AsyncFileWriterTestSupport() {
	}

	public static ExecutorService currentExecutor() {
		return AsyncFileWriter.currentExecutorForTest();
	}

	public static void installExecutor(ExecutorService executor) {
		AsyncFileWriter.installExecutorForTest(executor);
	}
}
