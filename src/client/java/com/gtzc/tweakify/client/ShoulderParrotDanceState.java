package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

import java.util.HashMap;
import java.util.Map;

/**
 * 肩膀鹦鹉跳舞判定：像基岩版一样，附近唱片机播放音乐时肩膀上的鹦鹉也跳舞。
 *
 * <p>Java 版把肩上的鹦鹉存进玩家数据并从世界里移除，原版唱片机事件只会通知
 * 世界里真实存在的鹦鹉实体，因此肩膀上的鹦鹉永远停在 {@code POSE_ON_SHOULDER}。
 * 这里按玩家位置查询客户端唱片机播放状态，把渲染姿态改为 {@code PARTY}。
 *
 * <p>判定结果按「世界 + gameTime + 玩家实体 ID」缓存，避免每个渲染帧重复计算。
 */
public final class ShoulderParrotDanceState {
	private static final Map<Integer, Boolean> CACHE = new HashMap<>();
	private static ClientLevel cachedLevel;
	private static long cachedGameTime = Long.MIN_VALUE;

	private ShoulderParrotDanceState() {
	}

	public static boolean shouldDance(AvatarRenderState playerState) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.fix.parrotDanceOnShoulder || playerState == null) {
			return false;
		}
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) {
			return false;
		}

		long gameTime = level.getGameTime();
		if (level != cachedLevel) {
			// 切换世界时唱片机位置属于旧维度，必须清空。
			cachedLevel = level;
			cachedGameTime = gameTime;
			CACHE.clear();
			JukeboxMusicTracker.clear();
		} else if (gameTime != cachedGameTime) {
			cachedGameTime = gameTime;
			CACHE.clear();
		}
		// 用眼睛高度近似鹦鹉站在肩膀上的位置，距离判定与原版鹦鹉更接近。
		return CACHE.computeIfAbsent(playerState.id,
			id -> JukeboxMusicTracker.isPlayingNear(level, playerState.x,
				playerState.y + playerState.eyeHeight, playerState.z));
	}
}
