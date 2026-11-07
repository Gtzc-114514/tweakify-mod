package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.client.mixin.FireworkRocketAccessor;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.phys.AABB;

/**
 * 鞘翅烟花使用限制：滑翔中使用烟花后，等到烟花加速结束（附着的烟花实体消失）才能再次使用。
 * 判定全部在客户端完成；烟花实体由服务端生成并同步，正常情况下使用后一两 tick 内可见。
 */
public final class ElytraFireworkState {
	// 等待烟花实体出现的超时：超过后认为本次使用没有产生加速（如被服务端拒绝），解除限制。
	private static final int ROCKET_APPEAR_TIMEOUT_TICKS = 40;
	private static final double SEARCH_RADIUS = 2.0;

	// 是否处于“已使用烟花、等待加速结束”的锁定状态。
	private static boolean blocking = false;
	// 锁定后是否已经观察到附着的烟花实体；观察到之后实体消失才代表加速结束。
	private static boolean sawRocket = false;
	private static int blockingTicks = 0;

	private ElytraFireworkState() {
	}

	public static void tick(Minecraft client) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.elytraFireworkCooldown
			|| client.player == null || client.level == null || !client.player.isFallFlying()) {
			reset();
			return;
		}
		if (!blocking) {
			return;
		}
		blockingTicks++;
		// 配置了固定间隔时按 tick 解锁，不再等待烟花实体消失。
		int fixedTicks = config.elytraFireworkCooldownTicks;
		if (fixedTicks > 0) {
			if (blockingTicks >= fixedTicks) {
				reset();
			}
			return;
		}
		boolean rocketPresent = hasAttachedRocket(client.player);
		if (rocketPresent) {
			sawRocket = true;
			return;
		}
		if (shouldRelease(sawRocket, false, blockingTicks)) {
			reset();
		}
	}

	// 纯判定：锁定期间没看到烟花实体，且（之前看到过烟花或等待已超时）时解除限制。
	static boolean shouldRelease(boolean sawRocket, boolean rocketPresent, int blockingTicks) {
		return !rocketPresent && (sawRocket || blockingTicks > ROCKET_APPEAR_TIMEOUT_TICKS);
	}

	// 由 MinecraftUseMixin 在右键开始时调用：锁定期间阻止再次使用烟花。
	public static boolean shouldBlockUse() {
		return blocking;
	}

	// 允许使用烟花时调用：进入锁定，直到加速结束。
	public static void markUsed() {
		blocking = true;
		sawRocket = false;
		blockingTicks = 0;
	}

	// 玩家附近附着在自身身上的烟花实体：只有它才会持续提供鞘翅加速。
	private static boolean hasAttachedRocket(Player player) {
		AABB box = player.getBoundingBox().inflate(SEARCH_RADIUS);
		for (Entity entity : player.level().getEntities(player, box, e -> e instanceof FireworkRocketEntity)) {
			if (((FireworkRocketAccessor) entity).tweakify$getAttachedToEntity() == player) {
				return true;
			}
		}
		return false;
	}

	static void reset() {
		blocking = false;
		sawRocket = false;
		blockingTicks = 0;
	}
}
