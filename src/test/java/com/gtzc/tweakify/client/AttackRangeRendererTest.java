package com.gtzc.tweakify.client;

import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 攻击范围渲染的提交前可见性判断：空列表不可见，无剔除时非空列表可见。
 */
class AttackRangeRendererTest {

	@Test
	void emptyEntriesAreNotVisible() {
		assertFalse(AttackRangeRenderer.hasVisible(List.of(), null));
	}

	@Test
	void entriesWithoutFrustumAreVisible() {
		assertTrue(AttackRangeRenderer.hasVisible(List.of(entry(1.0f)), null));
	}

	@Test
	void anyEntryMakesListVisible() {
		assertTrue(AttackRangeRenderer.hasVisible(List.of(entry(1.0f), entry(2.0f)), null));
	}

	private static AttackRangeState.Entry entry(float radius) {
		return new AttackRangeState.Entry(0.0, 64.0, 64.9, 0.0, radius,
			new AABB(-radius, 64.0 - radius, -radius, radius, 64.0 + radius, radius), 0.0);
	}
}
