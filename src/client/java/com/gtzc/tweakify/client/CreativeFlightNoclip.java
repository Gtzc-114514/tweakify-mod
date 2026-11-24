package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * 创造飞行穿墙的判定与状态：只在本地客户端的创造飞行期间跳过方块碰撞。
 *
 * <p>世界边界与实体碰撞保持不变；服务端仍会按原版校验移动，因此不会影响多人服务器的
 * 反作弊判定逻辑本身。
 */
public final class CreativeFlightNoclip {
	private CreativeFlightNoclip() {
	}

	public static boolean shouldBypass(Entity entity) {
		if (entity == null || !entity.level().isClientSide()) {
			// 先按逻辑端短路：集成服务端线程不会加载 LocalPlayer。
			return false;
		}
		if (!(entity instanceof LocalPlayer player)) {
			return false;
		}
		TweakifyModConfig config = TweakifyMod.CONFIG;
		return shouldBypass(config != null && config.functions.creativeFlightNoclip,
			player.level().isClientSide(), player.getAbilities().flying,
			player.isCreative(), player.isPassenger());
	}

	// 纯判定入口：便于单元测试覆盖各个前置条件。
	static boolean shouldBypass(boolean enabled, boolean clientSide, boolean flying,
								boolean creative, boolean passenger) {
		return enabled && clientSide && flying && creative && !passenger;
	}

	// 集成服务端放行：创造飞行时跳过 ServerGamePacketListenerImpl 的碰撞回滚。
	// 远程专用服务器仍需在服务端安装本模组才会生效。
	public static boolean shouldBypassServer(ServerPlayer player) {
		if (player == null) {
			return false;
		}
		TweakifyModConfig config = TweakifyMod.CONFIG;
		return shouldBypassServer(config != null && config.functions.creativeFlightNoclip,
			player.isCreative(), player.getAbilities().flying, player.isPassenger());
	}

	static boolean shouldBypassServer(boolean enabled, boolean creative, boolean flying, boolean passenger) {
		return enabled && creative && flying && !passenger;
	}
}
