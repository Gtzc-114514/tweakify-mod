package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * 调试叠加层的纯文本格式：速度行把 tick 速度换算成格每秒，
 * 水平与垂直分量分别显示，便于确认飞行/攀爬等功能是否生效。
 */
class DebugInfoOverlayTest {
	@Test
	void formatSpeedConvertsTickVelocityToBlocksPerSecond() {
		assertEquals("Speed: 4.32 / 0.00 (h/v)", DebugInfoOverlay.formatSpeed(0.216, 0.0, 0.0));
		assertEquals("Speed: 0.00 / 1.20 (h/v)", DebugInfoOverlay.formatSpeed(0.0, 0.06, 0.0));
	}

	@Test
	void formatSpeedUsesCombinedHorizontalComponent() {
		assertEquals("Speed: 5.00 / -2.00 (h/v)",
			DebugInfoOverlay.formatSpeed(0.15, -0.1, 0.2));
	}

	// 同一签名在间隔内复用同一份文本，渲染帧不再重复构建。
	@Test
	void lineCacheReusesSameSignatureWithinInterval() {
		DebugInfoOverlay.LineCache cache = new DebugInfoOverlay.LineCache();
		AtomicInteger builds = new AtomicInteger();
		Supplier<List<String>> builder = () -> {
			builds.incrementAndGet();
			return List.of("XYZ: 1.0 / 2.0 / 3.0");
		};

		List<String> first = cache.get(1_000_000_000L, "sig", 500_000_000L, builder);
		List<String> second = cache.get(1_400_000_000L, "sig", 500_000_000L, builder);

		assertSame(first, second, "间隔内同签名复用同一列表实例");
		assertEquals(1, builds.get());
	}

	// 签名变化（坐标/朝向/目标/子开关）必须立即重建，不等间隔到期。
	@Test
	void lineCacheRebuildsOnSignatureChangeAndIntervalExpiry() {
		DebugInfoOverlay.LineCache cache = new DebugInfoOverlay.LineCache();
		AtomicInteger builds = new AtomicInteger();
		Supplier<List<String>> builder = () -> {
			builds.incrementAndGet();
			return List.of("line-" + builds.get());
		};

		cache.get(1_000_000_000L, "sig-a", 500_000_000L, builder);
		cache.get(1_100_000_000L, "sig-b", 500_000_000L, builder);
		assertEquals(2, builds.get(), "签名变化立即重建");

		cache.get(1_600_000_000L, "sig-b", 500_000_000L, builder);
		assertEquals(3, builds.get(), "间隔到期（>=500ms）重建");
	}

	@Test
	void lineCacheResetForcesRebuild() {
		DebugInfoOverlay.LineCache cache = new DebugInfoOverlay.LineCache();
		AtomicInteger builds = new AtomicInteger();
		Supplier<List<String>> builder = () -> {
			builds.incrementAndGet();
			return List.of("line");
		};

		cache.get(1_000_000_000L, "sig", 500_000_000L, builder);
		cache.reset();
		cache.get(1_000_000_000L, "sig", 500_000_000L, builder);

		assertEquals(2, builds.get(), "reset 后即使签名与时间都未变也重建");
	}

	// 无缓存等价性：缓存返回内容与直接构建一致，只是复用了列表实例。
	@Test
	void cachedLinesEqualUncachedBuild() {
		DebugInfoOverlay.LineCache cache = new DebugInfoOverlay.LineCache();
		Supplier<List<String>> builder = () -> List.of(
			"XYZ: 1.0 / 2.0 / 3.0", "Facing: north (0.0)", "Dim: minecraft:overworld");

		List<String> cached = cache.get(1_000_000_000L, "sig", 500_000_000L, builder);

		assertEquals(builder.get(), cached);
	}
}
