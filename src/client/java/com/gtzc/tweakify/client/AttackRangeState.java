package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.mixin.MobAttackRangeAccessor;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 攻击范围：按每秒 12 次的频率收集附近的生物与玩家并计算原版攻击距离，
 * ID 列表为空时包含全部生物与玩家，非空时只保留命中的实体。
 */
public final class AttackRangeState {
	// feetY 用于平面圆环，centerY 用于球体中心；bounds 用于渲染前的视锥剔除，
	// distanceSq 在收集时算好，排序与渲染都用同一份
	public record Entry(double x, double feetY, double centerY, double z, float radius, AABB bounds,
						double distanceSq) {
	}

	// 按距离从近到远；并列时用坐标/半径做确定性次序，
	// 避免等距实体因迭代顺序不同导致摘要变化与展示拖动。
	private static final Comparator<Entry> BY_DISTANCE = Comparator
		.comparingDouble(Entry::distanceSq)
		.thenComparingDouble(Entry::x)
		.thenComparingDouble(Entry::feetY)
		.thenComparingDouble(Entry::z)
		.thenComparingDouble(Entry::radius);

	private static List<Entry> entries = List.of();
	// 已发布内容的摘要：相同则跳过发布，让 AttackRangeGeometryCache 保持旧引用、不重建顶点。
	private static int entriesHash;
	private static boolean hasPublishedEntries;
	// 刷新频率：每秒 12 次（20/12 ≈ 1.67 tick），比屏障/基岩更快。
	static final int REFRESHES_PER_SECOND = 12;
	private static final RefreshScheduler REFRESH = new RefreshScheduler(REFRESHES_PER_SECOND);
	private static boolean wasEnabled;

	private AttackRangeState() {
	}

