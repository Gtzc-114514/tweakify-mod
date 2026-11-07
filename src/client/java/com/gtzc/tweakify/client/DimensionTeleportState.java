package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.EndGatewayBlock;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.Portal;

/**
 * 维度传送拦截状态：总开关关闭时全部不生效。
 * 每个门子开关都是该门及其黑名单的总开关：开启后黑名单为空时拦截该门所有实体，
 * 非空时只拦截名单中的实体；开关关闭时黑名单不生效。
 * 末地折跃门有独立的开关与黑名单，三个门的名单互不影响。
 */
public final class DimensionTeleportState {
	private DimensionTeleportState() {
	}

	public static boolean shouldBlockPortal(Portal portal, Entity entity) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.disableDimensionTeleport || entity == null) {
			return false;
		}

		// 只拦玩家时，其他实体继续正常穿门。
		if (config.dimensionTeleportPlayersOnly && !(entity instanceof Player)) {
			return false;
		}

		// 末地折跃门：独立的开关与黑名单。
		if (portal instanceof EndGatewayBlock) {
			return shouldBlockPortalWithBlacklist(config.disableEndGatewayTeleport,
				config.isEndGatewayTeleportBlacklistEmpty(),
				config.isEndGatewayTeleportBlacklisted(entity.getType()));
		}

		boolean isEndPortal = portal instanceof EndPortalBlock;
		boolean isNetherPortal = portal instanceof NetherPortalBlock;
		if (!isEndPortal && !isNetherPortal) {
			return false;
		}

		boolean blacklisted = isEndPortal
			? config.isEndPortalTeleportBlacklisted(entity.getType())
			: config.isNetherPortalTeleportBlacklisted(entity.getType());
		boolean blacklistEmpty = isEndPortal
			? config.isEndPortalTeleportBlacklistEmpty()
			: config.isNetherPortalTeleportBlacklistEmpty();
		return shouldBlock(config.disableEndPortalTeleport, config.disableNetherPortalTeleport,
			isEndPortal, isNetherPortal, blacklistEmpty, blacklisted);
	}

	// 末地门/地狱门的纯判定：门开关关闭时不拦截（黑名单也不生效）；
	// 开启后黑名单为空则拦截所有实体，非空则只拦截命中名单的实体。
	static boolean shouldBlock(boolean endPortalEnabled, boolean netherPortalEnabled,
							   boolean isEndPortal, boolean isNetherPortal,
							   boolean blacklistEmpty, boolean blacklisted) {
		if (!isEndPortal && !isNetherPortal) {
			return false;
		}
		return shouldBlockPortalWithBlacklist(isEndPortal ? endPortalEnabled : netherPortalEnabled,
			blacklistEmpty, blacklisted);
	}

	// 门开关 + 黑名单的共用判定（末地门、地狱门、末地折跃门一致）。
	static boolean shouldBlockPortalWithBlacklist(boolean enabled, boolean blacklistEmpty, boolean blacklisted) {
		return enabled && (blacklistEmpty || blacklisted);
	}
}
