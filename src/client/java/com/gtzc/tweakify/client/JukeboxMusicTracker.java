package com.gtzc.tweakify.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * 客户端唱片机播放状态：记录正在播放音乐的唱片机位置。
 *
 * <p>原版客户端不会把唱片机里的物品同步到方块实体，只通过 1010 / 1011
 * 两个方块事件通知声音开始与停止；肩膀鹦鹉的跳舞判定必须依据这两个事件，
 * 不能读取客户端方块实体的 {@code isPlaying()}。
 */
public final class JukeboxMusicTracker {
	// 与原版鹦鹉 aiStep 相同的判定距离。
	private static final double JUKEBOX_RANGE = 3.46;
	private static final double JUKEBOX_RANGE_SQUARED = JUKEBOX_RANGE * JUKEBOX_RANGE;

	private static final Set<BlockPos> PLAYING = new HashSet<>();

	private JukeboxMusicTracker() {
	}

	// 方块事件 1010：唱片机开始播放。
	public static void start(BlockPos pos) {
		PLAYING.add(pos.immutable());
	}

	// 方块事件 1011：唱片机停止播放（手动停止或歌曲播完）。
	public static void stop(BlockPos pos) {
		PLAYING.remove(pos);
	}

	// 切换世界时清空，避免把上一个维度的位置带到新世界。
	public static void clear() {
		PLAYING.clear();
	}

	public static boolean isPlayingNear(Level level, double x, double y, double z) {
		for (BlockPos pos : PLAYING) {
			if (!level.getBlockState(pos).is(Blocks.JUKEBOX)) {
				continue;
			}
			if (Vec3.atCenterOf(pos).distanceToSqr(x, y, z) <= JUKEBOX_RANGE_SQUARED) {
				return true;
			}
		}
		return false;
	}
}