	public static void tick(Minecraft client) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		boolean enabled = config != null && config.functions.attackRangeDisplay
			&& client.player != null && client.level != null;
		if (!enabled) {
			entries = List.of();
			hasPublishedEntries = false;
			entriesHash = 0;
			wasEnabled = false;
			REFRESH.reset();
			return;
		}
		// 每秒 12 次（20/12 ≈ 1.67 tick）；开启后立即收集一次，未到窗口时沿用上一轮结果。
		boolean refreshDue = REFRESH.tick();
		if (!refreshDue && wasEnabled) {
			return;
		}
		wasEnabled = true;
		double maxDistance = Mth.clamp(config.functions.attackRangeMaxDistance,
			TweakifyModConfig.MIN_ATTACK_RANGE_DISTANCE, TweakifyModConfig.MAX_ATTACK_RANGE_DISTANCE);
		int maxTargets = Mth.clamp(config.functions.attackRangeMaxTargets,
			TweakifyModConfig.MIN_ATTACK_RANGE_TARGETS, TweakifyModConfig.MAX_ATTACK_RANGE_TARGETS);
		// 用范围查询代替遍历全部渲染实体，减少每 tick 的实体扫描量。
		AABB searchBox = client.player.getBoundingBox().inflate(maxDistance);
		double maxDistanceSq = maxDistance * maxDistance;
		List<Entry> found = new ArrayList<>();
		if (config.functions.attackRangeIncludeSelf) {
			// 自己与自己的距离为 0，与其它实体走同一条收集规则。
			addEntry(found, client.player, client.player, maxDistanceSq, config);
		}
		for (Entity entity : client.level.getEntities(client.player, searchBox,
			e -> e instanceof Mob || e instanceof Player)) {
			if (entity instanceof LivingEntity living && living != client.player) {
				addEntry(found, living, client.player, maxDistanceSq, config);
			}
		}
		publishEntries(found, maxTargets);
	}

	// 发布本次收集结果：先按距离排序并截断，内容摘要未变时保留旧列表引用，
	// 避免 AttackRangeGeometryCache 每秒 12 次仍然整表重建顶点。
	// 包私有：供 tick 与单测共用。
	static void publishEntries(List<Entry> found, int maxTargets) {
		found.sort(BY_DISTANCE);
		List<Entry> visible = found.subList(0, Math.min(found.size(), maxTargets));
		int hash = contentHash(visible);
		// 摘要只作预筛，再用几何字段精确比对：哈希碰撞不会让几何停在旧内容。
		if (hasPublishedEntries && hash == entriesHash && sameGeometry(entries, visible)) {
			return;
		}
		hasPublishedEntries = true;
		entriesHash = hash;
		// 内容变化时必须发布新列表实例：缓存的失效条件就是引用变化。
		entries = List.copyOf(visible);
	}

	// 精确兜底：摘要相同后再逐条比较几何字段，排除 distanceSq 与 bounds，
	// 使哈希碰撞不会把已变化的坐标/半径误判为相同。
	private static boolean sameGeometry(List<Entry> published, List<Entry> current) {
		if (published.size() != current.size()) {
			return false;
		}
		for (int i = 0; i < published.size(); i++) {
			Entry before = published.get(i);
			Entry after = current.get(i);
			if (before.x() != after.x() || before.feetY() != after.feetY()
				|| before.centerY() != after.centerY() || before.z() != after.z()
				|| before.radius() != after.radius()) {
				return false;
			}
		}
		return true;
	}

	// 轻量内容摘要：只含决定几何的坐标与半径；
	// distanceSq 随玩家移动变化，不参与摘要，否则玩家移动会让缓存白重建。
	private static int contentHash(List<Entry> entries) {
		int hash = entries.size();
		for (Entry entry : entries) {
			hash = 31 * hash + Double.hashCode(entry.x());
			hash = 31 * hash + Double.hashCode(entry.feetY());
			hash = 31 * hash + Double.hashCode(entry.centerY());
			hash = 31 * hash + Double.hashCode(entry.z());
			hash = 31 * hash + Float.hashCode(entry.radius());
		}
		return hash;
	}

	// 单个实体的过滤与条目计算：状态、距离与 ID 列表都在这里统一处理。
	private static void addEntry(List<Entry> found, LivingEntity living, Player viewer,
			double maxDistanceSq, TweakifyModConfig config) {
		if (!living.isAlive() || living.isRemoved() || living.isSpectator() || living.isInvisible()) {
			return;
		}
		double distanceSq = living.distanceToSqr(viewer);
		if (distanceSq > maxDistanceSq) {
			return;
		}
		if (!config.isAttackRangeEntityAllowed(living.getType())) {
			return;
		}
		double x = living.getX();
		double feetY = living.getY() + 0.05;
		double centerY = living.getY() + living.getBbHeight() / 2.0;
		double z = living.getZ();
		float radius = attackRadius(living);
		AABB bounds = new AABB(x - radius, centerY - radius, z - radius,
			x + radius, centerY + radius, z + radius);
		found.add(new Entry(x, feetY, centerY, z, radius, bounds, distanceSq));
	}

	public static List<Entry> entries() {
		return entries;
	}

	// 包私有：测试之间清理静态状态，避免共享列表与摘要互相影响。
	static void resetForTest() {
		entries = List.of();
		entriesHash = 0;
		hasPublishedEntries = false;
		wasEnabled = false;
		REFRESH.reset();
	}

	// 生物沿用 Mob.isWithinMeleeAttackRange 的距离来源；玩家使用主手物品的 AttackRange（默认实体交互距离）。
	private static float attackRadius(LivingEntity living) {
		if (living instanceof Player player) {
			AttackRange range = player.getAttackRangeWith(player.getMainHandItem());
			return (float) (range.effectiveMaxRange(player) + player.getBbWidth() / 2.0);
		}
		if (living instanceof Mob mob) {
			ItemStack activeItem = mob.getActiveItem();
			AttackRange range = activeItem.get(DataComponents.ATTACK_RANGE);
			double reach = range != null
				? range.effectiveMaxRange(mob)
				: MobAttackRangeAccessor.tweakify$getDefaultAttackReach();
			return (float) (reach + mob.getBbWidth() / 2.0);
		}
		return 0.0f;
	}
}
