package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
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
	// 开启开关后抬到 1.0，同时保留属性本身大于 1 时的增益。
	@Redirect(method = "getDestroySpeed",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;getValue()D"))
	private double tweakify$removeUnderwaterPenalty(AttributeInstance instance) {
		double value = instance.getValue();
		TweakifyModConfig config = TweakifyMod.CONFIG;
		return config != null && config.disableUnderwaterMiningPenalty ? Math.max(1.0, value) : value;
	}

	// 空中惩罚：离地时 getDestroySpeed 末尾固定除以 5，把该常量换成 1 即不再减速。
	@ModifyConstant(method = "getDestroySpeed", constant = @Constant(floatValue = 5.0F))
	private float tweakify$removeAirPenalty(float original) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		return config != null && config.disableAirMiningPenalty ? 1.0F : original;
	}
}
