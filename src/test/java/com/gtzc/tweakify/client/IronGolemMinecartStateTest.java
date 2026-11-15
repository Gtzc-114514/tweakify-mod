package com.gtzc.tweakify.client;

import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IronGolemMinecartStateTest {
	private static final double EPSILON = 1.0e-6;

	@Test
	void picksUpOnServerWhenEnabledAndCartIsEmpty() {
		assertTrue(IronGolemMinecartState.shouldPickUp(true, false, false, true));
	}

	@Test
	void rejectsWhenSwitchIsOff() {
		assertFalse(IronGolemMinecartState.shouldPickUp(false, false, false, true));
	}

	@Test
	void rejectsOnClientSide() {
		assertFalse(IronGolemMinecartState.shouldPickUp(true, true, false, true));
	}

	@Test
	void rejectsWhenCartAlreadyHasPassenger() {
		assertFalse(IronGolemMinecartState.shouldPickUp(true, false, true, true));
	}

	// 漏斗/运输/动力/TNT 等特种矿车不是普通矿车，铁傀儡不能坐进去。
	@Test
	void rejectsSpecialMinecarts() {
		assertFalse(IronGolemMinecartState.shouldPickUp(true, false, false, false));
	}

	// 距离限制：0 表示不限制；大于 0 时必须有玩家在范围内。
	@Test
	void distanceGateRequiresNearbyPlayer() {
		assertTrue(IronGolemMinecartState.isPlayerInRange(0, false), "0 = 不限制");
		assertTrue(IronGolemMinecartState.isPlayerInRange(16, true));
		assertFalse(IronGolemMinecartState.isPlayerInRange(16, false));
		assertTrue(IronGolemMinecartState.isPlayerInRange(-1, false), "负数按不限制处理");
		assertFalse(IronGolemMinecartState.shouldPickUp(true, false, false, true, false));
		assertTrue(IronGolemMinecartState.shouldPickUp(true, false, false, true, true));
	}

	@Test
	void searchBoxExpandsInAllThreeAxes() {
		AABB cart = new AABB(0.0, 0.0, 0.0, 1.0, 0.7, 1.0);
		AABB box = IronGolemMinecartState.searchBox(cart);

		assertEquals(-0.2, box.minX, EPSILON);
		assertEquals(1.2, box.maxX, EPSILON);
		assertEquals(-0.2, box.minY, EPSILON);
		assertEquals(0.9, box.maxY, EPSILON);
		assertEquals(-0.2, box.minZ, EPSILON);
		assertEquals(1.2, box.maxZ, EPSILON);
	}

	@Test
	void searchBoxCoversGolemStandingOnTop() {
		AABB cart = new AABB(0.0, 0.0, 0.0, 1.0, 0.7, 1.0);
		// 铁傀儡站在矿车顶部，碰撞箱从 y = 0.7 开始。
		AABB golemOnTop = new AABB(0.25, 0.7, 0.25, 1.15, 3.4, 1.15);

		assertTrue(IronGolemMinecartState.searchBox(cart).intersects(golemOnTop),
			"Y 方向膨胀后应覆盖站在矿车顶部的铁傀儡");
	}

	@Test
	void searchBoxRejectsGolemTooFarAbove() {
		AABB cart = new AABB(0.0, 0.0, 0.0, 1.0, 0.7, 1.0);
		// 底部高于膨胀后的顶部 0.9，不应被拾取。
		AABB golemAbove = new AABB(0.25, 0.91, 0.25, 1.15, 3.4, 1.15);

		assertFalse(IronGolemMinecartState.searchBox(cart).intersects(golemAbove));
	}
}
