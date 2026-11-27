package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.MiningPenaltyState;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public class PlayerMiningPenaltyMixin {
	// 水下惩罚：原版在水下时把挖掘速度乘以 SUBMERGED_MINING_SPEED 属性（默认 0.2）。
	// 开关开启后按保留百分比抬向 1.0；属性本身大于 1 时保留增益。
	@Redirect(method = "getDestroySpeed",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;getValue()D"))
	private double tweakify$removeUnderwaterPenalty(AttributeInstance instance) {
		double value = instance.getValue();
		TweakifyModConfig config = TweakifyMod.config();
		if (config == null || !config.modified.disableUnderwaterMiningPenalty) {
			return value;
		}
		Player self = (Player) (Object) this;
		if (!MiningPenaltyState.shouldRemoveUnderwaterPenalty(
			config.modified.underwaterMiningPenaltySwimmingOnly, self.isSwimming())) {
			return value;
		}
		return MiningPenaltyState.underwaterMultiplier(value, config.modified.underwaterMiningPenaltyKeep);
	}

	// 空中惩罚：离地时 getDestroySpeed 末尾固定除以 5，按保留百分比把除数拉向 1。
	@ModifyConstant(method = "getDestroySpeed", constant = @Constant(floatValue = 5.0F))
	private float tweakify$removeAirPenalty(float original) {
		TweakifyModConfig config = TweakifyMod.config();
		if (config == null || !config.modified.disableAirMiningPenalty) {
			return original;
		}
		Player self = (Player) (Object) this;
		if (config.modified.airMiningPenaltyOnlyWhenFalling
			&& self.getDeltaMovement().y >= 0.0) {
			return original;
		}
		return MiningPenaltyState.airDivisor(original, config.modified.airMiningPenaltyKeep);
	}
}
