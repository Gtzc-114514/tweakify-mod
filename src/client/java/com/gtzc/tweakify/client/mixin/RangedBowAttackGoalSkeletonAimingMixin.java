package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.SkeletonAimingState;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MC-121706：骷髅和幻术师在高处瞄准时只看水平方向，不抬头/低头。
 *
 * <p>26.3 的 RangedBowAttackGoal 在走位分支调用 Monster.lookAt(target, 30, 30)；
 * 之后补一次 LookControl.setLookAt(target, 30, 30)，让视线跟随目标的俯仰角。
 * 只在该目标存在且配置开启时介入。
 */
@Mixin(RangedBowAttackGoal.class)
public abstract class RangedBowAttackGoalSkeletonAimingMixin {
	@Unique
	private static final AtomicBoolean TWEAKIFY$SKELETON_AIMING_PROBE = new AtomicBoolean();

	@Shadow
	@Final
	private Monster mob;

	@Inject(method = "tick", require = 0,
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/monster/Monster;lookAt("
				+ "Lnet/minecraft/world/entity/Entity;FF)V",
			shift = At.Shift.AFTER))
	private void tweakify$lookAtTargetPitch(CallbackInfo ci,
			@Local(ordinal = 0) LivingEntity target) {
		TweakifyModConfig config = TweakifyMod.config();
		boolean enabled = config != null && config.fix.skeletonAimingFix;
		if (enabled && config.fix.skeletonAimingOnlySkeletons
			&& !(this.mob instanceof AbstractSkeleton)) {
			return;
		}
		if (enabled && config.fix.skeletonAimingOnlyTargetAbove && target != null
			&& target.getY() <= this.mob.getY()) {
			return;
		}
		if (!SkeletonAimingState.shouldForceLook(enabled, target != null)) {
			return;
		}
		HookProbe.markOnce(TWEAKIFY$SKELETON_AIMING_PROBE, "skeletonAimingFix");
		this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
	}
}
