package com.gtzc.tweakify.client.state;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.List;

/**
 * 长矛相关修复的配置读取与纯计算。
 *
 * <p>按 MC 问题号拆成独立子项：蓄力命中的速度/击退/冷却、副手附魔来源、
 * 以及生物普通刺击的击退。计算部分保持无副作用，方便单元测试。
 */
public final class SpearFixes {
	private SpearFixes() {
	}

	private static TweakifyModConfig config() {
		return TweakifyMod.config();
	}

	private static boolean root() {
		TweakifyModConfig config = config();
		return config.fix.spearFixes;
	}

	public static boolean mc305835() {
		return root() && config().fix.spearFixMc305835;
	}

	public static boolean mc308786() {
		return root() && config().fix.spearFixMc308786;
	}

	public static boolean mc303451() {
		return root() && config().fix.spearFixMc303451;
	}

	public static boolean mc310858() {
		return root() && config().fix.spearFixMc310858;
	}

	public static boolean mc302732() {
		return root() && config().fix.spearFixMc302732;
	}

	public static boolean mc304406() {
		return root() && config().fix.spearFixMc304406;
	}

	public static boolean mc308270() {
		return root() && config().fix.spearFixMc308270;
	}

	public static boolean mc308272() {
		return root() && config().fix.spearFixMc308272;
	}

	// 蓄力命中逻辑需要重写原版方法：任一相关子项开启即可。
	public static boolean chargeFixesEnabled() {
		return mc303451() || mc308786() || mc310858();
	}

	// 蓄力伤害的附魔来源改为武器槽本身（锋利/亡灵杀手/节肢杀手）。
	public static boolean damageEnchantFromWeapon() {
		return mc302732() || mc304406() || mc308270();
	}

	// 蓄力击退的附魔来源改为武器槽本身（击退）。
	public static boolean knockbackEnchantFromWeapon() {
		return mc302732() || mc304406();
	}

	// 命中后的附加效果来源改为武器槽本身（火焰附加/抢夺）。
	public static boolean postEffectsFromWeapon() {
		return mc302732() || mc304406() || mc308272();
	}

	// 与原版 KineticWeapon.damageEntities 等价，按开启的子项替换速度/击退/冷却判定。
	public static void damageEntities(KineticWeapon weapon, ItemStack stack, int ticksRemaining,
									  LivingEntity attacker, EquipmentSlot slot) {
		int ticksUsed = stack.getUseDuration(attacker) - ticksRemaining;
		if (ticksUsed < weapon.delayTicks()) {
			return;
		}
		ticksUsed -= weapon.delayTicks();
		final int usedTicks = ticksUsed;

		boolean horizontalOnly = mc308786();
		Vec3 look = attacker.getLookAngle();
		Vec3 direction = horizontalOnly ? horizontalDirection(look) : look;
		Vec3 attackerMotion = horizontalMotion(KineticWeapon.getMotion(attacker), horizontalOnly);
		double attackerSpeed = speedAlong(direction, attackerMotion);
		float actionFactor = attacker instanceof Player ? 1.0F : 0.2F;
		double baseDamage = attacker.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
		boolean affected = false;

		Collection<EntityHitResult> hitResults = ProjectileUtil.getHitEntitiesAlong(attacker,
				attacker.getAttackRangeWith(stack), entity -> PiercingWeapon.canHitEntity(attacker, entity),
				ClipContext.Block.COLLIDER).right().orElse(List.of());

		for (EntityHitResult hitResult : hitResults) {
			Entity target = hitResult.getEntity();
			if (target instanceof EnderDragonPart dragonPart) {
				target = dragonPart.parentMob;
			}
			if (attacker.wasRecentlyStabbed(target, weapon.contactCooldownTicks())) {
				continue;
			}

			Vec3 targetMotion = horizontalMotion(KineticWeapon.getMotion(target), horizontalOnly);
			double targetSpeed = speedAlong(direction, targetMotion);
			double relativeSpeed = Math.max(0.0, attackerSpeed - targetSpeed);
			double knockbackSpeed = knockbackConditionSpeed(attackerSpeed, relativeSpeed, mc303451());

			boolean dismount = weapon.dismountConditions()
				.map(condition -> condition.test(usedTicks, attackerSpeed, relativeSpeed, actionFactor))
				.orElse(false);
			boolean knockback = weapon.knockbackConditions()
				.map(condition -> condition.test(usedTicks, knockbackSpeed, relativeSpeed, actionFactor))
				.orElse(false);
			boolean damage = weapon.damageConditions()
				.map(condition -> condition.test(usedTicks, attackerSpeed, relativeSpeed, actionFactor))
				.orElse(false);

			boolean hit = false;
			if (dismount || knockback || damage) {
				float damageDealt = (float) baseDamage
					+ (float) Mth.floor(relativeSpeed * weapon.damageMultiplier());
				hit = attacker.stabAttack(slot, target, damageDealt, damage, knockback, dismount);
			}
			affected |= hit;
			// MC-310858：只有真正命中才记录接触冷却；关闭该项时保持原版提前记录。
			if (!mc310858() || hit) {
				attacker.rememberStabbedEntity(target);
			}
		}

		if (affected) {
			attacker.level().broadcastEntityEvent(attacker, (byte) 2);
			if (attacker instanceof ServerPlayer player) {
				CriteriaTriggers.SPEAR_MOBS_TRIGGER.trigger(player,
					attacker.stabbedEntities(entity -> entity instanceof LivingEntity));
			}
		}
	}

	// MC-308786：只看水平方向，避免低头攻击时俯仰角把速度投影压到阈值以下。
	public static Vec3 horizontalDirection(Vec3 look) {
		Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
		double lengthSqr = horizontal.lengthSqr();
		return lengthSqr < 1.0e-6
			? new Vec3(0.0, 0.0, 1.0)
			: horizontal.scale(1.0 / Math.sqrt(lengthSqr));
	}

	public static Vec3 horizontalMotion(Vec3 motion, boolean horizontalOnly) {
		return horizontalOnly ? new Vec3(motion.x, 0.0, motion.z) : motion;
	}

	public static double speedAlong(Vec3 direction, Vec3 motion) {
		return direction.dot(motion);
	}

	// MC-303451：击退条件的速度实参改用相对速度（伤害条件仍看自身速度），
	// 相对速度不足时不再出现只击退不伤害的冲锋。
	public static double knockbackConditionSpeed(double attackerSpeed, double relativeSpeed,
												  boolean useRelativeSpeed) {
		return useRelativeSpeed ? relativeSpeed : attackerSpeed;
	}

	// 副手附魔修复：伤害附魔按武器槽物品计算（与原版 ServerPlayer 的差值仅在此）。
	public static float enchantDamage(LivingEntity attacker, ItemStack weaponItem, Entity target,
									  DamageSource source, float baseDamage) {
		if (attacker.level() instanceof ServerLevel level) {
			return EnchantmentHelper.modifyDamage(level, weaponItem, target, source, baseDamage);
		}
		return baseDamage;
	}

	// 副手附魔修复：击退附魔按武器槽物品计算。
	public static float enchantKnockback(LivingEntity attacker, ItemStack weaponItem, Entity target,
										 DamageSource source) {
		float knockback = (float) attacker.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
		if (attacker.level() instanceof ServerLevel level) {
			return EnchantmentHelper.modifyKnockback(level, weaponItem, target, source, knockback) / 2.0F;
		}
		return knockback / 2.0F;
	}
}
