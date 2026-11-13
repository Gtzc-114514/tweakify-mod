package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 更好的攀爬（对齐 artemisSystem/better-climbing）：
 * 向下看时加速下降、攀爬上升更快、攀爬方块中可从地面起跳、
 * 无移动输入时不因贴墙碰撞减速。全部只在客户端本地玩家生效。
 *
 * <p>另外承载“修改”标签的“经过可攀爬方块不减速”：它是水平攀爬减速的唯一开关，
 * 只跳过水平速度限制，垂直方向的攀爬规则保持原版。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityClimbingMixin {
	@Unique
	private int tweakify$climbDownTicks;
	@Unique
	private int tweakify$climbUpTicks;
	@Unique
	private boolean tweakify$climbingUpThisTick;

	@Shadow
	public abstract boolean onClimbable();

	// 攀爬水平减速只由修改标签的“经过可攀爬方块不减速”控制：
	// 只要蹭到梯子/藤蔓的边缘就不再夹紧水平速度，垂直方向的攀爬夹紧保持原版。
	// 旧“更好的攀爬 → 攀爬不减速”已合并到这里，避免关闭新开关后仍被旧子开关覆盖。
	@Redirect(method = "handleOnClimbable", require = 0, at = @At(value = "INVOKE",
		target = "Lnet/minecraft/util/Mth;clamp(DDD)D"))
	private double tweakify$noClimbSlowdown(double speed, double min, double max) {
		LivingEntity self = (LivingEntity) (Object) this;
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config != null && config.climbablePassNoSlowdown
			&& self instanceof LocalPlayer && self.level().isClientSide()) {
			return speed;
		}
		return Mth.clamp(speed, min, max);
	}

	// 向下看时加速下降：视角从 20 度压到 90 度时下降速度平滑增加到 -0.4。
	@Redirect(method = "handleOnClimbable", require = 0, at = @At(value = "INVOKE",
		target = "Ljava/lang/Math;max(DD)D"))
	private double tweakify$fastDescend(double currentYSpeed, double downSpeed) {
		LivingEntity self = (LivingEntity) (Object) this;
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config != null && config.betterClimbing && config.climbingFastDescend
			&& self instanceof LocalPlayer && self.level().isClientSide()) {
			double maxDown = Mth.clampedMap(self.getXRot(), 20.0f, 90.0f, downSpeed, -0.4);
			if (maxDown < -0.15) {
				maxDown = Mth.clampedMap((double) this.tweakify$climbDownTicks, 0.0, 60.0,
					maxDown, maxDown * 1.5);
			}
			return Math.max(currentYSpeed, maxDown);
		}
		return Math.max(currentYSpeed, downSpeed);
	}

	// 计时器：向下看持续攀爬下降、以及向上攀爬的 tick 数。
	@ModifyReturnValue(method = "handleRelativeFrictionAndCalculateMovement", at = @At("RETURN"), require = 0)
	private Vec3 tweakify$updateClimbTicks(Vec3 original, Vec3 movement, float friction) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!self.level().isClientSide() || !(self instanceof LocalPlayer)) {
			return original;
		}
		if (this.onClimbable() && original.y < 0.0 && self.getXRot() > 20.0f) {
			this.tweakify$climbDownTicks++;
		} else {
			this.tweakify$climbDownTicks = 0;
		}
		if (this.tweakify$climbingUpThisTick) {
			this.tweakify$climbUpTicks++;
			this.tweakify$climbingUpThisTick = false;
		} else {
			this.tweakify$climbUpTicks = 0;
		}
		return original;
	}

	// 攀爬上升速度：1.25 倍起步、持续攀爬 3 秒内提升到 2.5 倍；可从地面直接起跳。
	@ModifyArg(method = "handleRelativeFrictionAndCalculateMovement", require = 0, at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/phys/Vec3;<init>(DDD)V"), index = 1)
	private double tweakify$fasterAscend(double climbSpeed) {
		LivingEntity self = (LivingEntity) (Object) this;
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.betterClimbing || !self.level().isClientSide()
			|| !(self instanceof LocalPlayer) || !self.onClimbable()) {
			return climbSpeed;
		}
		this.tweakify$climbingUpThisTick = true;
		double speed = climbSpeed;
		if (config.climbingFasterAscend) {
			double increased = climbSpeed * 1.25;
			speed = Mth.clampedMap((double) this.tweakify$climbUpTicks, 0.0, 60.0,
				increased, increased * 2.0);
		}
		if (config.climbingJumpFromGround) {
			return Math.max(self.getDeltaMovement().y, speed);
		}
		return speed;
	}

	// 攀爬时没有移动输入则不触发水平碰撞减速（只对本地玩家）。
	@Redirect(method = "handleRelativeFrictionAndCalculateMovement", require = 0, at = @At(value = "FIELD",
		target = "Lnet/minecraft/world/entity/LivingEntity;horizontalCollision:Z", opcode = Opcodes.GETFIELD))
	private boolean tweakify$cancelNonDeliberateCollision(LivingEntity entity) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config != null && config.betterClimbing && config.climbingCancelCollision
			&& entity.level().isClientSide() && entity instanceof LocalPlayer player) {
			return entity.horizontalCollision && player.input.getMoveVector().length() > 0.0f;
		}
		return entity.horizontalCollision;
	}
}
