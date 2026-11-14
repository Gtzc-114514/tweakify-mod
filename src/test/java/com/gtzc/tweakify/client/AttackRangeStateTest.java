package com.gtzc.tweakify.client;

import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * 攻击范围的刷新频率与发布去重：内容未变时保留旧列表引用，
 * 让 {@link AttackRangeGeometryCache} 跳过整表重建；内容变化才发布新实例。
 */
class AttackRangeStateTest {

	@BeforeEach
	void reset() {
		AttackRangeState.resetForTest();
	}

	// 需求数值用字面量固定：常量被改动时必须在这里同步暴露，避免测试跟着常量漂移。
	@Test
	void refreshRateMatchesRequirement() {
		assertEquals(12, AttackRangeState.REFRESHES_PER_SECOND, "攻击范围每秒刷新 12 次");
	}

	@Test
	void identicalContentKeepsSameListInstance() {
		AttackRangeState.publishEntries(new ArrayList<>(List.of(entry(0.0, 1.0f, 0.0))), 64);
		List<AttackRangeState.Entry> first = AttackRangeState.entries();

		AttackRangeState.publishEntries(new ArrayList<>(List.of(entry(0.0, 1.0f, 0.0))), 64);

		assertSame(first, AttackRangeState.entries(), "内容相同应复用旧列表，顶点缓存不重建");
	}

	@Test
	void distanceOnlyChangeDoesNotRebuild() {
		AttackRangeState.publishEntries(new ArrayList<>(List.of(entry(0.0, 1.0f, 5.0))), 64);
		List<AttackRangeState.Entry> first = AttackRangeState.entries();

		// 几何不变、仅 distanceSq 变化（玩家移动）：摘要只含几何字段，应复用旧列表。
		AttackRangeState.publishEntries(new ArrayList<>(List.of(entry(0.0, 1.0f, 9.0))), 64);

		assertSame(first, AttackRangeState.entries());
	}

	@Test
	void changedRadiusPublishesNewList() {
		AttackRangeState.publishEntries(new ArrayList<>(List.of(entry(0.0, 1.0f, 0.0))), 64);
		List<AttackRangeState.Entry> first = AttackRangeState.entries();

		AttackRangeState.publishEntries(new ArrayList<>(List.of(entry(0.0, 2.0f, 0.0))), 64);

		assertNotSame(first, AttackRangeState.entries());
		assertEquals(2.0f, AttackRangeState.entries().get(0).radius(), 1.0e-6f);
	}

	@Test
	void changedPositionPublishesNewList() {
		AttackRangeState.publishEntries(new ArrayList<>(List.of(entry(0.0, 1.0f, 0.0))), 64);
		List<AttackRangeState.Entry> first = AttackRangeState.entries();

		AttackRangeState.publishEntries(new ArrayList<>(List.of(entry(4.0, 1.0f, 0.0))), 64);

		assertNotSame(first, AttackRangeState.entries());
		assertEquals(4.0, AttackRangeState.entries().get(0).x(), 1.0e-6);
	}

	@Test
	void resetAllowsRepublishingSameContent() {
		AttackRangeState.publishEntries(new ArrayList<>(List.of(entry(0.0, 1.0f, 0.0))), 64);
		AttackRangeState.resetForTest();

		AttackRangeState.publishEntries(new ArrayList<>(List.of(entry(0.0, 1.0f, 0.0))), 64);

		assertEquals(1, AttackRangeState.entries().size(), "重置后应再次发布");
	}

	@Test
	void inputOrderDoesNotMatterAfterSorting() {
		AttackRangeState.publishEntries(new ArrayList<>(List.of(
			entry(0.0, 1.0f, 20.0), entry(4.0, 1.0f, 10.0))), 64);
		List<AttackRangeState.Entry> first = AttackRangeState.entries();

		// 输入顺序交换，排序后顺序一致，摘要与精确比对都判定相同。
		AttackRangeState.publishEntries(new ArrayList<>(List.of(
			entry(4.0, 1.0f, 10.0), entry(0.0, 1.0f, 20.0))), 64);

		assertSame(first, AttackRangeState.entries());
	}

	@Test
	void tiedDistancesUseDeterministicOrder() {
		// distanceSq 相同：并列规则按 x 排序，输入顺序不影响结果。
		AttackRangeState.publishEntries(new ArrayList<>(List.of(
			entry(4.0, 1.0f, 10.0), entry(1.0, 1.0f, 10.0))), 64);
		List<AttackRangeState.Entry> first = AttackRangeState.entries();
		assertEquals(1.0, first.get(0).x(), 1.0e-6);

		AttackRangeState.publishEntries(new ArrayList<>(List.of(
			entry(1.0, 1.0f, 10.0), entry(4.0, 1.0f, 10.0))), 64);

		assertSame(first, AttackRangeState.entries());
	}

	@Test
	void maxTargetsTruncationKeepsNearestAndDeduplicates() {
		AttackRangeState.publishEntries(new ArrayList<>(List.of(
			entry(0.0, 1.0f, 30.0), entry(4.0, 1.0f, 10.0), entry(8.0, 1.0f, 20.0))), 2);
		List<AttackRangeState.Entry> first = AttackRangeState.entries();
		assertEquals(2, first.size());
		assertEquals(4.0, first.get(0).x(), 1.0e-6);
		assertEquals(8.0, first.get(1).x(), 1.0e-6);

		// 被截断的远处条目变化不影响前两个：复用旧列表。
		AttackRangeState.publishEntries(new ArrayList<>(List.of(
			entry(0.0, 2.0f, 30.0), entry(4.0, 1.0f, 10.0), entry(8.0, 1.0f, 20.0))), 2);

		assertSame(first, AttackRangeState.entries());
	}

	@Test
	void maxTargetsTruncationChangePublishesNewList() {
		AttackRangeState.publishEntries(new ArrayList<>(List.of(
			entry(0.0, 1.0f, 30.0), entry(4.0, 1.0f, 10.0), entry(8.0, 1.0f, 20.0))), 2);
		List<AttackRangeState.Entry> first = AttackRangeState.entries();

		// 第三个条目变近：截断后的前两个集合变化，必须重发。
		AttackRangeState.publishEntries(new ArrayList<>(List.of(
			entry(0.0, 1.0f, 5.0), entry(4.0, 1.0f, 10.0), entry(8.0, 1.0f, 20.0))), 2);

		assertNotSame(first, AttackRangeState.entries());
		assertEquals(0.0, AttackRangeState.entries().get(0).x(), 1.0e-6);
	}

	private static AttackRangeState.Entry entry(double x, float radius, double distanceSq) {
		double y = 64.0;
		return new AttackRangeState.Entry(x, y, y + 0.9, 0.0, radius,
			new AABB(x - radius, y - radius, -radius, x + radius, y + radius, radius), distanceSq);
	}
}
