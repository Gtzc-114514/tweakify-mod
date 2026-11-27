package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.world.entity.player.Player;

/**
 * 创造飞行穿墙：飞行期间把 {@code noPhysics} 设为 true，跳过方块碰撞。
 *
 * <p>只在原版每 tick 重置 noPhysics 之后、移动逻辑之前设置，客户端与集成服务端都会执行，
 * 因此单人/局域网主机不需要额外的服务端放行。远程服务器没有安装本模组，仍按原版校验。
 */
public final class CreativeFlightNoclip {
	private CreativeFlightNoclip() {
	}

	public static boolean isActive(Player player) {
		if (player == null) {
			return false;
		}
		TweakifyModConfig config = TweakifyMod.config();
		return isActive(config != null && config.functions.creativeFlightNoclip,
			player.isCreative(), player.getAbilities().flying);
	}

	static boolean isActive(boolean enabled, boolean creative, boolean flying) {
		return enabled && creative && flying;
	}
}
