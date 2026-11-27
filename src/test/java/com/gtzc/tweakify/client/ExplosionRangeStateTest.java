package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.TweakifyModTestSupport;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 爆炸范围条目的排序、截断与摘要去重；不依赖真实实体或世界。
 */
class ExplosionRangeStateTest {
	@AfterEach
	void resetState() {
		ExplosionRangeState.resetForTest();
	}

	@Test
	void publishSortsByDistanceAndCapsTargets() {
		List<AttackRangeState.Entry> found = new ArrayList<>(List.of(
			entry(1.0, 9.0),
			entry(2.0, 1.0),
			entry(3.0, 4.0)));
		ExplosionRangeState.publishEntries(found, 2);

		assertEquals(List.of(2.0, 3.0), ExplosionRangeState.entries().stream()
			.map(AttackRangeState.Entry::x).toList());
	}

	@Test
	void disablingFeatureClearsPublishedEntries() {
		ExplosionRangeState.publishEntries(new ArrayList<>(List.of(entry(1.0, 1.0))), 8);
		assertFalse(ExplosionRangeState.entries().isEmpty());

		TweakifyModConfig previous = TweakifyMod.config();
		TweakifyModConfig config = new TweakifyModConfig();
		config.functions.explosionRangeDisplay = false;
		TweakifyModTestSupport.installConfig(config);
		try {
			// 关闭功能后不触碰客户端世界，直接清理上一次发布的条目。
			ExplosionRangeState.tick(null);
			assertTrue(ExplosionRangeState.entries().isEmpty(),
				"disabling the feature must clear published entries");
		} finally {
			TweakifyModTestSupport.installConfig(previous);
		}
	}

	@Test
	void publishReusesListWhenGeometryIsUnchanged() {
		ExplosionRangeState.publishEntries(new ArrayList<>(List.of(entry(1.0, 1.0))), 8);
		List<AttackRangeState.Entry> first = ExplosionRangeState.entries();

		ExplosionRangeState.publishEntries(new ArrayList<>(List.of(entry(1.0, 1.0))), 8);
		assertSame(first, ExplosionRangeState.entries(),
			"identical geometry must keep the published list instance");

		ExplosionRangeState.publishEntries(new ArrayList<>(List.of(entry(2.0, 1.0))), 8);
		assertNotSame(first, ExplosionRangeState.entries(),
			"changed geometry must publish a new list instance");
	}

	private static AttackRangeState.Entry entry(double x, double distanceSq) {
		float radius = 6.0f;
		return new AttackRangeState.Entry(x, 0.05, 3.0, 0.0, radius,
			new AABB(x - radius, 3.0 - radius, -radius, x + radius, 3.0 + radius, radius),
			distanceSq);
	}
}
