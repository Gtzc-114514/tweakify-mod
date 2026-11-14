package com.gtzc.tweakify.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 单层基岩高亮状态：记录哪些面朝向空气，供渲染层填充标记。
 * 邻接方块挡住的面不标记，避免无谓的绘制。
 * 扫描由 {@link BlockMarkerScanner} 与屏障可见共用一次方块遍历。
 */
public final class BedrockHighlightState {
	// 一个需要标记的单层基岩：顶/底面是否暴露（邻接空气）。
	public record Marker(BlockPos pos, boolean topExposed, boolean bottomExposed) {
	}

	private static List<Marker> markers = List.of();
	// 已发布内容的摘要：相同则跳过发布，让 BedrockGeometryCache 保持旧引用、不重建顶点。
	private static int markersHash;
	private static boolean hasPublishedMarkers;

	private BedrockHighlightState() {
	}

	// 当前需要标记的方块（只读）。
	public static List<Marker> markers() {
		return markers;
	}

	// 纯判定：自身是基岩且上下都不是基岩 -> 该列只有一层。
	public static boolean isSingleLayer(boolean bedrock, boolean aboveBedrock, boolean belowBedrock) {
		return bedrock && !aboveBedrock && !belowBedrock;
	}

	// 纯判定：相机在顶面之上才能看到顶面，在底面之下才能看到底面。
	public static boolean isFaceVisibleFrom(boolean topFace, double cameraY, int blockY) {
		return topFace ? cameraY > blockY + 1.0 : cameraY < blockY;
	}

	// 计算某个位置的标记；不是单层基岩或两面都被方块挡住时返回 null。
	// 自身状态由调用方传入（同一位置只查一次），只有候选方块才会再去查上下邻居。
	// 批量扫描时由调用方提供可变的相邻坐标，避免每层上千次 above()/below() 产生的临时 BlockPos。
	static Marker markerAt(BlockGetter level, BlockPos pos, BlockState state,
			BlockPos.MutableBlockPos neighbor) {
		if (!state.is(Blocks.BEDROCK)) {
			return null;
		}
		neighbor.set(pos.getX(), pos.getY() + 1, pos.getZ());
		BlockState above = level.getBlockState(neighbor);
		if (above.is(Blocks.BEDROCK)) {
			return null;
		}
		neighbor.set(pos.getX(), pos.getY() - 1, pos.getZ());
		BlockState below = level.getBlockState(neighbor);
		if (below.is(Blocks.BEDROCK)) {
			return null;
		}
		boolean topExposed = above.isAir();
		boolean bottomExposed = below.isAir();
		return topExposed || bottomExposed ? new Marker(pos.immutable(), topExposed, bottomExposed) : null;
	}

	// 接收扫描结果：超出上限时保留离玩家最近的标记，避免扫描窗口角落抢占名额。
	static void setMarkers(List<Marker> found, BlockPos center, int maxMarkers) {
		List<Marker> accepted = found;
		if (accepted.size() > maxMarkers) {
			List<Marker> sorted = new ArrayList<>(accepted);
			sorted.sort(Comparator.comparingDouble(marker -> marker.pos().distSqr(center)));
			accepted = sorted.subList(0, maxMarkers);
		}
		// 内容摘要未变时保留旧列表引用：BedrockGeometryCache 按引用失效，
		// 跳过发布即可避免每秒 6 次仍然整表重建顶点数组。
		// 摘要只作预筛，再用 equals 精确比对，哈希碰撞不会让几何停在旧内容。
		int hash = contentHash(accepted);
		if (hasPublishedMarkers && hash == markersHash && markers.equals(accepted)) {
			return;
		}
		hasPublishedMarkers = true;
		markersHash = hash;
		// 内容变化时必须发布新列表实例：缓存的失效条件就是引用变化。
		markers = List.copyOf(accepted);
	}

	// 轻量内容摘要：size + 每个标记的坐标与暴露面。
	// 成本是一次 O(n) 哈希运算，远低于重建几何（每个标记 48 顶点）。
	private static int contentHash(List<Marker> markers) {
		int hash = markers.size();
		for (Marker marker : markers) {
			hash = 31 * hash + marker.hashCode();
		}
		return hash;
	}

	// 功能关闭时清空标记（避免残留上一帧的几何），并重置摘要以便重新开启后再次发布。
	static void clear() {
		if (!markers.isEmpty()) {
			markers = List.of();
		}
		hasPublishedMarkers = false;
		markersHash = 0;
	}
}
