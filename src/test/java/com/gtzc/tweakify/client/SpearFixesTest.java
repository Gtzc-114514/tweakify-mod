package com.gtzc.tweakify.client;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 长矛蓄力修复的纯计算：水平投影与击退/伤害判定的一致性。
 */
class SpearFixesTest {
	@Test
	void horizontalDirectionIgnoresPitch() {
		// 抬头/低头看向同一水平方向时，水平基准方向必须一致。
		Vec3 level = SpearFixes.horizontalDirection(new Vec3(0.0, 0.0, 1.0));
		Vec3 down = SpearFixes.horizontalDirection(new Vec3(0.0, -0.8, 0.6).normalize());
		assertEquals(level.x, down.x, 1.0e-6);
		assertEquals(level.z, down.z, 1.0e-6);
		assertEquals(1.0, down.length(), 1.0e-6);
	}

	@Test
	void horizontalDirectionHandlesStraightUp() {
		Vec3 fallback = SpearFixes.horizontalDirection(new Vec3(0.0, -1.0, 0.0));
		assertEquals(0.0, fallback.x, 1.0e-6);
		assertEquals(1.0, fallback.z, 1.0e-6);
	}

	@Test
	void pitchNoLongerReducesSpeedProjection() {
		// 水平速度 1.0 格/tick；低头 45 度时原版投影会缩到 0.5。
		Vec3 motion = new Vec3(0.0, 0.0, 20.0);
		Vec3 look = new Vec3(0.0, -0.70710678, 0.70710678);
		double vanilla = SpearFixes.speedAlong(look, motion);
		double flattened = SpearFixes.speedAlong(SpearFixes.horizontalDirection(look),
			SpearFixes.horizontalMotion(motion, true));
		assertEquals(14.142135, vanilla, 1.0e-3);
		assertEquals(20.0, flattened, 1.0e-3);
	}

	@Test
	void relativeSpeedIsClampedToZero() {
		Vec3 direction = new Vec3(0.0, 0.0, 1.0);
		double relative = SpearFixes.speedAlong(direction, new Vec3(0.0, 0.0, 4.0))
			- SpearFixes.speedAlong(direction, new Vec3(0.0, 0.0, 9.0));
		assertEquals(0.0, Math.max(0.0, relative), 1.0e-6);
	}

	@Test
	void knockbackConditionUsesRelativeSpeedWhenRequested() {
		assertEquals(5.1, SpearFixes.knockbackConditionSpeed(5.1, 4.8, false), 1.0e-6);
		assertEquals(4.8, SpearFixes.knockbackConditionSpeed(5.1, 4.8, true), 1.0e-6);
		// 伤害阈值 4.6 通过时，改用相对速度的击退阈值 5.1 不会反过来产生“只击退”。
		assertTrue(SpearFixes.knockbackConditionSpeed(5.2, 5.2, true) >= 4.6);
	}
}
