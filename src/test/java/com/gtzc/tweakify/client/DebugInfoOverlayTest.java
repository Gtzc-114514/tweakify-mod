package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.HitResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

	// 签名工厂必须逐项使用入参：身份、坐标、朝向、方块、目标、命中、掩码与语言。
	@Test
	void signatureFactoryTakesEveryFieldFromArguments() {
		Object language = new Object();
		BlockPos hitPos = new BlockPos(4, 64, -6);
		DebugInfoOverlay.LineSignature signature = DebugInfoOverlay.signature(
			101, 202, 1.24, -2.26, 3.04, 89.96f, Direction.NORTH,
			new BlockPos(4, 65, -6), 303, true, HitResult.Type.BLOCK, hitPos,
			0b1010_1010, language);

		assertEquals(101, signature.playerIdentity());
		assertEquals(202, signature.levelIdentity());
		assertEquals(12L, signature.xTenth());
		assertEquals(-23L, signature.yTenth());
		assertEquals(30L, signature.zTenth());
		assertEquals(900L, signature.yRotTenth());
		assertEquals(Direction.NORTH.ordinal(), signature.direction());
		assertEquals(4, signature.blockX());
		assertEquals(65, signature.blockY());
		assertEquals(-6, signature.blockZ());
		assertEquals(303, signature.targetIdentity());
		assertTrue(signature.hasTarget());
		assertEquals(HitResult.Type.BLOCK, signature.hitType());
		assertEquals(hitPos, signature.hitPos());
		assertEquals(0b1010_1010, signature.configMask());
		assertSame(language, signature.language());
	}

	// 每个调试子开关都必须从配置读取并落到独立位。
	@Test
	void configMaskReflectsEverySubSwitch() {
		TweakifyModConfig config = new TweakifyModConfig();
		config.testing.debugInfoShowPerformance = false;
		config.testing.debugInfoShowTarget = false;
		config.testing.debugInfoShowBiome = false;
		config.testing.debugInfoShowSpeed = false;
		config.testing.debugInfoShowEntities = false;
		config.testing.debugInfoShowConfigStats = false;
		config.testing.debugInfoShowEffects = false;
		config.testing.debugInfoShowHooks = false;
		assertEquals(0, DebugInfoOverlay.configMask(config));

		config.testing.debugInfoShowPerformance = true;
		assertEquals(1, DebugInfoOverlay.configMask(config));
		config.testing.debugInfoShowPerformance = false;
		config.testing.debugInfoShowTarget = true;
		assertEquals(1 << 1, DebugInfoOverlay.configMask(config));
		config.testing.debugInfoShowTarget = false;
		config.testing.debugInfoShowBiome = true;
		assertEquals(1 << 2, DebugInfoOverlay.configMask(config));
		config.testing.debugInfoShowBiome = false;
		config.testing.debugInfoShowSpeed = true;
		assertEquals(1 << 3, DebugInfoOverlay.configMask(config));
		config.testing.debugInfoShowSpeed = false;
		config.testing.debugInfoShowEntities = true;
		assertEquals(1 << 4, DebugInfoOverlay.configMask(config));
		config.testing.debugInfoShowEntities = false;
		config.testing.debugInfoShowConfigStats = true;
		assertEquals(1 << 5, DebugInfoOverlay.configMask(config));
		config.testing.debugInfoShowConfigStats = false;
		config.testing.debugInfoShowEffects = true;
		assertEquals(1 << 6, DebugInfoOverlay.configMask(config));
		config.testing.debugInfoShowEffects = false;
		config.testing.debugInfoShowHooks = true;
		assertEquals(1 << 7, DebugInfoOverlay.configMask(config));
	}
}
