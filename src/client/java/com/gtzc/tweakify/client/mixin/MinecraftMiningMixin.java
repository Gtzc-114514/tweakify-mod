package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.MiningCooldownState;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMiningMixin {
	@Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
	private void tweakify$blockMiningAfterFirstBlock(boolean attacking, CallbackInfo ci) {
		// 开关与 tick 判定都在状态类内：这里不再直接读可变标志。
		if (MiningCooldownState.shouldBlockMining()) {
			ci.cancel();
		}
	}
}
