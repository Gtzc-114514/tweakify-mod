package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 主动爬行：客户端每 tick 采样一次爬行键的按住状态，姿态强制由
 * {@code Player.updatePlayerPose} 的注入读取同一份快照。
 *
 * <p>只在“本地玩家”或“单人/局域网房主”身上生效：姿态需要客户端与集成服务端同时改变，
 * 否则服务端仍按 1.8 格站立判定碰撞，玩家会钻不进 1 格高的空间。
 */
public final class ForceCrawlState {
	// volatile：客户端线程写，集成服务端线程读。
	private static volatile boolean crawlKeyHeld = false;

	private ForceCrawlState() {
	}

	// 由客户端 tick 调用：总开关关闭或没有玩家时立即清除按住状态。
	public static void tick(Minecraft client) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		crawlKeyHeld = config != null && config.functions.forceCrawl
			&& client.player != null
			&& TweakifyModConfigOptions.FORCE_CRAWL_KEY.getKeybind().isKeybindHeld();
	}

	public static boolean shouldForceCrawl(Player player) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		return config != null && config.functions.forceCrawl && crawlKeyHeld && isControlledPlayer(player);
	}

	// 客户端只认本地玩家；服务端只认单人/局域网房主，避免 LAN 访客跟着房主的按键一起爬。
	// 远程服务器没有本模组的服务端注入，客户端单方面改姿态会被服务器拉回，因此只在集成服务端世界生效。
	private static boolean isControlledPlayer(Player player) {
		if (player.level().isClientSide()) {
			return player instanceof LocalPlayer && Minecraft.getInstance().hasSingleplayerServer();
		}
		if (player instanceof ServerPlayer serverPlayer) {
			MinecraftServer server = serverPlayer.level().getServer();
			return server != null && server.isSingleplayerOwner(serverPlayer.nameAndId());
		}
		return false;
	}
}
