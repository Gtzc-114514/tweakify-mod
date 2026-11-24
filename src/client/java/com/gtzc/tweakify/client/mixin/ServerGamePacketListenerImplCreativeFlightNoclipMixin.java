package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.CreativeFlightNoclip;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

/**
 * 创造飞行穿墙的服务端放行：仅单人/局域网集成服务端。
 *
 * <p>客户端已经跳过方块碰撞，但服务端在 {@code handlePlayerPositionChange} 中还会用
 * {@code isEntityCollidingWithAnythingNew} 检测玩家是否进入方块，命中后立即 teleport
 * 回旧位置。这里只放行玩家自身的那次检测；载具检测在另一个方法中，保持原版。
 * 远程专用服务器未安装本模组时仍会回滚。
 */
@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplCreativeFlightNoclipMixin {
	@Shadow
	public ServerPlayer player;

	@ModifyExpressionValue(method = "handlePlayerPositionChange",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;"
				+ "isEntityCollidingWithAnythingNew"
				+ "(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/world/entity/Entity;"
				+ "Lnet/minecraft/world/phys/AABB;DDD)Z"))
	private boolean tweakify$allowCreativeFlightNoclip(boolean original) {
		return CreativeFlightNoclip.shouldBypassServer(this.player) ? false : original;
	}
}
