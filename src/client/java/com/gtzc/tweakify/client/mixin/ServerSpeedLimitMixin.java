package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 取消速度上限：跳过服务端 {@code handleMovePlayer} 中的
 * “moved too quickly” 速度比较。
 *
 * <p>{@code shouldCheckPlayerMovement} 只控制速度检查入口；后续的碰撞与位置校正
 * （{@code moved wrongly}）不受影响。判定在集成服务端执行，因此仅在单人/自己开设的
 * 局域网世界生效。
 */
@Mixin(ServerGamePacketListenerImpl.class)
public class ServerSpeedLimitMixin {
	@Shadow
	public ServerPlayer player;

	@ModifyReturnValue(method = "shouldCheckPlayerMovement", at = @At("RETURN"))
	private boolean tweakify$skipSpeedLimit(boolean original) {
		TweakifyModConfig config = TweakifyMod.config();
		if (!config.modified.removeSpeedLimit) {
			return original;
		}
		boolean vehiclesOnly = config.modified.removeSpeedLimitOnlyVehicles;
		boolean flyingOnly = config.modified.removeSpeedLimitOnlyFlying;
		if (vehiclesOnly || flyingOnly) {
			if (this.player == null) {
				return original;
			}
			// 两个过滤项是“或”关系：开启任意一项时，只要玩家满足其中一类才取消上限。
			boolean matches = (vehiclesOnly && this.player.isPassenger())
				|| (flyingOnly && (this.player.getAbilities().flying || this.player.isFallFlying()));
			if (!matches) {
				return original;
			}
		}
		return false;
	}
}
