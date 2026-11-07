package com.gtzc.tweakify.client.mixin;

import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 读取 Mob 的默认近战攻击距离（原版 isWithinMeleeAttackRange 在无 ATTACK_RANGE 组件时使用）。
 */
@Mixin(Mob.class)
public interface MobAttackRangeAccessor {
	@Accessor("DEFAULT_ATTACK_REACH")
	static double tweakify$getDefaultAttackReach() {
		throw new AssertionError("Mixin accessor not applied");
	}
}
