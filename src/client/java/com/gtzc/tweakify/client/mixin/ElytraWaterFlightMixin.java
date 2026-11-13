package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 鞘翅水中飞行：滑翔时不再让流体移动逻辑接管。
 *
 * <p>原版 LivingEntity.travel 优先判断“是否应走流体移动”，在水中即使正在滑翔也会切成游泳/下沉。
 * 开关开启且实体正在滑翔时改为走鞘翅移动分支，水中也能保持滑翔姿态与速度。
 */
@Mixin(LivingEntity.class)
public class ElytraWaterFlightMixin {
	@Redirect(method = "travel", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/LivingEntity;shouldTravelInFluid(Lnet/minecraft/world/level/material/FluidState;)Z"))
	private boolean tweakify$keepGlidingInWater(LivingEntity self, FluidState fluidState) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config != null && config.modified.elytraWaterFlight && self.isFallFlying()
			&& (!config.modified.elytraWaterFlightOnlyWater || fluidState.getType().isSame(Fluids.WATER))) {
			return false;
		}
		// 原逻辑：处于液体、受流体影响且无法站在该流体上时才走流体移动。
		return self.isInLiquid() && self.isAffectedByFluids() && !self.canStandOnFluid(fluidState);
	}
}
