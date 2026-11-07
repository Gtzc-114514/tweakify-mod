package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.DimensionTeleportState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PortalProcessor;
import net.minecraft.world.level.block.Portal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PortalProcessor.class)
public class PortalProcessorMixin {
	@Shadow
	@Final
	private Portal portal;

	// processPortalTeleportation 返回 false 表示本 tick 不完成传送，也不会写入传送冷却，
	// 玩家留在传送门内即可持续被拦截；只有末地门/地狱门会命中。
	@Inject(method = "processPortalTeleportation", at = @At("HEAD"), cancellable = true)
	private void tweakify$blockDimensionTeleport(ServerLevel level, Entity entity, boolean canUsePortal,
												CallbackInfoReturnable<Boolean> cir) {
		if (DimensionTeleportState.shouldBlockPortal(this.portal, entity)) {
			cir.setReturnValue(false);
		}
	}
}
