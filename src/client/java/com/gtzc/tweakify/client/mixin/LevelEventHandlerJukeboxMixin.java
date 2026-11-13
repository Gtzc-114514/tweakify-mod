package com.gtzc.tweakify.client.mixin;

import com.gtzc.tweakify.client.JukeboxMusicTracker;
import net.minecraft.client.renderer.LevelEventHandler;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 记录客户端唱片机的播放状态：原版只在 1010 / 1011 方块事件里通知开始与停止，
 * 客户端方块实体本身不保存唱片机里的物品。
 */
@Mixin(LevelEventHandler.class)
public class LevelEventHandlerJukeboxMixin {
	// 原版方块事件：1010 开始播放唱片，1011 停止播放。
	private static final int JUKEBOX_PLAY_EVENT = 1010;
	private static final int JUKEBOX_STOP_EVENT = 1011;

	@Inject(method = "levelEvent", at = @At("HEAD"))
	private void tweakify$trackJukeboxMusic(int eventId, BlockPos pos, int data, CallbackInfo ci) {
		if (eventId == JUKEBOX_PLAY_EVENT) {
			JukeboxMusicTracker.start(pos);
		} else if (eventId == JUKEBOX_STOP_EVENT) {
			JukeboxMusicTracker.stop(pos);
		}
	}
}
