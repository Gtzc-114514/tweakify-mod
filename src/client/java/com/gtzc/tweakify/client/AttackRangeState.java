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
 * 攻击范围：每 tick 收集附近的生物与玩家并计算原版攻击距离，
 * ID 列表为空时包含全部生物与玩家，非空时只保留命中的实体。
 */
public final class AttackRangeState {
	// feetY 用于平面圆环，centerY 用于球体中心；bounds 用于渲染前的视锥剔除，
	// distanceSq 在收集时算好，排序与渲染都用同一份
	public record Entry(double x, double feetY, double centerY, double z, float radius, AABB bounds,
						double distanceSq) {
	}

	// 按距离从近到远：比较器只建一次，不再每个刷新周期新建 lambda。
	private static final Comparator<Entry> BY_DISTANCE = Comparator.comparingDouble(Entry::distanceSq);

	private static List<Entry> entries = List.of();
	private static int tickCounter;

	private AttackRangeState() {
	}

	public static void tick(Minecraft client) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.functions.attackRangeDisplay || client.player == null || client.level == null) {
			entries = List.of();
			tickCounter = 0;
			return;
		}
		// 每 2 tick 刷新一次（约 100ms），把实体扫描与半径计算开销减半。
		if (++tickCounter % 2 != 0) {
			return;
		}
		double maxDistance = Mth.clamp(config.functions.attackRangeMaxDistance,
			TweakifyModConfig.MIN_ATTACK_RANGE_DISTANCE, TweakifyModConfig.MAX_ATTACK_RANGE_DISTANCE);
		int maxTargets = Mth.clamp(config.functions.attackRangeMaxTargets,
			TweakifyModConfig.MIN_ATTACK_RANGE_TARGETS, TweakifyModConfig.MAX_ATTACK_RANGE_TARGETS);
		// 用范围查询代替遍历全部渲染实体，减少每 tick 的实体扫描量。
		AABB searchBox = client.player.getBoundingBox().inflate(maxDistance);
		List<LivingEntity> candidates = new ArrayList<>();
		if (config.functions.attackRangeIncludeSelf) {
			candidates.add(client.player);
		}
		for (Entity entity : client.level.getEntities(client.player, searchBox,
			e -> e instanceof Mob || e instanceof Player)) {
			if (entity instanceof LivingEntity living && living != client.player) {
				candidates.add(living);
			}
		}
		double maxDistanceSq = maxDistance * maxDistance;
		List<Entry> found = new ArrayList<>();
		for (LivingEntity living : candidates) {
			if (!living.isAlive() || living.isRemoved() || living.isSpectator() || living.isInvisible()) {
				continue;
			}
			double distanceSq = living.distanceToSqr(client.player);
			if (distanceSq > maxDistanceSq) {
				continue;
			}
			if (!config.isAttackRangeEntityAllowed(living.getType())) {
				continue;
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
		// 按距离排序：渲染层只取最近的若干目标，保证超出显示上限时先显示近处。
		found.sort(BY_DISTANCE);
		if (found.size() > maxTargets) {
			found = new ArrayList<>(found.subList(0, maxTargets));
		}
		entries = List.copyOf(found);
	}

	public static List<Entry> entries() {
		return entries;
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
