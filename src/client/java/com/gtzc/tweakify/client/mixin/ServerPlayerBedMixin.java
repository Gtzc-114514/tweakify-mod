package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.state.BedDistanceRules;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 禁用床太远：跳过 startSleepInBed 里的床距离检查（TOO_FAR_AWAY），
 * 其余检查（是否已入睡、床被阻挡、怪物附近等）保持不变。
 */
@Mixin(ServerPlayer.class)
public class ServerPlayerBedMixin {
	@Redirect(method = "startSleepInBed", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/server/level/ServerPlayer;bedInRange(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z"))
	private boolean tweakify$ignoreBedDistance(ServerPlayer self, BlockPos pos, Direction direction) {
		TweakifyModConfig config = TweakifyMod.config();
		boolean vanillaInRange = ((ServerPlayerAccessor) (Object) self).tweakify$bedInRange(pos, direction);
		if (config.modified.disableBedTooFar) {
			double distanceSq = self.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
			return BedDistanceRules.shouldAllowSleep(true, vanillaInRange, distanceSq,
				config.modified.bedTooFarMaxDistance);
		}
		return vanillaInRange;
	}
}
