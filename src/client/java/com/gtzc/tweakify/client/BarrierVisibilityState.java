package com.gtzc.tweakify.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * 屏障可见状态：沿用原版手持屏障时的 BLOCK_MARKER 粒子机制。
 * 粒子固定在方块中心，每个方块单独冷却，避免随机偏移、贴图重叠和同一方块堆叠。
 * 方块扫描由 {@link BlockMarkerScanner} 与基岩高亮共用一次遍历。
 */
public final class BarrierVisibilityState {
	// 单次扫描最多保留的标记数量：超出时保留离玩家最近的。
	private static final int MAX_MARKERS = 256;
	// 每 tick 最多生成的粒子数；配合逐方块冷却，均匀覆盖而不会堆叠。
	private static final int PARTICLES_PER_TICK = 8;
	// 同一方块两次标记粒子之间的冷却（3 秒），确保旧粒子已经消失。
	private static final int PARTICLE_COOLDOWN_TICKS = 60;

	private static List<BlockPos> markers = List.of();
	private static long tickCounter;
	private static int particleCursor;
	private static final Map<BlockPos, Long> nextParticleAt = new HashMap<>();
	private static final HashSet<BlockPos> MARKER_SET = new HashSet<>();
	private static BlockParticleOption barrierMarker;

	private BarrierVisibilityState() {
	}

	// 当前需要标记的屏障方块（只读），供测试与调试查看。
	public static List<BlockPos> markers() {
		return markers;
	}

	// 纯判定：水logged 的屏障仍是屏障方块本身。
	public static boolean isBarrier(BlockState state) {
		return state.is(Blocks.BARRIER);
	}

	// 接收扫描结果：超出上限时保留离玩家最近的，并清理已离开范围的粒子冷却记录。
	static void setMarkers(List<BlockPos> found, BlockPos center) {
		List<BlockPos> accepted = found;
		if (accepted.size() > MAX_MARKERS) {
			List<BlockPos> sorted = new ArrayList<>(accepted);
			sorted.sort(Comparator.comparingDouble(pos -> pos.distSqr(center)));
			accepted = sorted.subList(0, MAX_MARKERS);
		}
		markers = List.copyOf(accepted);
		pruneCooldowns();
	}

	// 功能关闭时清空标记与粒子冷却。
	static void clear() {
		if (!markers.isEmpty()) {
			markers = List.of();
		}
		nextParticleAt.clear();
		MARKER_SET.clear();
		particleCursor = 0;
		tickCounter = 0;
	}

	// 每 tick 调用：按固定顺序轮询标记，每个方块冷却结束后在方块正中心生成一个粒子。
	static void tickParticles(Minecraft client) {
		tickCounter++;
		if (markers.isEmpty()) {
			return;
		}
		BlockParticleOption marker = barrierMarker();
		int markerCount = markers.size();
		if (particleCursor >= markerCount) {
			particleCursor = 0;
		}
		int count = Math.min(PARTICLES_PER_TICK, markerCount);
		int spawned = 0;
		int scanned = 0;
		while (spawned < count && scanned < markerCount) {
			BlockPos pos = markers.get(particleCursor);
			particleCursor = (particleCursor + 1) % markerCount;
			scanned++;
			Long next = nextParticleAt.get(pos);
			if (next != null && tickCounter < next) {
				continue;
			}
			nextParticleAt.put(pos, tickCounter + PARTICLE_COOLDOWN_TICKS);
			// 固定在方块中心，不再使用随机偏移，保证上下左右对称。
			client.level.addParticle(marker,
				pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
				0.0, 0.0, 0.0);
			spawned++;
		}
	}

	// 与手持屏障物品时相同的原版方块标记粒子。
	private static BlockParticleOption barrierMarker() {
		if (barrierMarker == null) {
			barrierMarker = new BlockParticleOption(ParticleTypes.BLOCK_MARKER,
				Blocks.BARRIER.defaultBlockState());
		}
		return barrierMarker;
	}

	// 清理已经不在标记列表中的冷却记录，避免玩家移动后 map 无限增长。
	private static void pruneCooldowns() {
		if (nextParticleAt.isEmpty()) {
			return;
		}
		if (markers.isEmpty()) {
			nextParticleAt.clear();
			return;
		}
		// 复用同一个集合做 retainAll，避免每 0.5 秒分配一个新的 HashSet。
		MARKER_SET.clear();
		MARKER_SET.addAll(markers);
		nextParticleAt.keySet().retainAll(MARKER_SET);
	}
}
