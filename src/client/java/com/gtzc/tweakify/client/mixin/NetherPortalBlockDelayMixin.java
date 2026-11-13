package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.NetherPortalDelayState;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 地狱门传送延迟：原版按 {@code PLAYERS_NETHER_PORTAL_DEFAULT_DELAY} /
 * {@code PLAYERS_NETHER_PORTAL_CREATIVE_DELAY} 两个游戏规则决定玩家等待时间。
 *
 * <p>这里只在玩家明确开启对应子设置时覆盖返回值：
 * 生存模式返回 0（立即传送），创造模式返回生存延迟（不再秒传）。
 */
@Mixin(NetherPortalBlock.class)
public class NetherPortalBlockDelayMixin {
	@ModifyReturnValue(method = "getPortalTransitionTime", at = @At("RETURN"))
	private int tweakify$overridePortalDelay(int original, ServerLevel level, Entity entity) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !(entity instanceof Player player)) {
			return original;
		}
		int survivalDelay = level.getGameRules().get(GameRules.PLAYERS_NETHER_PORTAL_DEFAULT_DELAY);
		int override = NetherPortalDelayState.overrideDelay(config.netherPortalTeleportDelay,
			config.netherPortalSurvivalInstantTeleport, config.netherPortalCreativeDelayedTeleport,
			true, player.getAbilities().invulnerable, survivalDelay);
		return override != NetherPortalDelayState.NO_OVERRIDE ? override : original;
	}
}
